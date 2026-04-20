package Controller.task;

import enums.PrioriteTache;
import enums.StatutTache;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;
import model.task.Tache;
import model.task.TaskSpace;
import service.task.TacheService;
import service.task.TaskSpaceService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class UpdateTask implements Initializable {

    @FXML private AnchorPane root;
    @FXML private TextField titre;
    @FXML private TextArea description;
    @FXML private ChoiceBox<PrioriteTache> priorite;
    @FXML private ChoiceBox<Integer> difficulte;
    @FXML private ChoiceBox<StatutTache> statut;
    @FXML private ChoiceBox<TaskSpace> taskSpace;
    @FXML private DatePicker deadline;

    private final TacheService tacheService = new TacheService();
    private final TaskSpaceService taskSpaceService = new TaskSpaceService();
    private Tache tacheCourante;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerPriorites();
        chargerDifficultes();
        chargerStatuts();
        chargerTaskSpaces();

        Platform.runLater(() -> {
            if (root.getUserData() instanceof Tache) {
                setTache((Tache) root.getUserData());
            }
        });
    }

    public void setTache(Tache t) {
        this.tacheCourante = t;
        titre.setText(t.getTitre());
        description.setText(t.getDescription());
        priorite.setValue(t.getPriorite());
        difficulte.setValue(t.getDifficulte());
        statut.setValue(t.getStatut());

        if (t.getDeadline() != null) {
            LocalDate ld = t.getDeadline().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            deadline.setValue(ld);
        } else {
            deadline.setValue(null);
        }

        if (t.getTaskSpaceId() > 0) {
            for (TaskSpace ts : taskSpace.getItems()) {
                if (ts != null && ts.getId() == t.getTaskSpaceId()) {
                    taskSpace.setValue(ts);
                    break;
                }
            }
        } else {
            if (!taskSpace.getItems().isEmpty()) {
                taskSpace.getSelectionModel().selectFirst();
            }
        }
    }

    private void chargerPriorites() {
        priorite.getItems().addAll(PrioriteTache.values());
    }

    private void chargerDifficultes() {
        difficulte.getItems().addAll(1, 2, 3, 4, 5);
    }

    private void chargerStatuts() {
        statut.getItems().addAll(StatutTache.values());
    }

    private void chargerTaskSpaces() {
        try {
            List<TaskSpace> spaces = taskSpaceService.findAll();

            TaskSpace soloSpace = new TaskSpace();
            soloSpace.setId(0);
            soloSpace.setNom("Mode Solo");

            taskSpace.getItems().add(soloSpace);
            taskSpace.getItems().addAll(spaces);

            taskSpace.setConverter(new StringConverter<TaskSpace>() {
                @Override
                public String toString(TaskSpace ts) {
                    if (ts == null) return "";
                    return ts.getNom();
                }

                @Override
                public TaskSpace fromString(String string) {
                    return null;
                }
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement TaskSpaces : " + e.getMessage());
        }
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
    private void mettreAJour(ActionEvent event) {
        if (tacheCourante == null) return;

        if (!isInputValid()) {
            return;
        }

        try {
            tacheCourante.setTitre(titre.getText().trim());
            tacheCourante.setDescription(description.getText() != null ? description.getText().trim() : "");
            tacheCourante.setPriorite(priorite.getValue());
            tacheCourante.setDifficulte(difficulte.getValue());
            tacheCourante.setStatut(statut.getValue());

            if (deadline.getValue() != null) {
                Date date = Date.from(deadline.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
                tacheCourante.setDeadline(date);
            } else {
                tacheCourante.setDeadline(null);
            }

            TaskSpace ts = taskSpace.getValue();
            if (ts != null) {
                tacheCourante.setTaskSpaceId(ts.getId());
            } else {
                tacheCourante.setTaskSpaceId(0);
            }

            tacheService.modifier(tacheCourante);
            retourVersReadTask();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    @FXML
    private void supprimerTache(ActionEvent event) {
        if (tacheCourante == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer la tâche « " + tacheCourante.getTitre() + " » ?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                tacheService.supprimer(tacheCourante.getId());
                retourVersReadTask();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
            }
        }
    }

    @FXML
    private void annuler(ActionEvent event) {
        retourVersReadTask();
    }

    private void retourVersReadTask() {
        try {
            Parent readRoot = FXMLLoader.load(getClass().getResource("/task/readtask.fxml"));
            root.getScene().setRoot(readRoot);
        } catch (IOException e) {
            System.err.println("Erreur retour ReadTask : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}