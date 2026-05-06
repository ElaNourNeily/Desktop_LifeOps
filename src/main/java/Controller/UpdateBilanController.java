package Controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import model.BilanSante;
import service.BilanSanteService;

import java.sql.SQLException;

public class UpdateBilanController {

    public static BilanSante currentBilan;

    @FXML
    private DatePicker dateDebutPicker;
    @FXML
    private DatePicker dateFinPicker;
    @FXML
    private ChoiceBox<Integer> fatigueBox;
    @FXML
    private ChoiceBox<Integer> stressBox;
    @FXML
    private TextField scoreField;
    @FXML
    private CheckBox burnoutCheckBox;
    @FXML
    private TextField recommandationsField;

    private final BilanSanteService service = new BilanSanteService();

    @FXML
    public void initialize() {
        fatigueBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        stressBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));

        if(currentBilan != null) {
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
        if (!isInputValid()) return;

        try {
            currentBilan.setDateDebut(dateDebutPicker.getValue());
            currentBilan.setDateFin(dateFinPicker.getValue());
            currentBilan.setNiveauFatigue(fatigueBox.getValue());
            currentBilan.setNiveauStress(stressBox.getValue());
            currentBilan.setScoreForme(Float.parseFloat(scoreField.getText()));
            currentBilan.setRisqueBurnout(burnoutCheckBox.isSelected());
            currentBilan.setRecommandations(recommandationsField.getText());

            service.modifier(currentBilan);

            utils.AlertUtils.showSuccess("Bilan actualisé ! ✅", 
                "Les modifications de votre analyse de santé ont bien été enregistrées.");

            goBack(event);

        } catch (SQLException e) {
            utils.AlertUtils.showError("Erreur SQL ⚠️", "Impossible de mettre à jour le bilan : " + e.getMessage());
        }
    }

    private boolean isInputValid() {
        String msg = "";
        if (dateDebutPicker.getValue() == null || dateFinPicker.getValue() == null) msg += "Dates invalides.\n";
        try {
            float s = Float.parseFloat(scoreField.getText());
            if (s < 0 || s > 10) msg += "Score doit être entre 0 et 10.\n";
        } catch (Exception e) { msg += "Score non numérique.\n"; }

        if (msg.isEmpty()) return true;
        utils.AlertUtils.showError("Erreur formulaire ❌", msg);
        return false;
    }

    @FXML
    void deleteBilan(ActionEvent event) {
        if (currentBilan == null) return;
        try {
            service.supprimer(currentBilan.getId());
            utils.AlertUtils.showSuccess("Supprimé ! 🗑", "Le bilan a été retiré de votre historique.");
            goBack(event);
        } catch (SQLException e) {
            utils.AlertUtils.showError("Erreur ⚠️", "Impossible de supprimer le bilan : " + e.getMessage());
        }
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ReadSante.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
            scene.setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
