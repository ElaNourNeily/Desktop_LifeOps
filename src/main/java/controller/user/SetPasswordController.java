package Controller.user;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import model.user.User;
import service.user.UserService;
import utils.Session;

public class SetPasswordController {

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    private final UserService userservice = new UserService();

    @FXML
    void confirmPassword(ActionEvent event) {
        String pwd = passwordField.getText();
        String confirmPwd = confirmPasswordField.getText();

        // Validate password length
        if (pwd.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        // Check passwords match
        if (!pwd.equals(confirmPwd)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        try {
            // Update the user's password in the session and database
            User currentUser = Session.getInstance().getCurrentUser();
            if (currentUser == null) {
                showError("Erreur de session — veuillez vous reconnecter");
                return;
            }

            currentUser.setMot_de_passe(pwd);
            userservice.updatePassword(currentUser);

            System.out.println("Password set successfully for: " + currentUser.getEmail());

            // Navigate to MainLayout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/MainLayout.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
