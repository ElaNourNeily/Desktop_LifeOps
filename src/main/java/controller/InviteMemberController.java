package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.User;
import service.UserService;
import service.TaskSpaceUserService;

public class InviteMemberController {

    @FXML private TextField txtEmail;
    @FXML private Label lblStatus;

    private int taskSpaceId;
    private final UserService userService = new UserService();
    private final TaskSpaceUserService spaceUserService = new TaskSpaceUserService();

    public void setTaskSpaceId(int id) {
        this.taskSpaceId = id;
    }

    @FXML
    private void handleInvite(ActionEvent event) {
        String email = txtEmail.getText().trim();
        if (email.isEmpty()) return;

        User user = userService.getUserByEmail(email);
        if (user == null) {
            showStatus("Utilisateur non trouvé.", "#ef4444");
            return;
        }

        // Check if already a member
        if (spaceUserService.getUserRoleInBoard(user.getId(), taskSpaceId) != null) {
            showStatus("L'utilisateur est déjà membre.", "#ef4444");
            return;
        }

        spaceUserService.addMember(taskSpaceId, user.getId(), "MEMBER");
        showStatus("Membre ajouté avec succès !", "#10b981");
        
        // Auto-close after 1 second? Or just allow adding more.
    }

    private void showStatus(String message, String color) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + color + ";");
        lblStatus.setVisible(true);
    }

    @FXML
    private void handleClose() {
        ((Stage) txtEmail.getScene().getWindow()).close();
    }
}
