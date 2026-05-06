package Controller.health;

import Model.health.BilanSante;
import controller.user.MainLayoutController;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
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
            recommandationsField.setText(currentBilan.getRecommandations());
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
            goBack(event);
        } catch (NumberFormatException e) { showError("Score invalide.");
        } catch (SQLException e) { showError("Erreur : " + e.getMessage()); }
    }

    @FXML
    void deleteBilan(ActionEvent event) {
        if (currentBilan == null) return;
        try { service.supprimer(currentBilan.getId()); goBack(event); }
        catch (SQLException e) { showError("Erreur : " + e.getMessage()); }
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/health/Sante.fxml"));
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) ctrl.loadContent(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        if (errorLabel != null) { errorLabel.setText("⚠ " + msg); errorLabel.setVisible(true); }
    }
}
