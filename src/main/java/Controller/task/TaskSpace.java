package Controller.task;

import enums.StatutTaskSpace;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import service.task.TaskSpaceService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class TaskSpace implements Initializable {

    @FXML private FlowPane flowLeader;
    @FXML private FlowPane flowMembre;

    private final TaskSpaceService taskSpaceService = new TaskSpaceService();

    // Replace with real logged-in user id later
    private static final int UTILISATEUR_ID = 2;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerTaskSpaces();
    }

    // ── Navigate to CreataskSpace ─────────────────────────────
    @FXML
    private void naviguerVersCreataskSpace() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/task/CreataskSpace.fxml"));
            flowLeader.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation CreataskSpace : " + e.getMessage());
        }
    }

    // ── Navigate to personal tasks (ReadTask) ─────────────────
    @FXML
    private void naviguerVersMesTaches() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/task/readtask.fxml"));
            flowLeader.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation ReadTask : " + e.getMessage());
        }
    }

    // ── Load and distribute TaskSpaces ───────────────────────
    private void chargerTaskSpaces() {
        flowLeader.getChildren().clear();
        flowMembre.getChildren().clear();

        try {
            List<model.task.TaskSpace> list = taskSpaceService.recupererParUtilisateur(UTILISATEUR_ID);
            // For now all belong to the logged-in user → Leader section
            // Extend with a members table later
            if (list.isEmpty()) {
                flowLeader.getChildren().add(makeEmptyLabel("Aucun projet. Cliquez sur « Nouveau Projet » pour commencer."));
            } else {
                for (model.task.TaskSpace ts : list) {
                    flowLeader.getChildren().add(creerCarteTaskSpace(ts, true));
                }
            }

            // Membre section — empty until member logic is added
            flowMembre.getChildren().add(makeEmptyLabel("Aucun projet partagé pour l'instant."));

        } catch (SQLException e) {
            System.err.println("Erreur chargement TaskSpaces : " + e.getMessage());
        }
    }

    // ── Build one project card ────────────────────────────────
    private VBox creerCarteTaskSpace(model.task.TaskSpace ts, boolean isLeader) {
        VBox card = new VBox(10);
        card.getStyleClass().add("project-card");
        card.setPadding(new Insets(18));
        card.setPrefWidth(360);

        // Row 1: Name + Status badge
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label lblNom = new Label(ts.getNom());
        lblNom.getStyleClass().add("project-card-name");
        lblNom.setWrapText(true);
        HBox.setHgrow(lblNom, Priority.ALWAYS);

        Label lblStatut = new Label(ts.getStatus().getValeur());
        lblStatut.getStyleClass().add(statusStyleClass(ts.getStatus()));
        topRow.getChildren().addAll(lblNom, lblStatut);

        // Row 2: Description
        String descText = (ts.getDescription() != null && !ts.getDescription().isBlank())
                ? ts.getDescription() : "Aucune description";
        Label lblDesc = new Label(descText);
        lblDesc.getStyleClass().add("project-card-desc");
        lblDesc.setWrapText(true);

        // Row 3: Type chip + duration
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label lblType = new Label(ts.getType().getValeur());
        lblType.getStyleClass().add("type-chip");

        Label lblDuration = new Label("⏱ " + ts.getDuration() + " jours");
        lblDuration.getStyleClass().add("project-card-meta");

        metaRow.getChildren().addAll(lblType, lblDuration);

        // Row 4: action buttons
        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(actionRow, new Insets(6, 0, 0, 0));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (isLeader) {
            Button btnManage = new Button("Gérer le projet →");
            btnManage.getStyleClass().add("btn-card-manage");
            btnManage.setOnAction(e -> naviguerVersGestion(ts));

            Button btnDelete = new Button("🗑");
            btnDelete.getStyleClass().add("btn-card-member");
            btnDelete.setOnAction(e -> supprimerTaskSpace(ts));

            actionRow.getChildren().addAll(spacer, btnDelete, btnManage);
        } else {
            Button btnView = new Button("Voir mes tâches assignées →");
            btnView.getStyleClass().add("btn-card-member");
            btnView.setPrefWidth(220);
            btnView.setOnAction(e -> naviguerVersTachesAssignees(ts));
            actionRow.getChildren().addAll(spacer, btnView);
        }

        card.getChildren().addAll(topRow, lblDesc, metaRow, actionRow);
        return card;
    }

    private Label makeEmptyLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("empty-state-label");
        return l;
    }

    private String statusStyleClass(StatutTaskSpace s) {
        if (s == null) return "badge-ferme";
        return switch (s.getValeur().toLowerCase()) {
            case "actif"    -> "badge-actif";
            case "en pause" -> "badge-en-pause";
            default         -> "badge-ferme";
        };
    }

    // ── Navigate to Kanban board for a given project ──────────
    private void naviguerVersGestion(model.task.TaskSpace ts) {
        // TODO: load project kanban FXML and pass ts
        System.out.println("Gérer le projet : " + ts.getNom());
    }

    private void naviguerVersTachesAssignees(model.task.TaskSpace ts) {
        System.out.println("Voir tâches de : " + ts.getNom());
    }

    private void supprimerTaskSpace(model.task.TaskSpace ts) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer le projet « " + ts.getNom() + " » et toutes ses tâches ?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                taskSpaceService.supprimer(ts.getId());
                chargerTaskSpaces();
            } catch (SQLException e) {
                System.err.println("Erreur suppression TaskSpace : " + e.getMessage());
            }
        }
    }

    public void rafraichir() {
        chargerTaskSpaces();
    }
}
