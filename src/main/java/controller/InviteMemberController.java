package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.User;
import service.UserService;
import service.TaskSpaceUserService;
import service.PusherService;
import java.util.HashMap;
import java.util.Map;

import java.util.List;

public class InviteMemberController {

    @FXML private TextField searchField;
    @FXML private ListView<User> usersList;
    @FXML private Label lblStatus;

    private int taskSpaceId;
    private final UserService userService = new UserService();
    private final TaskSpaceUserService spaceUserService = new TaskSpaceUserService();
    private final PusherService pusherService = new PusherService();

    public void setTaskSpaceId(int id) {
        this.taskSpaceId = id;
    }

    @FXML
    public void initialize() {
        // Start with empty list
        usersList.setItems(FXCollections.observableArrayList());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            lblStatus.setVisible(false); // hide previous status message
            
            if (newVal == null || newVal.trim().isEmpty()) {
                usersList.getItems().clear();
                return;
            }

            List<User> results = userService.searchUsers(newVal.trim());
            usersList.setItems(FXCollections.observableArrayList(results));

            if (results.isEmpty()) {
                showStatus("Aucun résultat trouvé.", "#94a3b8");
            }
        });

        // Setup custom cell factory for the ListView
        usersList.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);

                if (empty || user == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox hbox = new HBox();
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    hbox.setSpacing(10);
                    hbox.setStyle("-fx-padding: 8px; -fx-background-color: #2a2d32; -fx-background-radius: 8px;");

                    VBox infoBox = new VBox();
                    infoBox.setSpacing(3);
                    Label nameLbl = new Label(user.getFullName());
                    nameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                    Label emailLbl = new Label(user.getEmail());
                    emailLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
                    infoBox.getChildren().addAll(nameLbl, emailLbl);

                    HBox.setHgrow(infoBox, Priority.ALWAYS);

                    Button inviteBtn = new Button("Inviter");
                    inviteBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 6px; -fx-cursor: hand;");
                    
                    // Check if already a member
                    if (taskSpaceId > 0 && spaceUserService.getUserRoleInBoard(user.getId(), taskSpaceId) != null) {
                        inviteBtn.setText("Membre");
                        inviteBtn.setDisable(true);
                        inviteBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #64748b; -fx-background-radius: 6px;");
                    } else {
                        inviteBtn.setOnAction(e -> handleInvite(user, inviteBtn));
                    }

                    hbox.getChildren().addAll(infoBox, inviteBtn);
                    setGraphic(hbox);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 4px;");
                }
            }
        });
    }

    private void handleInvite(User user, Button inviteBtn) {
        spaceUserService.addMember(taskSpaceId, user.getId(), "MEMBER");
        showStatus(user.getFullName() + " ajouté avec succès !", "#10b981");

        // realtime: member-invited
        if (pusherService.isEnabled()) {
            Map<String, Object> data = new HashMap<>();
            data.put("boardId", taskSpaceId);
            data.put("userId", utils.Session.isLoggedIn() ? utils.Session.getCurrentUser().getId() : null);
            data.put("invitedUserId", user.getId());
            data.put("role", "MEMBER");
            pusherService.triggerEvent(pusherService.channelForBoard(taskSpaceId), "member-invited", data);
        }
        
        // Update button visually
        inviteBtn.setText("Membre");
        inviteBtn.setDisable(true);
        inviteBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #64748b; -fx-background-radius: 6px;");
    }

    private void showStatus(String message, String color) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        lblStatus.setVisible(true);
    }

    @FXML
    private void handleClose() {
        ((Stage) searchField.getScene().getWindow()).close();
    }
}
