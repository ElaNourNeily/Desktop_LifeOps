package controller.user;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import Model.User;
import service.user.Userservice;
import utils.Session;

import java.sql.SQLException;

public class EditUserController {

    @FXML
    private TextField nom;
    @FXML
    private TextField prenom;
    @FXML
    private TextField email;
    @FXML
    private TextField age;
    @FXML
    private TextField telephone;

    @FXML
    private PasswordField oldPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label nomerror;
    @FXML
    private Label p; // prenom error
    @FXML
    private Label invalidmail;
    @FXML
    private Label ageerror;
    @FXML
    private Label phoneerror;
    @FXML
    private Label pwdinvalid;
    @FXML
    private Label messageLabel;

    private Userservice userservice = new Userservice();
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = Session.getInstance().getCurrentUser();
        if (currentUser != null) {
            nom.setText(currentUser.getNom());
            prenom.setText(currentUser.getPrenom());
            email.setText(currentUser.getEmail());
            age.setText(String.valueOf(currentUser.getAge()));
            telephone.setText(currentUser.getTelephone());
        }
    }

    @FXML
    private void handleUpdate() {
        if (currentUser == null) return;
        int x = 0;

        try {
            // Validate Prenom
            if (prenom.getText().matches("^[a-zA-Z].*")) {
                p.setVisible(false);
                x++;
            } else {
                p.setVisible(true);
            }

            // Validate Nom
            if (nom.getText().matches("^[a-zA-Z].*")) {
                nomerror.setVisible(false);
                x++;
            } else {
                nomerror.setVisible(true);
            }

            // Validate Age
            if (age.getText().isEmpty() || Integer.parseInt(age.getText()) > 70 || Integer.parseInt(age.getText()) < 0) {
                ageerror.setVisible(true);
            } else {
                ageerror.setVisible(false);
                x++;
            }

            // Validate Email
            if (email.getText().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                // If email changed, check if it already exists
                if (!email.getText().equals(currentUser.getEmail()) && userservice.findbyMail(email.getText()) != null) {
                    invalidmail.setText("Email already exists");
                    invalidmail.setVisible(true);
                } else {
                    invalidmail.setVisible(false);
                    x++;
                }
            } else {
                invalidmail.setText("Invalid email");
                invalidmail.setVisible(true);
            }

            // Validate Telephone
            if (!telephone.getText().matches("^[0-9]+$") || telephone.getText().length() < 8) {
                phoneerror.setVisible(true);
            } else {
                phoneerror.setVisible(false);
                x++;
            }

            if (x == 5) {
                currentUser.setNom(nom.getText());
                currentUser.setPrenom(prenom.getText());
                currentUser.setEmail(email.getText());
                currentUser.setAge(Integer.parseInt(age.getText()));
                currentUser.setTelephone(telephone.getText());

                userservice.update(currentUser);
                showMessage("Profil mis à jour avec succès !", "#00d285");
            } else {
                showMessage("Veuillez corriger les erreurs.", "#ff4b4b");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Une erreur est survenue.", "#ff4b4b");
        }
    }

    @FXML
    private void handleChangePassword() {
        if (currentUser == null) return;

        String oldPass = oldPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showMessage("Veuillez remplir tous les champs.", "#ff4b4b");
            return;
        }

        if (!currentUser.getMot_de_passe().equals(oldPass)) {
            showMessage("Ancien mot de passe incorrect.", "#ff4b4b");
            return;
        }

        if (newPass.length() > 5) {
            pwdinvalid.setVisible(false);
            if (newPass.equals(confirmPass)) {
                try {
                    currentUser.setMot_de_passe(newPass);
                    userservice.updatePassword(currentUser);
                    oldPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                    showMessage("Mot de passe modifié !", "#00d285");
                } catch (SQLException e) {
                    showMessage("Erreur DB.", "#ff4b4b");
                }
            } else {
                showMessage("Les mots de passe ne correspondent pas.", "#ff4b4b");
            }
        } else {
            pwdinvalid.setVisible(true);
        }
    }

    private void showMessage(String text, String color) {
        messageLabel.setText(text);
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
        messageLabel.setVisible(true);
    }
}
