package Controllers;

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
import model.User;
import Service.Userservice;
import utils.Session;
import utils.RememberMe;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

public class login {

    @FXML
    private TextField email;

    @FXML
    private PasswordField password;

    @FXML
    private Label errorLabel;

    @FXML
    private CheckBox rememberMeCheckbox;

    Userservice userservice = new Userservice();

    @FXML
    void LogIn(ActionEvent event) {
        try {
            User testingUser = new User();
            testingUser.setEmail(email.getText());
            testingUser.setMot_de_passe(password.getText());

            if (userservice.recherche(testingUser)) {
                // Fetch the full user from the DB and store in session
                User loggedInUser = userservice.findbyMail(email.getText());
                Session.getInstance().setCurrentUser(loggedInUser);

                System.out.println("Login successful — Welcome "
                        + loggedInUser.getPrenom() + " " + loggedInUser.getNom());

                // Save remember-me if checkbox is selected
                if (rememberMeCheckbox != null && rememberMeCheckbox.isSelected()) {
                    RememberMe.save(loggedInUser.getEmail());
                }

                navigateToMainLayout(event);
            } else {
                System.out.println("Login unsuccessful — invalid email or password");
                if (errorLabel != null) {
                    errorLabel.setText("Email ou mot de passe incorrect");
                    errorLabel.setVisible(true);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    void loginwithgoogle(ActionEvent event) {
        try {
            GoogleAuthorizationCodeFlow flow =
                    new GoogleAuthorizationCodeFlow.Builder(
                            new NetHttpTransport(),
                            JacksonFactory.getDefaultInstance(),
                            "",
                            "",
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
    void navtosignin(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/SignIn.fxml")));

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainLayout.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SetPassword.fxml"));
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
            user.setMot_de_passe("GOOGLE_USER"); // placeholder
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
}
