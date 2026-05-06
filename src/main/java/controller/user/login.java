package controller.user;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import Model.User;
import service.user.Userservice;
import utils.RememberMe;
import utils.Session;
import utils.Mailsender;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.json.JSONObject;

public class login {
    private Map<String, Integer> attemptsMap = new HashMap<>();

    @FXML
    private TextField email;

    @FXML
    private Label error;

    @FXML
    private PasswordField password;

    @FXML
    private CheckBox rememberMeCheckbox;

    private Userservice userservice = new Userservice();

    @FXML
    public void initialize() {
        String savedEmail = RememberMe.load();
        if (savedEmail != null) {
            email.setText(savedEmail);
            rememberMeCheckbox.setSelected(true);
        }
    }

    @FXML
    void LogIn(ActionEvent event) {
        String userEmail = email.getText();
        String userPassword = password.getText();

        try {
            User userInDb = userservice.findbyMail(userEmail);
            User testingUser = new User();
            testingUser.setEmail(userEmail);
            testingUser.setMot_de_passe(userPassword);

            if (userInDb != null && userInDb.getBanUntil() != null && userInDb.getBanUntil().isAfter(LocalDateTime.now())) {
                System.out.println("LOG: Utilisateur banni détecté : " + userEmail);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
                error.setText("Compte banni jusqu'au " + userInDb.getBanUntil().format(formatter));
                error.setVisible(true);
                return;
            } else if (userservice.recherche(testingUser)) {
                attemptsMap.remove(userEmail); // Reset attempts on success

                User loggedInUser = userservice.findbyMail(userEmail);
                Session.getInstance().setCurrentUser(loggedInUser);
                Session.getInstance().setLoginTime(java.time.LocalDateTime.now());

                System.out.println("Login successful — Welcome "
                        + loggedInUser.getPrenom() + " " + loggedInUser.getNom());

                if (rememberMeCheckbox != null && rememberMeCheckbox.isSelected()) {
                    RememberMe.save(loggedInUser.getEmail());
                }

                // Redirect based on role
                if ("ROLE_ADMIN".equals(loggedInUser.getRole())) {
                    navigateToBackOffice(event);
                } else {
                    navigateToMainLayout(event);
                }

            } else if (userEmail.isEmpty()) {
                error.setText("Email vide");
                error.setVisible(true);

            } else if (userPassword.isEmpty()) {
                error.setText("Mot de passe vide");
                error.setVisible(true);

            } else {
                // Handle failed attempts
                int attempts = attemptsMap.getOrDefault(userEmail, 0) + 1;
                attemptsMap.put(userEmail, attempts);
                System.out.println("DEBUG: Tentative échouée (" + attempts + "/3) pour " + userEmail);

                if (attempts >= 3) {
                    if (userInDb != null) {
                        System.out.println("DEBUG: Ban en cours pour l'ID: " + userInDb.getId());
                        userservice.ban(userInDb);
                        error.setText("Trop de tentatives. Compte banni pour 3 heures.");
                    } else {
                        System.out.println("DEBUG: Email non trouvé en DB, pas de ban possible.");
                        error.setText("Email ou mot de passe incorrect!");
                    }
                    attemptsMap.remove(userEmail);
                } else {
                    error.setText("Email ou mot de passe incorrect! (" + attempts + "/3)");
                }
                error.setVisible(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            error.setText("Erreur de base de données.");
            error.setVisible(true);
        }
    }

    @FXML
    void loginwithgoogle(ActionEvent event) {
        try {
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    "",
                    "",
                    Arrays.asList(
                            "https://www.googleapis.com/auth/userinfo.profile",
                            "https://www.googleapis.com/auth/userinfo.email"))
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            HttpRequest request = new NetHttpTransport()
                    .createRequestFactory(credential)
                    .buildGetRequest(new GenericUrl("https://www.googleapis.com/oauth2/v2/userinfo"));

            String response = request.execute().parseAsString();
            System.out.println(response);

            // Check if the Google user is banned in our system
            JSONObject jsonObj = new JSONObject(response);
            String googleEmail = jsonObj.getString("email");
            User userInDb = userservice.findbyMail(googleEmail);

            if (userInDb != null && userInDb.getBanUntil() != null && userInDb.getBanUntil().isAfter(LocalDateTime.now())) {
                System.out.println("LOG: Google Login bloqué par ban : " + googleEmail);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
                error.setText("Compte banni jusqu'au " + userInDb.getBanUntil().format(formatter));
                error.setVisible(true);
                return;
            }

            boolean isNewUser = createUserFromGoogle(response);

            if (isNewUser) {
                navigateToSetPassword(event);
            } else {
                if (rememberMeCheckbox != null && rememberMeCheckbox.isSelected()) {
                    RememberMe.save(Session.getInstance().getCurrentUser().getEmail());
                }

                User loggedInUser = Session.getInstance().getCurrentUser();
                if ("ROLE_ADMIN".equals(loggedInUser.getRole())) {
                    navigateToBackOffice(event);
                } else {
                    navigateToMainLayout(event);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (error != null) {
                error.setText("Échec de la connexion Google");
                error.setVisible(true);
            }
        }
    }

    private void navigateToMainLayout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/MainLayout.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToBackOffice(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/BackOffice.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToSetPassword(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/SetPassword.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void navtosignin(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/SignIn.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean createUserFromGoogle(String json) {
        try {
            JSONObject obj = new JSONObject(json);

            String userEmail = obj.getString("email");
            String name = obj.getString("name");

            // Split name
            String[] parts = name.split(" ");

            User user = new User();
            user.setEmail(userEmail);
            user.setPrenom(parts[0]);
            user.setNom(parts.length > 1 ? parts[1] : "");
            user.setMot_de_passe("GOOGLE_USER");
            user.setAge(0);

            User existing = userservice.findbyMail(userEmail);

            if (existing == null) {
                userservice.create(user);
                User created = userservice.findbyMail(userEmail);
                Session.getInstance().setCurrentUser(created);
                System.out.println("Google login — new user created: " + userEmail);
                return true; // new user → must set password
            } else if ("GOOGLE_USER".equals(existing.getMot_de_passe())) {
                Session.getInstance().setCurrentUser(existing);
                System.out.println("Google login — existing user without password: " + userEmail);
                return true; // user exists but hasn't set a password yet → must set password
            } else {
                Session.getInstance().setCurrentUser(existing);
                System.out.println("Google login — existing user: " + userEmail);
                return false; // existing user with password → go to main app
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @FXML
    void Motdepasseoublie(MouseEvent event) {
        String userEmail = email.getText();

        if (userEmail.isEmpty()) {
            error.setText("Veuillez saisir votre email.");
            error.setVisible(true);
            return;
        }

        try {
            User user = userservice.findbyMail(userEmail);
            if (user != null) {
                // Generate a 6-digit verification code
                String verificationCode = String.format("%06d", new Random().nextInt(1000000));

                // Store in session
                Session.getInstance().setVerificationCode(verificationCode);
                Session.getInstance().setResetPasswordEmail(userEmail);

                // Send email in a background thread to avoid UI freeze
                new Thread(() -> {
                    try {
                        Mailsender.sendEmail(userEmail, "Réinitialisation de mot de passe",
                                "Votre code de vérification est : " + verificationCode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

                System.out.println("Code de vérification envoyé à : " + userEmail);

                // Navigate to the reset password screen
                Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/ResetPassword.fxml")));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } else {
                error.setText("Email non trouvé.");
                error.setVisible(true);
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            if (error != null) {
                error.setText("Une erreur est survenue.");
                error.setVisible(true);
            }
        }
    }

}
