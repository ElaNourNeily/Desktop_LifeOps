package Controller.task;

import enums.StatutTaskSpace;
import enums.TypeTaskSpace;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import model.task.TaskSpace;
import service.task.TaskSpaceService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.ResourceBundle;

public class CreataskSpace implements Initializable {

    @FXML private AnchorPane root;
    @FXML private TextField nom;
    @FXML private ChoiceBox<TypeTaskSpace> type;
    @FXML private ChoiceBox<StatutTaskSpace> statut;
    @FXML private Spinner<Integer> duration;
    @FXML private TextArea description;

    private final TaskSpaceService taskSpaceService = new TaskSpaceService();

    private static final int UTILISATEUR_ID = 2;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        type.getItems().setAll(TypeTaskSpace.values());
        statut.getItems().setAll(StatutTaskSpace.values());

        type.setValue(TypeTaskSpace.values()[0]);
        statut.setValue(StatutTaskSpace.values()[0]);

        duration.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 14)
        );
        duration.setEditable(true);
    }

    @FXML
    private void retourVersTaskSpace(ActionEvent event) {
        try {
            Parent readRoot = FXMLLoader.load(getClass().getResource("/task/TaskSpace.fxml"));
            root.getScene().setRoot(readRoot);
        } catch (IOException e) {
            System.err.println("Erreur retour TaskSpace : " + e.getMessage());
        }
    }

    @FXML
    private void enregistrer(ActionEvent event) {
        String tNom = nom.getText();

        if (tNom == null || tNom.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Le nom du projet est obligatoire et ne peut pas être vide !");
            return;
        }

        TypeTaskSpace tType = type.getValue();
        if (tType == null) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Veuillez sélectionner un type de projet !");
            return;
        }

        StatutTaskSpace tStatut = statut.getValue();
        if (tStatut == null) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Veuillez sélectionner un statut de projet !");
            return;
        }

        Integer tDuration = duration.getValue();
        if (tDuration == null || tDuration < 1 || tDuration > 365) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "La durée doit être comprise entre 1 et 365 jours !");
            return;
        }

        String tDesc = description.getText();
        if (tDesc != null && tDesc.length() > 500) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "La description est trop longue (maximum 500 caractères) !");
            return;
        }

        try {
            TaskSpace ts = new TaskSpace(
                    tNom, tType, new Date(), tDesc, tDuration, tStatut, UTILISATEUR_ID
            );
            taskSpaceService.ajouter(ts);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Projet créé avec succès !");
            retourVersTaskSpace(event);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void annuler(ActionEvent event) {
        retourVersTaskSpace(event);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert a = new Alert(alertType);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}