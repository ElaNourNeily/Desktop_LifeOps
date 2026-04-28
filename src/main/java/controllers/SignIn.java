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
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import model.User;
import Service.Userservice;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import utils.Session;

public class SignIn {

    @FXML
    private TextField prenom;

    @FXML
    private TextField nom;

    @FXML
    private TextField age;
    @FXML
    private Label p;

    @FXML
    private TextField telephone;

    @FXML
    private TextField email;

    @FXML
    private PasswordField password;

    private Userservice userservice = new Userservice();

    @FXML
    void SignUp(ActionEvent event) {
        int x = 0;
        try {
            User newUser = new User();
            if (prenom.getText().matches("^[a-zA-Z].*")){
                newUser.setPrenom(prenom.getText());
                x+=1;
                newUser.setNom(nom.getText());

            }else {
                p.setVisible(true);
            }
            if (nom.getText().matches("^[a-zA-Z].*")){
                newUser.setNom(nom.getText());
                x+=1;

            }

            
            if (!age.getText().isEmpty()) {
                newUser.setAge(Integer.parseInt(age.getText()));
                x+=1;

            }
            if (email.getText().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                newUser.setEmail(email.getText());
                x+=1;

            } else {
                System.out.println("Invalid email format");
            }
            if(password.getText().length()>5){
                newUser.setMot_de_passe(password.getText());
                x+=1;


            }else System.out.println("Invalid password format");
            if (x==5){
                userservice.create(newUser);
                System.out.println("Sign in successful!");
            }

        } catch (Exception e) {
            System.out.println("Sign in Failed: " + e.getMessage());
        }
    }

    @FXML
    void createwithgoogle(ActionEvent event) {
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

            // Get user info
            HttpRequest request = new NetHttpTransport()
                    .createRequestFactory(credential)
                    .buildGetRequest(new GenericUrl("https://www.googleapis.com/oauth2/v2/userinfo"));

            String response = request.execute().parseAsString();
            System.out.println(response);

            boolean isNewUser = createUserFromGoogle(response);

            if (isNewUser) {
                // First-time Google user → force password creation
                navigateToSetPassword(event);
            } else {
                // Returning user → go to main app
                navigateToMainLayout(event);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void navtolog(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/login.fxml")));

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

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

    private boolean createUserFromGoogle(String json) {
        try {
            JSONObject obj = new JSONObject(json);

            String email = obj.getString("email");
            String name = obj.getString("name");
            String googleId = obj.getString("id");

            // Split name (simple way)
            String[] parts = name.split(" ");

            User user = new User();
            user.setEmail(email);
            user.setPrenom(parts[0]);
            user.setNom(parts.length > 1 ? parts[1] : "");
            user.setMot_de_passe("GOOGLE_USER"); // placeholder
            user.setAge(0); // optional

            // Check if user already exists
            User existing = userservice.findbyMail(email);

            if (existing == null) {
                userservice.create(user);
                User created = userservice.findbyMail(email);
                Session.getInstance().setCurrentUser(created);
                System.out.println("User created with Google!");
                return true; // new user → must set password
            } else {
                Session.getInstance().setCurrentUser(existing);
                System.out.println("User already exists, logging in...");
                return false; // existing user
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
