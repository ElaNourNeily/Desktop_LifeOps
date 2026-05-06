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
            activiteTypeField.setText(currentSuivi.getActivite());
            notesField.setText(currentSuivi.getNotes());
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
            goBack(event);
        } catch (SQLException e) { if (errorLabel != null) { errorLabel.setText("Erreur : " + e.getMessage()); errorLabel.setVisible(true); } }
    }

    @FXML
    void deleteSuiviSante(ActionEvent event) {
        if (currentSuivi == null) return;
        try { service.supprimer(currentSuivi.getId()); goBack(event); }
        catch (SQLException e) { if (errorLabel != null) { errorLabel.setText("Erreur : " + e.getMessage()); errorLabel.setVisible(true); } }
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/health/Sante.fxml"));
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) ctrl.loadContent(view);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
