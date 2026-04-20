package Controller.task;

import enums.StatutTaskSpace;
import enums.TypeTaskSpace;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import model.task.TaskSpace;
import service.task.TaskSpaceService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class UpdateTaskSpace implements Initializable {

    @FXML private AnchorPane root;
    @FXML private Label lblProjectName;
    @FXML private TextField nom;
    @FXML private ChoiceBox<TypeTaskSpace> type;
    @FXML private ChoiceBox<StatutTaskSpace> statut;
    @FXML private Spinner<Integer> duration;
    @FXML private TextArea description;

    private final TaskSpaceService taskSpaceService = new TaskSpaceService();
    private TaskSpace taskSpace;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        type.getItems().setAll(TypeTaskSpace.values());
        statut.getItems().setAll(StatutTaskSpace.values());
        duration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 14));
        duration.setEditable(true);
    }

    // C'EST ICI QUE LES DONNÉES SONT RÉCUPÉRÉES ET AFFICHEES
    public void setTaskSpace(TaskSpace ts) {
        this.taskSpace = ts;
        if (ts != null) {
            lblProjectName.setText(ts.getNom());
            nom.setText(ts.getNom());
            type.setValue(ts.getType());
            statut.setValue(ts.getStatus());
            duration.getValueFactory().setValue(ts.getDuration());
            description.setText(ts.getDescription() != null ? ts.getDescription() : "");
        }
    }

    @FXML
    private void mettreAJour(ActionEvent event) {
        if (nom.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le nom du projet ne peut pas être vide.");
            return;
        }

        taskSpace.setNom(nom.getText().trim());
        taskSpace.setType(type.getValue());
        taskSpace.setStatus(statut.getValue());
        taskSpace.setDuration(duration.getValue());
        taskSpace.setDescription(description.getText());

        try {
            taskSpaceService.modifier(taskSpace);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Projet mis à jour avec succès !");
            navigateToTaskSpace();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le projet : " + e.getMessage());
        }
    }

    @FXML
    private void supprimerProjet(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer le projet « " + taskSpace.getNom() + " » et toutes ses tâches ?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                taskSpaceService.supprimer(taskSpace.getId());
                navigateToTaskSpace();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer : " + e.getMessage());
            }
        }
    }

    @FXML
    private void annuler(ActionEvent event) {
        navigateToTaskSpace();
    }

    @FXML
    private void retourVersReadTaskSpace(ActionEvent event) {
        navigateToTaskSpace();
    }

    private void navigateToTaskSpace() {
        try {
            Parent readRoot = FXMLLoader.load(getClass().getResource("/task/TaskSpace.fxml"));
            root.getScene().setRoot(readRoot);
        } catch (IOException e) {
            System.err.println("Erreur retour TaskSpace : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert a = new Alert(alertType);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}