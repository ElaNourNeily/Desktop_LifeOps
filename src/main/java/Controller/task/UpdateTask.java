package Controller.task;

import enums.PrioriteTache;
import enums.StatutTache;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import model.task.Tache;
import service.task.TacheService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class UpdateTask implements Initializable {

    @FXML private AnchorPane root;
    @FXML private TextField     titre;
    @FXML private TextArea      description;
    @FXML private ChoiceBox<PrioriteTache> priorite;
    @FXML private ChoiceBox<Integer>       difficulte;
    @FXML private ChoiceBox<StatutTache>   statut;
    @FXML private ChoiceBox<Integer>       taskSpace;
    @FXML private DatePicker    deadline;

    private final TacheService tacheService = new TacheService();
    private Tache tacheCourante; // task received from ReadTask

    // ── Called by ReadTask after loading this controller ─────────────
    public void setTache(Tache t) {
        this.tacheCourante = t;
        remplirChamps(t);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priorite.getItems().setAll(PrioriteTache.values());
        statut.getItems().setAll(StatutTache.values());
        difficulte.getItems().setAll(1, 2, 3, 4, 5);
        // 0 = Solo, replace later with real project list from DB
        taskSpace.getItems().setAll(0, 1, 2, 3);
    }

    // ── Fill all fields with the selected task's data ─────────────────
    private void remplirChamps(Tache t) {
        titre.setText(t.getTitre());
        description.setText(t.getDescription() != null ? t.getDescription() : "");
        priorite.setValue(t.getPriorite());
        statut.setValue(t.getStatut());
        difficulte.setValue(t.getDifficulte());
        taskSpace.setValue(t.getTaskSpaceId());

        if (t.getDeadline() != null) {
            LocalDate ld = t.getDeadline().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            deadline.setValue(ld);
        }
    }

    // ── Mettre à jour ─────────────────────────────────────────────────
    @FXML
    private void mettreAJour(ActionEvent event) {
        if (tacheCourante == null) return;

        String tTitre = titre.getText();
        if (tTitre == null || tTitre.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le titre est obligatoire !");
            return;
        }

        try {
            tacheCourante.setTitre(tTitre);
            tacheCourante.setDescription(description.getText());
            tacheCourante.setPriorite(priorite.getValue());
            tacheCourante.setDifficulte(difficulte.getValue() != null ? difficulte.getValue() : 1);
            tacheCourante.setStatut(statut.getValue());
            tacheCourante.setTaskSpaceId(taskSpace.getValue() != null ? taskSpace.getValue() : 0);

            if (deadline.getValue() != null) {
                Date d = Date.from(deadline.getValue()
                        .atStartOfDay(ZoneId.systemDefault()).toInstant());
                tacheCourante.setDeadline(d);
            } else {
                tacheCourante.setDeadline(null);
            }

            tacheService.modifier(tacheCourante);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Tâche mise à jour avec succès !");
            retourVersReadTask();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ── Supprimer ─────────────────────────────────────────────────────
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

    // ── Annuler → back to ReadTask ────────────────────────────────────
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
