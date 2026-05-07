package Controller.health;

import Model.health.BilanSante;
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
import service.health.BilanSanteService;

import java.io.IOException;
import java.sql.SQLException;

public class UpdateBilanController {

    public static BilanSante currentBilan;

    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private ChoiceBox<Integer> fatigueBox;
    @FXML private ChoiceBox<Integer> stressBox;
    @FXML private TextField scoreField;
    @FXML private CheckBox burnoutCheckBox;
    @FXML private TextField recommandationsField;
    @FXML private Label errorLabel;

    private final BilanSanteService service = new BilanSanteService();
    private Runnable onSaved;

    /** Open as a modal popup. */
    public static void openPopup(BilanSante bilan, Runnable onSaved) {
        currentBilan = bilan;
        try {
            FXMLLoader loader = new FXMLLoader(
                    UpdateBilanController.class.getResource("/health/UpdateBilanPopup.fxml"));
            Parent root = loader.load();
            UpdateBilanController ctrl = loader.getController();
            ctrl.onSaved = onSaved;

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(
                    UpdateBilanController.class.getResource("/Task/board_style.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void initialize() {
        fatigueBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        stressBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        if (currentBilan != null) {
            dateDebutPicker.setValue(currentBilan.getDateDebut());
            dateFinPicker.setValue(currentBilan.getDateFin());
            fatigueBox.setValue(currentBilan.getNiveauFatigue());
            stressBox.setValue(currentBilan.getNiveauStress());
            scoreField.setText(String.valueOf(currentBilan.getScoreForme()));
            burnoutCheckBox.setSelected(currentBilan.isRisqueBurnout());
            recommandationsField.setText(currentBilan.getRecommandations() != null ? currentBilan.getRecommandations() : "");
        }
    }

    @FXML
    void updateBilan(ActionEvent event) {
        try {
            float score = Float.parseFloat(scoreField.getText());
            if (score < 0 || score > 10) { showError("Score doit être entre 0 et 10."); return; }
            currentBilan.setDateDebut(dateDebutPicker.getValue());
            currentBilan.setDateFin(dateFinPicker.getValue());
            currentBilan.setNiveauFatigue(fatigueBox.getValue());
            currentBilan.setNiveauStress(stressBox.getValue());
            currentBilan.setScoreForme(score);
            currentBilan.setRisqueBurnout(burnoutCheckBox.isSelected());
            currentBilan.setRecommandations(recommandationsField.getText());
            service.modifier(currentBilan);
            if (onSaved != null) onSaved.run();
            closeStage();
        } catch (NumberFormatException e) {
            showError("Score invalide.");
        } catch (SQLException e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (currentBilan == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer ce bilan ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    service.supprimer(currentBilan.getId());
                    if (onSaved != null) onSaved.run();
                    closeStage();
                } catch (SQLException e) { showError("Erreur : " + e.getMessage()); }
            }
        });
    }

    @FXML
    void handleClose(ActionEvent event) { closeStage(); }

    // Legacy
    @FXML void deleteBilan(ActionEvent event) { handleDelete(event); }
    @FXML void goBack(ActionEvent event) { closeStage(); }

    private void closeStage() {
        if (dateDebutPicker != null && dateDebutPicker.getScene() != null)
            ((Stage) dateDebutPicker.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        if (errorLabel != null) { errorLabel.setText("⚠ " + msg); errorLabel.setVisible(true); }
    }
}
