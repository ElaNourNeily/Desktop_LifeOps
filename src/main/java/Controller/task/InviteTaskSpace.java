package Controller.task;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.task.TaskSpace;
import model.user.User;
import service.user.Userservice;
import service.task.TaskSpaceMembreService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class InviteTaskSpace implements Initializable {

    @FXML private AnchorPane root;
    @FXML private Label lblBreadcrumbProject;
    @FXML private TextField searchField;

    @FXML private VBox searchResultsContainer;
    @FXML private Label lblNoResults;

    @FXML private VBox equipeSection;
    @FXML private VBox membresContainer;
    @FXML private Label lblAucunMembre;

    private TaskSpace taskSpace;
    private int currentUserId;
    private final Userservice userService = new Userservice();
    private final TaskSpaceMembreService membreService = new TaskSpaceMembreService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            rechercherUtilisateurs(newVal.trim());
        });
    }

    public void setTaskSpace(TaskSpace ts, int userId) {
        this.taskSpace = ts;
        this.currentUserId = userId;
        if (ts != null) {
            lblBreadcrumbProject.setText(ts.getNom());
            chargerMembres();
        }
    }

    private void rechercherUtilisateurs(String query) {
        searchResultsContainer.getChildren().clear();
        if (query.isEmpty()) {
            lblNoResults.setManaged(false);
            lblNoResults.setVisible(false);
            return;
        }

        try {
            List<User> results = userService.rechercherUser(query);
            if (results.isEmpty()) {
                lblNoResults.setManaged(true);
                lblNoResults.setVisible(true);
            } else {
                lblNoResults.setManaged(false);
                lblNoResults.setVisible(false);
                for (User u : results) {
                    if (u.getId() != currentUserId) {
                        searchResultsContainer.getChildren().add(creerResultRow(u));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche : " + e.getMessage());
        }
    }

    private HBox creerResultRow(User u) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("member-row");
        row.setPadding(new Insets(12, 16, 12, 16));

        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 20px;");

        VBox info = new VBox(2);
        Label lblNom = new Label(u.getNom() + " " + u.getPrenom());
        lblNom.getStyleClass().add("member-name");
        Label lblEmail = new Label(u.getEmail());
        lblEmail.getStyleClass().add("member-email");
        info.getChildren().addAll(lblNom, lblEmail);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button btnInviter = new Button("Inviter");
        btnInviter.getStyleClass().add("btn-invite");

        try {
            if (taskSpace != null && membreService.isMembre(taskSpace.getId(), u.getId())) {
                btnInviter.setText("Déjà membre");
                btnInviter.setDisable(true);
            } else {
                btnInviter.setOnAction(e -> inviterMembre(u, btnInviter));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        row.getChildren().addAll(avatar, info, btnInviter);
        return row;
    }

    @FXML
    void naviguerVersEquipe(ActionEvent event) {
        equipeSection.setManaged(true);
        equipeSection.setVisible(true);
        chargerMembres();
    }

    private void chargerMembres() {
        if (taskSpace == null) return;

        membresContainer.getChildren().clear();
        try {
            List<User> membres = membreService.recupererMembres(taskSpace.getId());
            if (membres.isEmpty()) {
                lblAucunMembre.setManaged(true);
                lblAucunMembre.setVisible(true);
            } else {
                lblAucunMembre.setManaged(false);
                lblAucunMembre.setVisible(false);
                for (User u : membres) {
                    membresContainer.getChildren().add(creerMembreRow(u));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox creerMembreRow(User u) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("member-row");
        row.setPadding(new Insets(12, 16, 12, 16));

        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 20px;");

        VBox info = new VBox(2);
        Label lblNom = new Label(u.getNom() + " " + u.getPrenom());
        lblNom.getStyleClass().add("member-name");
        Label lblEmail = new Label(u.getEmail());
        lblEmail.getStyleClass().add("member-email");
        info.getChildren().addAll(lblNom, lblEmail);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label roleBadge = new Label("Membre");
        roleBadge.getStyleClass().add("member-role-badge");

        Button btnRetirer = new Button("Retirer");
        btnRetirer.getStyleClass().add("btn-remove-member");
        btnRetirer.setOnAction(e -> retirerMembre(u));

        row.getChildren().addAll(avatar, info, roleBadge, btnRetirer);
        return row;
    }

    private void inviterMembre(User u, Button btn) {
        try {
            if (taskSpace != null) {
                membreService.ajouterMembre(taskSpace.getId(), u.getId());
                btn.setText("✓ Invité");
                btn.setDisable(true);
                showAlert(Alert.AlertType.INFORMATION, "Succès", u.getNom() + " a été ajouté au projet !");
                chargerMembres();
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter le membre : " + e.getMessage());
        }
    }

    private void retirerMembre(User u) {
        try {
            if (taskSpace != null) {
                membreService.retirerMembre(taskSpace.getId(), u.getId());
                chargerMembres();
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retirer le membre : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    void retourVersKanban(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/task/ReadTaskSpace.fxml"));
            Parent readRoot = loader.load();

            ReadTaskSpace ctrl = loader.getController();
            if (this.taskSpace != null) {
                ctrl.setTaskSpace(this.taskSpace, this.currentUserId, true);
            }

            root.getScene().setRoot(readRoot);
        } catch (IOException e) {
            System.err.println("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }
}