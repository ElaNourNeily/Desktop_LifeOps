package Controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import model.SuiviSante;
import service.SuiviSanteService;

import java.sql.SQLException;

public class UpdateSanteController {

    public static SuiviSante currentSuivi;

    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField sommeilField;
    @FXML
    private ChoiceBox<Integer> qualiteSommeilBox;
    @FXML
    private TextField eauField;
    @FXML
    private TextField activiteMinField;
    @FXML
    private TextField poidsField;
    @FXML
    private ChoiceBox<Integer> humeurBox;
    @FXML
    private TextField activiteTypeField;
    @FXML
    private TextField notesField;

    private final SuiviSanteService service = new SuiviSanteService();

    @FXML
    public void initialize() {
        qualiteSommeilBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        humeurBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));

        if(currentSuivi != null) {
            datePicker.setValue(currentSuivi.getDate());
            sommeilField.setText(String.valueOf(currentSuivi.getHeuresSommeil()));
            qualiteSommeilBox.setValue(currentSuivi.getQualiteSommeil());
            eauField.setText(String.valueOf(currentSuivi.getVerresEau()));
            activiteMinField.setText(String.valueOf(currentSuivi.getMinutesActivite()));
            poidsField.setText(String.valueOf(currentSuivi.getPoids()));
            humeurBox.setValue(currentSuivi.getHumeur());
            activiteTypeField.setText(currentSuivi.getActivite());
            notesField.setText(currentSuivi.getNotes());
        }
    }

    @FXML
    void updateSuiviSante(ActionEvent event) {
        if (!isInputValid()) {
            return;
        }

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

            utils.AlertUtils.showSuccess("Mise à jour réussie ! 🌟", 
                "Votre carnet de santé a été mis à jour avec brio. Continuez ainsi !");

            goBack(event);

        } catch (SQLException e) {
            utils.AlertUtils.showError("Erreur SQL ⚠️", "Impossible de mettre à jour : " + e.getMessage());
        }
    }

    private boolean isInputValid() {
        String errorMessage = "";
        if (datePicker.getValue() == null) errorMessage += "Date manquante.\n";
        try {
            float s = Float.parseFloat(sommeilField.getText());
            if (s < 0 || s > 24) errorMessage += "Sommeil invalide (0-24).\n";
        } catch (Exception e) { errorMessage += "Sommeil n'est pas un nombre.\n"; }

        try {
             float p = Float.parseFloat(poidsField.getText());
             if (p <= 0) errorMessage += "Poids doit être > 0.\n";
        } catch (Exception e) { errorMessage += "Poids invalide.\n"; }

        if (errorMessage.isEmpty()) return true;
        utils.AlertUtils.showError("Formulaire invalide ❌", errorMessage);
        return false;
    }

    @FXML
    void deleteSuiviSante(ActionEvent event) {
        if (currentSuivi == null) return;
        try {
            service.supprimer(currentSuivi.getId());
            utils.AlertUtils.showSuccess("Supprimé ! 🗑", "Le suivi a été retiré de votre historique.");
            goBack(event);
        } catch (SQLException e) {
            utils.AlertUtils.showError("Erreur ⚠️", "Impossible de supprimer : " + e.getMessage());
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
