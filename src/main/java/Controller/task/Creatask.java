package Controller.task;

import enums.PrioriteTache;
import enums.StatutTache;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;
import model.task.Tache;
import model.task.TaskSpace;
import model.user.User;
import service.task.TacheService;
import service.task.TaskSpaceService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class Creatask implements Initializable {

    @FXML private AnchorPane root;
    @FXML private TextField titre;
    @FXML private TextField description;
    @FXML private ChoiceBox<PrioriteTache> priorite;
    @FXML private ChoiceBox<Integer> difficulte;
    @FXML private ChoiceBox<StatutTache> statut;
    @FXML private ChoiceBox<TaskSpace> taskSpace;
    @FXML private DatePicker deadline;

    private final TacheService tacheService = new TacheService();
    private final TaskSpaceService taskSpaceService = new TaskSpaceService();

    private User selectedUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priorite.getItems().setAll(PrioriteTache.values());
        statut.getItems().setAll(StatutTache.values());
        difficulte.getItems().setAll(1, 2, 3, 4, 5);

        try {
            List<TaskSpace> taskSpaces = taskSpaceService.recuperer();
            TaskSpace solo = new TaskSpace(0, "Solo", null, null, null, 0, null, 0);

            taskSpace.getItems().add(solo);
            taskSpace.getItems().addAll(taskSpaces);

            taskSpace.setConverter(new StringConverter<TaskSpace>() {
                @Override
                public String toString(TaskSpace object) {
                    return object != null ? object.getNom() : "";
                }

                @Override
                public TaskSpace fromString(String string) {
                    return null;
                }
            });

            taskSpace.setValue(solo);

        } catch (SQLException e) {
            System.err.println("Erreur chargement TaskSpaces: " + e.getMessage());
        }

        difficulte.setValue(1);

        Platform.runLater(() -> {
            if (root.getUserData() instanceof User) {
                this.selectedUser = (User) root.getUserData();
            }
        });
    }

    private boolean isInputValid() {
        String tTitre = titre.getText();

        if (tTitre == null || tTitre.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Le titre est obligatoire.");
            return false;
        }
        if (tTitre.trim().length() < 3) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Le titre doit contenir au moins 3 caractères.");
            return false;
        }
        if (tTitre.trim().length() > 100) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Le titre ne peut pas dépasser 100 caractères.");
            return false;
        }

        if (priorite.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Veuillez choisir une priorité.");
            return false;
        }

        if (statut.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Veuillez choisir un statut.");
            return false;
        }

        if (deadline.getValue() != null) {
            if (deadline.getValue().isBefore(LocalDate.now())) {
                showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "La deadline ne peut pas être une date passée.");
                return false;
            }
        }

        return true;
    }

    @FXML
    private void save(ActionEvent event) {
        if (!isInputValid()) {
            return;
        }

        try {
            String tTitre         = titre.getText().trim();
            PrioriteTache tPrio   = priorite.getValue();
            int tDiff             = difficulte.getValue() != null ? difficulte.getValue() : 1;
            StatutTache tStatut   = statut.getValue();
            int tTaskSpace        = taskSpace.getValue() != null ? taskSpace.getValue().getId() : 0;
            String tDesc          = description.getText() != null ? description.getText().trim() : "";

            Date tDate = null;
            if (deadline.getValue() != null) {
                tDate = Date.from(deadline.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
            }

            int utilisateurId = (selectedUser != null) ? selectedUser.getId() : 1;

            Tache nouvelleTache = new Tache(tTitre, tDesc, tPrio, tDiff, tStatut, tDate, tTaskSpace, utilisateurId);
            tacheService.ajouter(nouvelleTache);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "La tâche a été ajoutée avec succès !");
            viderChamps();
            retourVersReadTask(event);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Base de données", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez vérifier vos champs de saisie.\n" + e.getMessage());
        }
    }

    @FXML
    private void retourVersReadTask(ActionEvent event) {
        try {
            Parent readRoot = FXMLLoader.load(getClass().getResource("/task/readtask.fxml"));
            root.getScene().setRoot(readRoot);
        } catch (IOException e) {
            System.err.println("Erreur retour ReadTask : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void viderChamps() {
        titre.clear();
        description.clear();
        deadline.setValue(null);
    }
}