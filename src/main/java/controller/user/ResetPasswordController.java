package controller.user;

import service.user.Userservice;
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
import Model.User;
import utils.Session;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public class ResetPasswordController {

    @FXML
    private TextField codeField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    private final Userservice userservice = new Userservice();

    @FXML
    void resetPassword(ActionEvent event) {
        String enteredCode = codeField.getText();
        String newPassword = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        String actualCode = Session.getInstance().getVerificationCode();
        String userEmail = Session.getInstance().getResetPasswordEmail();

        if (enteredCode.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        if (!enteredCode.equals(actualCode)) {
            showError("Code de vérification incorrect.");
            return;
        }

        if (newPassword.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            User user = userservice.findbyMail(userEmail);
            if (user != null) {
                user.setMot_de_passe(newPassword);
                userservice.updatePassword(user);

                System.out.println("Mot de passe réinitialisé avec succès pour : " + userEmail);

                // Clear reset session data
                Session.getInstance().setVerificationCode(null);
                Session.getInstance().setResetPasswordEmail(null);

                // Success! Navigate back to login
                navigateToLoginScreen(event);
            } else {
                showError("Utilisateur non trouvé.");
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showError("Erreur lors de la mise à jour du mot de passe.");
        }
    }

    @FXML
    void navToLogin(MouseEvent event) {
        try {
            navigateToLoginScreen(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToLoginScreen(Object eventSource) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/login.fxml")));
        Stage stage;
        if (eventSource instanceof MouseEvent) {
            stage = (Stage) ((Node) ((MouseEvent) eventSource).getSource()).getScene().getWindow();
        } else if (eventSource instanceof ActionEvent) {
            stage = (Stage) ((Node) ((ActionEvent) eventSource).getSource()).getScene().getWindow();
        } else {
            stage = (Stage) codeField.getScene().getWindow();
        }
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
