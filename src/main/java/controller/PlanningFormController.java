package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Planning;
import service.PlanningService;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;

public class PlanningFormController {

    @FXML private DatePicker datePicker;
    @FXML private CheckBox checkDisponibilite;
    @FXML private TextField txtHeureDebut;
    @FXML private TextField txtHeureFin;
    @FXML private Label lblErrorDate;
    @FXML private Button btnSubmit;

    private final PlanningService service = new PlanningService();
    private java.util.function.Consumer<LocalDate> onSaveCallback;
    private int currentUserId;
    private Planning currentPlanning;

    @FXML
    public void initialize() {
        datePicker.setValue(LocalDate.now());
        txtHeureDebut.setText("08:00:00");
        txtHeureFin.setText("20:00:00");

        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) checkDateAvailability(newVal);
        });
    }

    private void checkDateAvailability(LocalDate chosenDate) {
        try {
            if (currentUserId <= 0) return;
            Planning existing = service.recupererParDate(chosenDate, currentUserId);
            boolean isConflict = (existing != null && (currentPlanning == null || existing.getId() != currentPlanning.getId()));
            
            if (isConflict) {
                lblErrorDate.setText("⚠️ Un planning existe déjà pour cette date.");
                lblErrorDate.setVisible(true);
                lblErrorDate.setManaged(true);
                // We'll let validate() handle the strict blocking, but this gives visual feedback
            } else {
                lblErrorDate.setVisible(false);
                lblErrorDate.setManaged(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setUserId(int id) {
        this.currentUserId = id;
    }

    public void setPlanning(Planning p) {
        this.currentPlanning = p;
        if (p != null) {
            datePicker.setValue(p.getDate().toLocalDate());
            checkDisponibilite.setSelected(p.isDisponibilite());
            txtHeureDebut.setText(p.getHeureDebutJournee().toString());
            txtHeureFin.setText(p.getHeureFinJournee().toString());
        }
    }

    public void setOnSave(java.util.function.Consumer<LocalDate> callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void handleSave() {
        // Force commit DatePicker value if it was typed
        try {
            if (datePicker.getEditor().getText() != null && !datePicker.getEditor().getText().isEmpty()) {
                LocalDate date = datePicker.getConverter().fromString(datePicker.getEditor().getText());
                datePicker.setValue(date);
            }
        } catch (Exception e) { /* ignore parse error here, validate() will handle it */ }

        if (validate()) {
            try {
                boolean isNew = (currentPlanning == null);
                if (isNew) {
                    currentPlanning = new Planning();
                    System.out.println("[DEBUG] Planning: Adding new record");
                } else {
                    System.out.println("[DEBUG] Planning: Updating existing record ID=" + currentPlanning.getId());
                }

                LocalDate selectedDate = datePicker.getValue();
                System.out.println("[SQL] Final Date to save: " + selectedDate);
                currentPlanning.setDate(Date.valueOf(selectedDate));
                currentPlanning.setDisponibilite(checkDisponibilite.isSelected());
                
                String start = txtHeureDebut.getText().trim();
                if (start.length() == 5) start += ":00";
                
                String end = txtHeureFin.getText().trim();
                if (end.length() == 5) end += ":00";
                
                currentPlanning.setHeureDebutJournee(Time.valueOf(start));
                currentPlanning.setHeureFinJournee(Time.valueOf(end));
                currentPlanning.setUtilisateurId(currentUserId);

                if (isNew) service.ajouter(currentPlanning);
                else service.modifier(currentPlanning);

                System.out.println("[DEBUG] Planning save successful.");
                showAlert("Succès", "Planning enregistré avec succès !", Alert.AlertType.INFORMATION);
                if (onSaveCallback != null) onSaveCallback.accept(datePicker.getValue());
                close();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Erreur BDD", "Erreur SQL : " + e.getMessage(), Alert.AlertType.ERROR);
            } catch (IllegalArgumentException e) {
                showAlert("Format invalide", "Veuillez respecter le format HH:mm (ex: 08:30)", Alert.AlertType.ERROR);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erreur inattendue", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private boolean validate() {
        if (datePicker.getValue() == null) {
            showAlert("Erreur", "La date est obligatoire.", Alert.AlertType.ERROR);
            return false;
        }
        
        // Restriction: Only today or future FOR NEW plannings or if the date was changed
        boolean isNew = (currentPlanning == null);
        LocalDate chosenDate = datePicker.getValue();
        if (isNew && chosenDate.isBefore(LocalDate.now())) {
            showAlert("Erreur", "Impossible de créer un planning pour une date passée.", Alert.AlertType.ERROR);
            return false;
        }

        // Duplicate Check (Ignore self if editing)
        try {
            if (currentUserId <= 0) {
                System.out.println("[VALIDATION ERROR] User ID is not set correctly: " + currentUserId);
                showAlert("Erreur Système", "ID Utilisateur invalide. Veuillez redémarrer l'application.", Alert.AlertType.ERROR);
                return false;
            }

            System.out.println("[VALIDATION] Checking duplicate for User ID: " + currentUserId + " | Date: " + chosenDate);
            Planning existing = service.recupererParDate(chosenDate, currentUserId);
            
            if (existing != null) {
                int existingId = existing.getId();
                int currentId = (currentPlanning != null) ? currentPlanning.getId() : -1;
                
                System.out.println("[VALIDATION DEBUG] Comparing IDs -> Existing: " + existingId + " | Current: " + currentId);
                
                if (isNew || existingId != currentId) {
                    System.out.println("[VALIDATION CONFLICT] Another record already occupies this date (ID " + existingId + ")");
                    showAlert("Date déjà occupée", "Un planning existe déjà pour la journée du " + chosenDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.FRENCH)) + ".", Alert.AlertType.ERROR);
                    return false;
                } else {
                    System.out.println("[VALIDATION OK] Matches current record ID " + existingId + ". Modification allowed.");
                }
            } else {
                System.out.println("[VALIDATION OK] No other planning found on this date.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            String sText = txtHeureDebut.getText();
            if (sText.length() == 5) sText += ":00";
            String eText = txtHeureFin.getText();
            if (eText.length() == 5) eText += ":00";
            
            Time start = Time.valueOf(sText);
            Time end = Time.valueOf(eText);
            if (!end.after(start)) {
                showAlert("Erreur", "L'heure de fin doit être après l'heure de début.", Alert.AlertType.ERROR);
                return false;
            }
        } catch (Exception e) {
            showAlert("Erreur", "Heures invalides. Utilisez le format HH:mm.", Alert.AlertType.ERROR);
            return false;
        }
        return true;
    }

    private void close() {
        ((Stage) datePicker.getScene().getWindow()).close();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        // Custom styling for the alert to match the dark theme if possible
        alert.getDialogPane().setStyle("-fx-background-color: #18181b; -fx-text-fill: white;");
        alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");
        
        alert.showAndWait();
    }
}
