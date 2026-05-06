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

public class CreateSanteController {

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
        qualiteSommeilBox.setValue(3);
        humeurBox.setValue(3);
        datePicker.setValue(java.time.LocalDate.now());
    }

    @FXML
    void saveSuiviSante(ActionEvent event) {
        if (!isInputValid()) {
            return;
        }

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

            utils.AlertUtils.showSuccess("Nouveau suivi enregistré ! ✨", 
                "Chaque petit effort compte. Votre suivi d'aujourd'hui est bien en sécurité dans votre carnet.");
            
            goBack(event);

        } catch (SQLException e) {
            utils.AlertUtils.showError("Erreur de connexion ⚠️", "Impossible d'atteindre la base de données : " + e.getMessage());
        }
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (datePicker.getValue() == null) {
            errorMessage += "Veuillez sélectionner une date.\n";
        }
        
        try {
            float sommeil = Float.parseFloat(sommeilField.getText());
            if (sommeil < 0 || sommeil > 24) errorMessage += "Le sommeil doit être entre 0 et 24h.\n";
        } catch (NumberFormatException e) {
            errorMessage += "Heures de sommeil invalides.\n";
        }

        try {
            int eau = Integer.parseInt(eauField.getText());
            if (eau < 0) errorMessage += "Le nombre de verres d'eau ne peut pas être négatif.\n";
        } catch (NumberFormatException e) {
            errorMessage += "Nombre de verres d'eau invalide.\n";
        }

        try {
            float poids = Float.parseFloat(poidsField.getText());
            if (poids <= 0) errorMessage += "Le poids doit être supérieur à 0.\n";
        } catch (NumberFormatException e) {
            errorMessage += "Poids invalide.\n";
        }

        if (activiteTypeField.getText() == null || activiteTypeField.getText().trim().isEmpty()) {
            errorMessage += "Veuillez préciser le type d'activité.\n";
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            utils.AlertUtils.showError("Champs incorrects ❌", errorMessage);
            return false;
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
