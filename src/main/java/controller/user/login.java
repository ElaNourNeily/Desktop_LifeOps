package Controller.user;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
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
import Model.user.User;
import service.user.UserService;
import utils.Session;
import utils.RememberMe;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

public class login {

    @FXML
    private Label error;

    @FXML
    private TextField email;

    @FXML
    private PasswordField password;

    @FXML
    private TextField passwordTextField;

    @FXML
    private Label errorLabel;

    @FXML
    private CheckBox rememberMeCheckbox;

    UserService userservice = new UserService();

    @FXML
    public void LogIn(ActionEvent event) {
        try {
            String emailText = email != null && email.getText() != null ? email.getText().trim() : "";

            String passwordText = "";
            if (password != null && password.isVisible()) {
                passwordText = password.getText();
            } else if (passwordTextField != null) {
                passwordText = passwordTextField.getText();
            }
            passwordText = passwordText != null ? passwordText.trim() : "";

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                error.setText("Aucun mot de passe ou email n’est fourni!!");
                error.setVisible(true);
                return;
            }

            // Check if user is banned before attempting login
            User existingUser = userservice.findbyMail(emailText);
            if (existingUser != null && existingUser.getBanUntil() != null) {
                if (existingUser.getBanUntil().isAfter(LocalDateTime.now())) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                    error.setText("Vous êtes banni jusqu'à " + existingUser.getBanUntil().format(formatter));
                    error.setVisible(true);
                    return;
                }
            }

            User testingUser = new User();
            testingUser.setEmail(emailText);
            testingUser.setMot_de_passe(passwordText);

            if (userservice.recherche(testingUser)) {
                // Fetch the full user from the DB and store in session
                User loggedInUser = userservice.findbyMail(emailText);

                // Clear ban status on successful login
                if (loggedInUser.getBanUntil() != null) {
                    userservice.updateBanUntil(loggedInUser.getId(), null);
                }

                Session.getInstance().setCurrentUser(loggedInUser);

                System.out.println("Login successful — Welcome "
                        + loggedInUser.getPrenom() + " " + loggedInUser.getNom());

                // Save remember-me if checkbox is selected
                if (rememberMeCheckbox != null && rememberMeCheckbox.isSelected()) {
                    RememberMe.save(loggedInUser.getEmail());
                }

                navigateToMainLayout(event);
            } else {
                // If the email exists but password was wrong, ban for 3 hours
                if (existingUser != null) {
                    LocalDateTime banUntil = LocalDateTime.now().plusHours(3);
                    userservice.updateBanUntil(existingUser.getId(), banUntil);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                    error.setText("Mot de passe incorrect. Vous êtes banni jusqu'à " + banUntil.format(formatter));
                } else {
                    error.setText("Email ou mot de passe incorrect!");
                }
                error.setVisible(true);
                System.out.println("Login unsuccessful — invalid email or password");
            }
        } catch (Exception e) {
            error.setText("Erreur: " + e.getMessage());
            error.setVisible(true);
            e.printStackTrace();
        }
    }

    @FXML
    public void loginwithgoogle(ActionEvent event) {
        try {
            String clientId     = System.getenv("GOOGLE_CLIENT_ID");
            String clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
            if (clientId == null)     clientId     = "YOUR_GOOGLE_CLIENT_ID";
            if (clientSecret == null) clientSecret = "YOUR_GOOGLE_CLIENT_SECRET";

            GoogleAuthorizationCodeFlow flow =
                    new GoogleAuthorizationCodeFlow.Builder(
                            new NetHttpTransport(),
                            JacksonFactory.getDefaultInstance(),
                            clientId,
                            clientSecret,
                            Arrays.asList(
                                    "https://www.googleapis.com/auth/userinfo.profile",
                                    "https://www.googleapis.com/auth/userinfo.email"
                            )
                    ).build();

            LocalServerReceiver receiver =
                    new LocalServerReceiver.Builder().setPort(8888).build();

            Credential credential =
                    new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            // Get user info from Google
            HttpRequest request = new NetHttpTransport()
                    .createRequestFactory(credential)
                    .buildGetRequest(new GenericUrl("https://www.googleapis.com/oauth2/v2/userinfo"));

            String response = request.execute().parseAsString();
            System.out.println(response);

            // Create or find user, store in session
            boolean isNewUser = createUserFromGoogle(response);

            if (isNewUser) {
                // First-time Google user → force password creation
                navigateToSetPassword(event);
            } else {
                // Save remember-me if checkbox is selected
                if (rememberMeCheckbox != null && rememberMeCheckbox.isSelected()) {
                    RememberMe.save(Session.getInstance().getCurrentUser().getEmail());
                }
                // Returning user → go to main app
                navigateToMainLayout(event);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (errorLabel != null) {
                errorLabel.setText("Échec de la connexion Google");
                errorLabel.setVisible(true);
            }
        }
    }

    @FXML
    public void navtosignin(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/user/SignIn.fxml")));

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Navigates to MainLayout.fxml after a successful login.
     */
    private void navigateToMainLayout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/MainLayout.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Navigates to SetPassword.fxml for first-time Google users.
     */
    private void navigateToSetPassword(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/SetPassword.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Creates a new user from Google profile data, or logs in an existing one.
     * Stores the user in Session either way.
     *
     * @return true if this is a brand-new user (needs to set a password)
     */
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

            // Check if user already exists
            User existing = userservice.findbyMail(userEmail);

            if (existing == null) {
                userservice.create(user);
                User created = userservice.findbyMail(userEmail);
                Session.getInstance().setCurrentUser(created);
                System.out.println("Google login — new user created: " + userEmail);
                return true; // new user → must set password
            } else {
                Session.getInstance().setCurrentUser(existing);
                System.out.println("Google login — existing user: " + userEmail);
                return false; // existing user → go to main app
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @FXML
    public void makevisivle(MouseEvent event) {
        if (password.isVisible()) {
            passwordTextField.setText(password.getText());
            passwordTextField.setVisible(true);
            password.setVisible(false);
        } else {
            password.setText(passwordTextField.getText());
            password.setVisible(true);
            passwordTextField.setVisible(false);
        }
    }

    @FXML
    void forget(MouseEvent event) {
        String userEmail = email.getText();

        if (userEmail == null || userEmail.isEmpty()) {
            error.setText("Veuillez saisir votre email d'abord !");
            error.setVisible(true);
            return;
        }

        try {
            User user = userservice.findbyMail(userEmail);
            if (user == null) {
                error.setText("Cet email n'existe pas dans notre système.");
                error.setVisible(true);
                return;
            }

            // Generate 6-digit code
            int code = (int) (Math.random() * 900000) + 100000;
            String verificationCode = String.valueOf(code);

            // Send Email
            String subject = "Réinitialisation de votre mot de passe - LifeOps";
            String body = "Bonjour " + user.getPrenom() + ",\n\n"
                    + "Vous avez demandé la réinitialisation de votre mot de passe.\n"
                    + "Votre code de vérification est : " + verificationCode + "\n\n"
                    + "Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet e-mail.\n\n"
                    + "L'équipe LifeOps.";

            utils.EmailService.sendEmail(userEmail, subject, body);

            // Store reset data in session
            Session.getInstance().setResetData(userEmail, verificationCode);

            // Success message and navigation
            error.setText("Code envoyé ! Vérification...");
            error.setStyle("-fx-text-fill: #34D399;");
            error.setVisible(true);

            System.out.println("Verification code for " + userEmail + ": " + verificationCode);

            // Navigate to Reset Password page
            navigateToResetPassword(event);

        } catch (Exception e) {
            error.setText("Erreur lors de l'envoi de l'email.");
            error.setVisible(true);
            e.printStackTrace();
        }
    }

    private void navigateToResetPassword(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/user/ResetPassword.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println("Navigation Error: " + e.getMessage());
        }
    }
}
