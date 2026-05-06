package Controller.health;

import Model.health.SuiviSante;
import controller.user.MainLayoutController;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
    private Runnable onSaved; // callback to refresh the list

    @FXML
    public void initialize() {
        qualiteSommeilBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        humeurBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        qualiteSommeilBox.setValue(3);
        humeurBox.setValue(3);
        datePicker.setValue(java.time.LocalDate.now());
    }

    /** Open this form as a modal popup. */
    public static void openPopup(Runnable onSaved) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    CreateSanteController.class.getResource("/health/CreateSantePopup.fxml"));
            Parent root = loader.load();
            CreateSanteController ctrl = loader.getController();
            ctrl.onSaved = onSaved;

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle("Nouveau Suivi Santé");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            if (onSaved != null) onSaved.run();
            closeStage();
        } catch (SQLException e) {
            showError("Erreur base de données : " + e.getMessage());
        }
    }

    @FXML
    void handleClose(ActionEvent event) {
        closeStage();
    }

    // Also used by the non-popup version (CreateSante.fxml)
    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/health/Sante.fxml"));
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) ctrl.loadContent(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void closeStage() {
        if (datePicker != null && datePicker.getScene() != null) {
            ((Stage) datePicker.getScene().getWindow()).close();
        }
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
