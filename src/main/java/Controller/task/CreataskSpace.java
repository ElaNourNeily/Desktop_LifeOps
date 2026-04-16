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


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        type.getItems().setAll(TypeTaskSpace.values());
        statut.getItems().setAll(StatutTaskSpace.values());

        // Default selections matching screenshot
        type.setValue(TypeTaskSpace.values()[0]);
        statut.setValue(StatutTaskSpace.values()[0]);

        // Spinner 1–365, default 14
        duration.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 14)
        );
        duration.setEditable(true);
    }
    // Replace with logged-in user id later
    private static final int UTILISATEUR_ID = 2;

    // ── Back → ReadTaskSpace ──────────────────────────────────
    @FXML
    private void retourVersReadTaskSpace(ActionEvent event) {
        try {
            Parent readRoot = FXMLLoader.load(getClass().getResource("/task/TaskSpace.fxml"));
            root.getScene().setRoot(readRoot);
        } catch (IOException e) {
            System.err.println("Erreur retour ReadTaskSpace : " + e.getMessage());
        }
    }

    // ── Save ─────────────────────────────────────────────────
    @FXML
    private void enregistrer(ActionEvent event) {
        String tNom = nom.getText();

        if (tNom == null || tNom.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le nom du projet est obligatoire !");
            return;
        }

        TypeTaskSpace tType     = type.getValue();
        StatutTaskSpace tStatut = statut.getValue();
        int tDuration           = duration.getValue() != null ? duration.getValue() : 14;
        String tDesc            = description.getText();

        try {
            TaskSpace ts = new TaskSpace(
                tNom, tType, new Date(), tDesc, tDuration, tStatut, UTILISATEUR_ID
            );
            taskSpaceService.ajouter(ts);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Projet créé avec succès !");
            retourVersReadTaskSpace(event);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ── Cancel ───────────────────────────────────────────────
    @FXML
    private void annuler(ActionEvent event) {
        retourVersReadTaskSpace(event);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
