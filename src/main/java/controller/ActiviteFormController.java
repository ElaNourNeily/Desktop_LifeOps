package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.Activite;
import service.ActiviteService;

import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class ActiviteFormController {

    @FXML private Label lblFormTitle;
    @FXML private Label lblDateSubtitle;
    @FXML private TextField txtTitre;
    @FXML private ComboBox<String> comboCategorie;
    @FXML private ComboBox<String> comboPriorite;
    @FXML private ComboBox<String> comboUrgence;
    @FXML private TextField txtHeureDebut;
    @FXML private TextField txtHeureFin;
    @FXML private Button btnDelete;
    @FXML private HBox boxColorEmeraude, boxColorBleu, boxColorOrange, boxColorRose, boxColorViolet;

    private final ActiviteService service = new ActiviteService();
    private Activite currentActivite;
    private int currentPlanningId;
    private Runnable onSaveCallback;
    private String selectedColor = "#8b5cf6"; // Default Violet

    @FXML
    public void initialize() {
        comboCategorie.setItems(FXCollections.observableArrayList("Travail", "Santé", "Finance", "Loisir", "Autre"));
        comboPriorite.setItems(FXCollections.observableArrayList("Basse", "Moyenne", "Haute"));
        comboUrgence.setItems(FXCollections.observableArrayList("Basse", "Moyenne", "Haute"));
        
        // Default values
        comboPriorite.setValue("Basse");
        comboUrgence.setValue("Moyenne");
        resetColorHighlights();
        highlightColorBox(boxColorViolet);
    }

    public void setPlanningId(int id) { this.currentPlanningId = id; }
    
    public void setOnSave(Runnable callback) { this.onSaveCallback = callback; }

    public void setDateSubtitle(java.util.Date date) {
        if (date != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            lblDateSubtitle.setText("Pour le " + sdf.format(date));
        }
    }

    public void setActivite(Activite a) {
        this.currentActivite = a;
        if (a != null) {
            lblFormTitle.setText("Modifier l'Activité");
            txtTitre.setText(a.getTitre());
            comboCategorie.setValue(a.getCategorie());
            comboPriorite.setValue(priorityIntToString(a.getPriorite()));
            comboUrgence.setValue(a.getNiveauUrgence().substring(0, 1).toUpperCase() + a.getNiveauUrgence().substring(1).toLowerCase());
            txtHeureDebut.setText(a.getHeureDebutEstimee().toString().substring(0, 5));
            txtHeureFin.setText(a.getHeureFinEstimee().toString().substring(0, 5));
            
            selectedColor = a.getCouleur();
            updateColorSelectionUI(selectedColor);
            
            this.currentPlanningId = a.getPlanningId();
            btnDelete.setVisible(true);
        }
    }

    public void setInitialTime(int hour) {
        txtHeureDebut.setText(String.format("%02d:00", hour));
        txtHeureFin.setText(String.format("%02d:00", hour + 1));
    }

    @FXML
    private void handleSave() {
        if (currentPlanningId <= 0 && currentActivite == null) {
            showAlert("Erreur", "Impossible d'associer l'activité : ID de planning invalide.");
            return;
        }
        if (validate()) {
            try {
                boolean isNew = (currentActivite == null);
                if (isNew) currentActivite = new Activite();

                currentActivite.setTitre(txtTitre.getText());
                currentActivite.setCategorie(comboCategorie.getValue());
                currentActivite.setPriorite(priorityStringToInt(comboPriorite.getValue()));
                
                String start = txtHeureDebut.getText();
                if (start.length() == 5) start += ":00";
                String end = txtHeureFin.getText();
                if (end.length() == 5) end += ":00";
                
                currentActivite.setHeureDebutEstimee(Time.valueOf(start));
                currentActivite.setHeureFinEstimee(Time.valueOf(end));
                
                // Check for overlaps (Conflicts)
                if (service.hasOverlap(currentPlanningId, currentActivite.getHeureDebutEstimee(), 
                        currentActivite.getHeureFinEstimee(), isNew ? -1 : currentActivite.getId())) {
                    showAlert("Conflit d'horaire", "Une autre activité est déjà prévue sur ce créneau.");
                    return;
                }

                // Calculate duration
                LocalTime s = LocalTime.parse(start);
                LocalTime e = LocalTime.parse(end);
                currentActivite.setDuree((int) java.time.Duration.between(s, e).toMinutes());
                
                currentActivite.setCouleur(selectedColor);
                currentActivite.setNiveauUrgence(comboUrgence.getValue().toLowerCase());
                currentActivite.setPlanningId(currentPlanningId);
                currentActivite.setEtat("en_attente");

                if (isNew) service.ajouter(currentActivite);
                else service.modifier(currentActivite);

                if (onSaveCallback != null) onSaveCallback.run();
                close();
            } catch (SQLException e) {
                showAlert("Erreur", "Base de données : " + e.getMessage());
            } catch (Exception e) {
                showAlert("Erreur", "Format invalide : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (currentActivite != null) {
            try {
                service.supprimer(currentActivite.getId());
                if (onSaveCallback != null) onSaveCallback.run();
                close();
            } catch (SQLException e) {
                showAlert("Erreur", "Suppression échouée : " + e.getMessage());
            }
        }
    }

    @FXML private void handleCancel() { close(); }

    @FXML private void selectEmeraude() { selectColor("#10b981", boxColorEmeraude); }
    @FXML private void selectBleu() { selectColor("#3b82f6", boxColorBleu); }
    @FXML private void selectOrange() { selectColor("#f59e0b", boxColorOrange); }
    @FXML private void selectRose() { selectColor("#ec4899", boxColorRose); }
    @FXML private void selectViolet() { selectColor("#8b5cf6", boxColorViolet); }

    private void selectColor(String hex, HBox box) {
        selectedColor = hex;
        resetColorHighlights();
        highlightColorBox(box);
    }

    private void resetColorHighlights() {
        HBox[] boxes = {boxColorEmeraude, boxColorBleu, boxColorOrange, boxColorRose, boxColorViolet};
        for (HBox b : boxes) {
            b.setStyle("-fx-opacity: 0.6; -fx-padding: 5; -fx-background-radius: 8;");
            b.getChildren().get(0).setStyle(""); // Circle
        }
    }

    private void highlightColorBox(HBox box) {
        box.setStyle("-fx-opacity: 1.0; -fx-background-color: rgba(255,255,255,0.05); -fx-padding: 5; -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8;");
        // We could also add a drop shadow to the circle here but style.css handles it via class if we wanted
    }

    private void updateColorSelectionUI(String color) {
        if (color == null) return;
        resetColorHighlights();
        if (color.equalsIgnoreCase("#10b981")) highlightColorBox(boxColorEmeraude);
        else if (color.equalsIgnoreCase("#3b82f6")) highlightColorBox(boxColorBleu);
        else if (color.equalsIgnoreCase("#f59e0b")) highlightColorBox(boxColorOrange);
        else if (color.equalsIgnoreCase("#ec4899")) highlightColorBox(boxColorRose);
        else if (color.equalsIgnoreCase("#8b5cf6")) highlightColorBox(boxColorViolet);
    }

    private boolean validate() {
        if (txtTitre.getText().isEmpty()) { showAlert("Erreur", "Le titre est requis."); return false; }
        if (comboCategorie.getValue() == null) { showAlert("Erreur", "La catégorie est requise."); return false; }
        try {
            String start = txtHeureDebut.getText();
            if (start.length() == 5) start += ":00";
            String end = txtHeureFin.getText();
            if (end.length() == 5) end += ":00";
            LocalTime.parse(start);
            LocalTime.parse(end);
        } catch (Exception e) {
            showAlert("Erreur", "Format d'heure invalide (ex: 08:00).");
            return false;
        }
        return true;
    }

    private void close() { ((Stage) txtTitre.getScene().getWindow()).close(); }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(content); alert.showAndWait();
    }

    private String priorityIntToString(int p) {
        return switch (p) { case 1 -> "Basse"; case 2 -> "Moyenne"; case 3 -> "Haute"; default -> "Basse"; };
    }

    private int priorityStringToInt(String p) {
        if (p == null) return 1;
        return switch (p) { case "Basse" -> 1; case "Moyenne" -> 2; case "Haute" -> 3; default -> 1; };
    }
}
