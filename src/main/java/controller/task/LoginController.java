package Controller.task;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import Model.user.User;
import service.user.UserService;
import utils.Session;

import java.io.IOException;

public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    private final UserService userService = new UserService();

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        User user = userService.login(email, password);

        if (user != null) {
            Session.getInstance().setCurrentUser(user);
            openMainApp();
        } else {
            // Check if the login failed because of has_set_password
            User existingUser = userService.getUserByEmail(email);
            if (existingUser != null && !existingUser.hasSetPassword()) {
                showError("Veuillez vous connecter via Google/Facebook (Mot de passe non défini).");
            } else {
                showError("Email ou mot de passe incorrect.");
            }
        }
    }

    private void openMainApp() {
        try {
            // Navigate to the unified MainLayout (user management version)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/MainLayout.fxml"));
            Parent root = loader.load();

            javafx.stage.Stage stage = (javafx.stage.Stage) txtEmail.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de l'application.");
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }
}
