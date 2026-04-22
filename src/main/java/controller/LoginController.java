package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.User;
import service.UserService;
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
            Session.setCurrentUser(user);
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
            Parent root = FXMLLoader.load(getClass().getResource("/Task/board_hub.fxml"));
            txtEmail.getScene().setRoot(root);
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
