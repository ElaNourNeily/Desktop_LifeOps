package Controller.user;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.user.User;
import service.user.UserService;
import utils.RememberMe;
import utils.Session;

import java.io.IOException;

public class EditUserController {

    @FXML private TextField prenomField;
    @FXML private TextField nomField;
    @FXML private TextField ageField;
    @FXML private TextField telephoneField;
    @FXML private TextField emailField;
    @FXML private Label messageLabel;
    @FXML private Label displayNameLabel;
    @FXML private Label displayEmailLabel;
    @FXML private Label displayRoleLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        User user = Session.getInstance().getCurrentUser();
        if (user != null) {
            prenomField.setText(user.getPrenom() != null ? user.getPrenom() : "");
            nomField.setText(user.getNom() != null ? user.getNom() : "");
            ageField.setText(user.getAge() > 0 ? String.valueOf(user.getAge()) : "");
            telephoneField.setText(user.getTelephone() != null ? user.getTelephone() : "");
            emailField.setText(user.getEmail() != null ? user.getEmail() : "");
            displayNameLabel.setText(user.getFullName());
            displayEmailLabel.setText(user.getEmail() != null ? user.getEmail() : "");
            displayRoleLabel.setText(user.getRole() != null ? user.getRole() : "ROLE_USER");
        }
    }

    @FXML
    void handleSave(ActionEvent event) {
        User user = Session.getInstance().getCurrentUser();
        if (user == null) { showError("Erreur de session."); return; }

        String prenom = prenomField.getText().trim();
        String nom    = nomField.getText().trim();
        String ageText = ageField.getText().trim();
        String telephone = telephoneField.getText().trim();

        if (prenom.isEmpty() || nom.isEmpty()) {
            showError("Le prénom et le nom sont obligatoires.");
            return;
        }
        if (!prenom.matches("^[a-zA-ZÀ-ÿ ]+$") || !nom.matches("^[a-zA-ZÀ-ÿ ]+$")) {
            showError("Le prénom et le nom ne doivent contenir que des lettres.");
            return;
        }
        int age = 0;
        if (!ageText.isEmpty()) {
            try {
                age = Integer.parseInt(ageText);
                if (age < 1 || age > 120) { showError("L'âge doit être entre 1 et 120."); return; }
            } catch (NumberFormatException e) {
                showError("L'âge doit être un nombre valide."); return;
            }
        }

        user.setPrenom(prenom);
        user.setNom(nom);
        user.setAge(age);
        user.setTelephone(telephone.isEmpty() ? null : telephone);

        try {
            userService.update(user);
            Session.getInstance().setCurrentUser(user);
            // Refresh displayed name
            displayNameLabel.setText(user.getFullName());
            // Also update sidebar labels
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) ctrl.refreshUserInfo();
            showSuccess("Profil mis à jour avec succès !");
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    @FXML
    void handleChangePassword(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/user/modifymdp.fxml"));
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) ctrl.loadContent(view);
        } catch (IOException e) {
            showError("Erreur chargement : " + e.getMessage());
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        Session.getInstance().logout();
        RememberMe.clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/user/login.fxml"));
            prenomField.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        messageLabel.setText("⚠  " + msg);
        messageLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px; " +
                "-fx-background-color: rgba(239,68,68,0.1); " +
                "-fx-background-radius: 8; -fx-padding: 8 14;");
        messageLabel.setVisible(true);
    }

    private void showSuccess(String msg) {
        messageLabel.setText("✓  " + msg);
        messageLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px; " +
                "-fx-background-color: rgba(52,211,153,0.1); " +
                "-fx-background-radius: 8; -fx-padding: 8 14;");
        messageLabel.setVisible(true);
    }
}
