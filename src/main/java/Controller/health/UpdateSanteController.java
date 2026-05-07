package Controller.health;

import Model.health.SuiviSante;
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

public class UpdateSanteController {

    public static SuiviSante currentSuivi;

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
    private Runnable onSaved;

    /** Open as a modal popup. */
    public static void openPopup(SuiviSante suivi, Runnable onSaved) {
        currentSuivi = suivi;
        try {
            FXMLLoader loader = new FXMLLoader(
                    UpdateSanteController.class.getResource("/health/UpdateSantePopup.fxml"));
            Parent root = loader.load();
            UpdateSanteController ctrl = loader.getController();
            ctrl.onSaved = onSaved;

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(
                    UpdateSanteController.class.getResource("/Task/board_style.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void initialize() {
        qualiteSommeilBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        humeurBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        if (currentSuivi != null) {
            datePicker.setValue(currentSuivi.getDate());
            sommeilField.setText(String.valueOf(currentSuivi.getHeuresSommeil()));
            qualiteSommeilBox.setValue(currentSuivi.getQualiteSommeil());
            eauField.setText(String.valueOf(currentSuivi.getVerresEau()));
            activiteMinField.setText(String.valueOf(currentSuivi.getMinutesActivite()));
            poidsField.setText(String.valueOf(currentSuivi.getPoids()));
            humeurBox.setValue(currentSuivi.getHumeur());
            activiteTypeField.setText(currentSuivi.getActivite() != null ? currentSuivi.getActivite() : "");
            notesField.setText(currentSuivi.getNotes() != null ? currentSuivi.getNotes() : "");
        }
    }

    @FXML
    void updateSuiviSante(ActionEvent event) {
        try {
            currentSuivi.setDate(datePicker.getValue());
            currentSuivi.setHeuresSommeil(Float.parseFloat(sommeilField.getText()));
            currentSuivi.setQualiteSommeil(qualiteSommeilBox.getValue());
            currentSuivi.setVerresEau(Integer.parseInt(eauField.getText()));
            currentSuivi.setMinutesActivite(Integer.parseInt(activiteMinField.getText()));
            currentSuivi.setPoids(Float.parseFloat(poidsField.getText()));
            currentSuivi.setHumeur(humeurBox.getValue());
            currentSuivi.setActivite(activiteTypeField.getText());
            currentSuivi.setNotes(notesField.getText());
            service.modifier(currentSuivi);
            if (onSaved != null) onSaved.run();
            closeStage();
        } catch (NumberFormatException e) {
            showError("Veuillez vérifier les valeurs numériques.");
        } catch (SQLException e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (currentSuivi == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer ce suivi ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    service.supprimer(currentSuivi.getId());
                    if (onSaved != null) onSaved.run();
                    closeStage();
                } catch (SQLException e) { showError("Erreur : " + e.getMessage()); }
            }
        });
    }

    @FXML
    void handleClose(ActionEvent event) { closeStage(); }

    // Legacy — kept for non-popup usage
    @FXML
    void deleteSuiviSante(ActionEvent event) { handleDelete(event); }

    @FXML
    void goBack(ActionEvent event) { closeStage(); }

    private void closeStage() {
        if (datePicker != null && datePicker.getScene() != null)
            ((Stage) datePicker.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        if (errorLabel != null) { errorLabel.setText("⚠ " + msg); errorLabel.setVisible(true); }
    }
}
