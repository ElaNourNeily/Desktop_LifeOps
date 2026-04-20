package Controller.task;

import enums.StatutTaskSpace;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import model.user.User;
import service.task.TaskSpaceService;
import service.user.Userservice;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class TaskSpace implements Initializable {

    @FXML private FlowPane flowLeader;
    @FXML private FlowPane flowMembre;
    @FXML private ComboBox<User> userCombo;

    private final TaskSpaceService taskSpaceService = new TaskSpaceService();
    private final Userservice userService = new Userservice();

    private int currentUserId = -1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerUsers();
        chargerTaskSpaces();
    }

    private void chargerUsers() {
        try {
            List<User> users = userService.findAll();
            if (userCombo != null) {
                userCombo.setItems(FXCollections.observableArrayList(users));

                userCombo.setConverter(new StringConverter<User>() {
                    @Override
                    public String toString(User user) {
                        if (user == null) return null;
                        return user.getNom() + " " + user.getPrenom();
                    }
                    @Override
                    public User fromString(String string) {
                        return null;
                    }
                });

                userCombo.setOnAction(event -> {
                    User selectedUser = userCombo.getValue();
                    if (selectedUser != null) {
                        currentUserId = selectedUser.getId();
                        chargerTaskSpaces();
                    }
                });
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement utilisateurs : " + e.getMessage());
        }
    }

    @FXML
    private void naviguerVersCreataskSpace() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/task/CreataskSpace.fxml"));
            flowLeader.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation CreataskSpace : " + e.getMessage());
        }
    }

    @FXML
    private void naviguerVersMesTaches() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/task/readtask.fxml"));
            flowLeader.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation ReadTask : " + e.getMessage());
        }
    }

    private void chargerTaskSpaces() {
        flowLeader.getChildren().clear();
        flowMembre.getChildren().clear();

        if (currentUserId == -1) {
            flowLeader.getChildren().add(makeEmptyLabel("Veuillez sélectionner un utilisateur en haut."));
            flowMembre.getChildren().add(makeEmptyLabel("Veuillez sélectionner un utilisateur en haut."));
            return;
        }

        try {
            List<model.task.TaskSpace> listLeader = taskSpaceService.recupererParUtilisateur(currentUserId);
            if (listLeader.isEmpty()) {
                flowLeader.getChildren().add(makeEmptyLabel("Aucun projet. Cliquez sur « Nouveau Projet » pour commencer."));
            } else {
                for (model.task.TaskSpace ts : listLeader) {
                    flowLeader.getChildren().add(creerCarteTaskSpace(ts, true));
                }
            }

            List<model.task.TaskSpace> listMembre = taskSpaceService.recupererProjetsMembre(currentUserId);
            if (listMembre.isEmpty()) {
                flowMembre.getChildren().add(makeEmptyLabel("Aucun projet partagé pour l'instant."));
            } else {
                for (model.task.TaskSpace ts : listMembre) {
                    flowMembre.getChildren().add(creerCarteTaskSpace(ts, false));
                }
            }

        } catch (SQLException e) {
            System.err.println("Erreur chargement TaskSpaces : " + e.getMessage());
        }
    }

    private VBox creerCarteTaskSpace(model.task.TaskSpace ts, boolean isLeader) {
        VBox card = new VBox(10);
        card.getStyleClass().add("project-card");
        card.setPadding(new Insets(18));
        card.setPrefWidth(360);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label lblNom = new Label(ts.getNom());
        lblNom.getStyleClass().add("project-card-name");
        lblNom.setWrapText(true);
        HBox.setHgrow(lblNom, Priority.ALWAYS);

        Label lblStatut = new Label(ts.getStatus() != null ? ts.getStatus().getValeur() : "Inconnu");
        lblStatut.getStyleClass().add(statusStyleClass(ts.getStatus()));
        topRow.getChildren().addAll(lblNom, lblStatut);

        String descText = (ts.getDescription() != null && !ts.getDescription().isBlank())
                ? ts.getDescription() : "Aucune description";
        Label lblDesc = new Label(descText);
        lblDesc.getStyleClass().add("project-card-desc");
        lblDesc.setWrapText(true);

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label lblType = new Label(ts.getType() != null ? ts.getType().getValeur() : "Projet");
        lblType.getStyleClass().add("type-chip");

        Label lblDuration = new Label("⏱ " + ts.getDuration() + " jours");
        lblDuration.getStyleClass().add("project-card-meta");

        metaRow.getChildren().addAll(lblType, lblDuration);

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

    private void naviguerVersGestion(model.task.TaskSpace ts) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/task/ReadTaskSpace.fxml"));
            Parent readRoot = loader.load();

            ReadTaskSpace ctrl = loader.getController();
            ctrl.setTaskSpace(ts, currentUserId, true);

            flowLeader.getScene().setRoot(readRoot);
        } catch (IOException e) {
            System.err.println("Erreur navigation ReadTaskSpace : " + e.getMessage());
        }
    }

    private void naviguerVersTachesAssignees(model.task.TaskSpace ts) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/task/ReadTaskSpace.fxml"));
            Parent readRoot = loader.load();

            ReadTaskSpace ctrl = loader.getController();
            ctrl.setTaskSpace(ts, currentUserId, false);

            flowLeader.getScene().setRoot(readRoot);
        } catch (IOException e) {
            System.err.println("Erreur navigation Taches Assignees : " + e.getMessage());
        }
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