package controller.user;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import model.user.User;
import org.mindrot.jbcrypt.BCrypt;
import service.user.UserService;
import utils.Session;

import java.io.IOException;

public class ModifyPasswordController {

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private final UserService userService = new UserService();

    @FXML
    void handleConfirm(ActionEvent event) {
        User user = Session.getInstance().getCurrentUser();
        if (user == null) { showError("Erreur de session."); return; }

        String current = currentPasswordField.getText();
        String newPwd  = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (current.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
            showError("Tous les champs sont obligatoires."); return;
        }

        // Verify current password (BCrypt-aware)
        String dbPass = user.getMot_de_passe();
        boolean currentOk;
        if (dbPass != null && (dbPass.startsWith("$2a$") || dbPass.startsWith("$2y$"))) {
            String checkPass = dbPass.replaceFirst("^\\$2y\\$", "\\$2a\\$");
            try { currentOk = BCrypt.checkpw(current, checkPass); }
            catch (Exception e) { currentOk = false; }
        } else {
            currentOk = current.equals(dbPass);
        }

        if (!currentOk) { showError("Le mot de passe actuel est incorrect."); return; }
        if (newPwd.length() < 6) { showError("Le nouveau mot de passe doit contenir au moins 6 caractères."); return; }
        if (!newPwd.equals(confirm)) { showError("Les mots de passe ne correspondent pas."); return; }

        try {
            user.setMot_de_passe(newPwd);
            userService.updatePassword(user);
            Session.getInstance().setCurrentUser(user);
            // Go back to profile page
            Parent view = FXMLLoader.load(getClass().getResource("/user/edituser.fxml"));
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) ctrl.loadContent(view);
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/user/edituser.fxml"));
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) ctrl.loadContent(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
    }
}
