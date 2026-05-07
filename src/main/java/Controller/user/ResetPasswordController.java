package Controller.user;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import service.user.UserService;
import utils.Session;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public class ResetPasswordController {

    @FXML
    private TextField codeField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label statusLabel;

    private final UserService userService = new UserService();

    @FXML
    void handleReset(ActionEvent event) {
        String enteredCode = codeField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        String expectedCode = Session.getInstance().getVerificationCode();
        String userEmail = Session.getInstance().getResetEmail();

        if (enteredCode == null || !enteredCode.equals(expectedCode)) {
            showError("Le code de vérification est incorrect.");
            return;
        }

        if (newPassword == null || newPassword.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            userService.updatePasswordByEmail(userEmail, newPassword);

            // Automatically log in the user
            Model.user.User user = userService.findbyMail(userEmail);
            Session.getInstance().setCurrentUser(user);

            // Clear reset data after success
            Session.getInstance().clearResetData();

            System.out.println("Password reset successful — logging in user: " + userEmail);

            // Navigate to the main application
            navigateToMainLayout(event);

        } catch (SQLException e) {
            showError("Erreur lors de la mise à jour : " + e.getMessage());
        }
    }

    private void navigateToMainLayout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/user/MainLayout.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println("Navigation Error: " + e.getMessage());
        }
    }

    @FXML
    void backToLogin(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/user/login.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #f87171;");
        statusLabel.setVisible(true);
    }
}
