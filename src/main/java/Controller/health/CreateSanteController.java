package Controller.health;

import Model.health.SuiviSante;
import controller.user.MainLayoutController;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import service.health.SuiviSanteService;

import java.io.IOException;
import java.sql.SQLException;

public class CreateSanteController {

    @FXML private DatePicker datePicker;
    @FXML private TextField sommeilField;
    @FXML private ChoiceBox<Integer> qualiteSommeilBox;
    @FXML private TextField eauField;
    @FXML private TextField activiteMinField;
    @FXML private TextField poidsField;
    @FXML private ChoiceBox<Integer> humeurBox;
    @FXML private TextField activiteTypeField;
    @FXML private TextField notesField;
    @FXML private Label errorLabel;

    private final SuiviSanteService service = new SuiviSanteService();

    @FXML
    public void initialize() {
        qualiteSommeilBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        humeurBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        qualiteSommeilBox.setValue(3);
        humeurBox.setValue(3);
        datePicker.setValue(java.time.LocalDate.now());
    }

    @FXML
    void saveSuiviSante(ActionEvent event) {
        if (!isValid()) return;
        try {
            SuiviSante s = new SuiviSante();
            s.setDate(datePicker.getValue());
            s.setHeuresSommeil(Float.parseFloat(sommeilField.getText()));
            s.setQualiteSommeil(qualiteSommeilBox.getValue());
            s.setVerresEau(Integer.parseInt(eauField.getText()));
            s.setMinutesActivite(Integer.parseInt(activiteMinField.getText()));
            s.setPoids(Float.parseFloat(poidsField.getText()));
            s.setHumeur(humeurBox.getValue());
            s.setActivite(activiteTypeField.getText());
            s.setNotes(notesField.getText());
            service.ajouter(s);
            goBack(event);
        } catch (SQLException e) {
            showError("Erreur base de données : " + e.getMessage());
        }
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/health/Sante.fxml"));
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) ctrl.loadContent(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private boolean isValid() {
        if (datePicker.getValue() == null) { showError("Veuillez sélectionner une date."); return false; }
        try { float s = Float.parseFloat(sommeilField.getText()); if (s < 0 || s > 24) { showError("Sommeil invalide (0-24h)."); return false; } }
        catch (NumberFormatException e) { showError("Heures de sommeil invalides."); return false; }
        try { int e = Integer.parseInt(eauField.getText()); if (e < 0) { showError("Verres d'eau invalide."); return false; } }
        catch (NumberFormatException e) { showError("Verres d'eau invalide."); return false; }
        try { float p = Float.parseFloat(poidsField.getText()); if (p <= 0) { showError("Poids invalide."); return false; } }
        catch (NumberFormatException e) { showError("Poids invalide."); return false; }
        if (activiteTypeField.getText() == null || activiteTypeField.getText().isBlank()) { showError("Veuillez préciser l'activité."); return false; }
        return true;
    }

    private void showError(String msg) {
        if (errorLabel != null) { errorLabel.setText("⚠ " + msg); errorLabel.setVisible(true); }
    }
}
