package Controller;

import enums.PrioriteTache;
import enums.StatutTache;
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
import model.Tache;
import service.TacheService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Date;
import java.util.ResourceBundle;

public class Creatask implements Initializable {

    @FXML private AnchorPane root;
    @FXML private TextField titre;
    @FXML private TextField description;
    @FXML private ChoiceBox<PrioriteTache> priorite;
    @FXML private ChoiceBox<Integer> difficulte;
    @FXML private ChoiceBox<StatutTache> statut;
    @FXML private ChoiceBox<Integer> taskSpace;
    @FXML private DatePicker deadline;

    private final TacheService tacheService = new TacheService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priorite.getItems().setAll(PrioriteTache.values());
        statut.getItems().setAll(StatutTache.values());
        difficulte.getItems().setAll(1, 2, 3, 4, 5);
        taskSpace.getItems().setAll(0, 1, 2, 3);
        difficulte.setValue(1);
        taskSpace.setValue(0);
    }

    // ── Back button → ReadTask.fxml ───────────────────────────────────
    @FXML
    private void retourVersReadTask(ActionEvent event) {
        try {
            Parent readTaskRoot = FXMLLoader.load(getClass().getResource("/readtask.fxml"));
            root.getScene().setRoot(readTaskRoot);
        } catch (IOException e) {
            System.err.println("Erreur retour vers ReadTask : " + e.getMessage());
        }
    }

    // ── Save task ─────────────────────────────────────────────────────
    @FXML
    void save(ActionEvent event) {
        try {
            String tTitre = titre.getText();

            if (tTitre == null || tTitre.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Le titre de la tâche est obligatoire !");
                return;
            }

            PrioriteTache tPrio   = priorite.getValue();
            Integer tDiff         = difficulte.getValue() != null ? difficulte.getValue() : 1;
            StatutTache tStatut   = statut.getValue();
            Integer tTaskSpace    = taskSpace.getValue() != null ? taskSpace.getValue() : 0;
            String tDesc          = description.getText();

            Date tDate = null;
            if (deadline.getValue() != null) {
                tDate = Date.from(deadline.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
            }

            int utilisateurId = 1; // replace with logged-in user later

            Tache nouvelleTache = new Tache(tTitre, tDesc, tPrio, tDiff, tStatut, tDate, tTaskSpace, utilisateurId);
            tacheService.ajouter(nouvelleTache);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "La tâche a été ajoutée avec succès !");
            viderChamps();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Base de données", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez vérifier vos champs de saisie.\n" + e.getMessage());
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
        priorite.setValue(null);
        statut.setValue(null);
        difficulte.setValue(1);
        taskSpace.setValue(0);
    }
}