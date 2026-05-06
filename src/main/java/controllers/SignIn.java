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
import java.util.Objects;
import java.util.Random;

import utils.Session;

public class SignIn {
    @FXML
    private Label nomerror;
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
    @FXML
    private Label ageerror;
    @FXML
    private Label invalidmail;
    @FXML
    private Label pwdinvalid;
    @FXML
    private Label phoneerror;

    // CAPTCHA fields
    @FXML
    private Label captchaLabel;
    @FXML
    private TextField captchaInput;
    @FXML
    private Label captchaError;

    private String generatedCaptcha;
    private Userservice userservice = new Userservice();

    @FXML
    public void initialize() {
        refreshCaptcha();
    }

    @FXML
    private void refreshCaptcha() {
        generatedCaptcha = generateRandomString(5);
        captchaLabel.setText(generatedCaptcha);
        // Add a slight random rotation to make it look like a real captcha
        captchaLabel.setRotate(new Random().nextInt(10) - 5);
        captchaInput.clear();
        captchaError.setVisible(false);
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Removed look-alike characters like 0, O, I, 1
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        while (sb.length() < length) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @FXML
    void SignUp(ActionEvent event) {
        int x = 0;
        try {
            User newUser = new User();

            // Validate Telephone
            if ((!telephone.getText().matches("^[0-9]+$")) || (telephone.getText().length() < 8)) {
                phoneerror.setVisible(true);
            } else {
                phoneerror.setVisible(false);
                newUser.setTelephone(telephone.getText());
                x++;
            }

            // Validate Prenom
            if (prenom.getText().matches("^[a-zA-Z].*")){
                p.setVisible(false);
                newUser.setPrenom(prenom.getText());
                x++;
            } else {
                p.setVisible(true);
            }

            // Validate Nom
            if (nom.getText().matches("^[a-zA-Z].*")){
                nomerror.setVisible(false);
                newUser.setNom(nom.getText());
                x++;
            } else {
                nomerror.setText("Format incorrect");
                nomerror.setVisible(true);
            }

            // Validate Age
            if ((age.getText().isEmpty()) || Integer.parseInt(age.getText()) > 70 || Integer.parseInt(age.getText()) < 0) {
                ageerror.setVisible(true);
            } else {
                ageerror.setVisible(false);
                newUser.setAge(Integer.parseInt(age.getText()));
                x++;
            }

            // Validate Email
            if (email.getText().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                if (userservice.findbyMail(email.getText()) != null) {
                    invalidmail.setText("Email already exists");
                    invalidmail.setVisible(true);
                } else {
                    invalidmail.setVisible(false);
                    newUser.setEmail(email.getText());
                    x++;
                }
            } else {
                invalidmail.setText("Invalid email");
                invalidmail.setVisible(true);
            }

            // Validate Password
            if (password.getText().length() > 5) {
                pwdinvalid.setVisible(false);
                newUser.setMot_de_passe(password.getText());
                x++;
            } else {
                pwdinvalid.setVisible(true);
            }

            // Validate CAPTCHA (The 7th validation)
            if (captchaInput.getText().equalsIgnoreCase(generatedCaptcha)) {
                captchaError.setVisible(false);
                x++;
            } else {
                captchaError.setVisible(true);
                refreshCaptcha(); // Change captcha on failure
            }

            if (x == 7) {
                userservice.create(newUser);
                System.out.println("Sign up successful!");
                navtolog_manual(event);
            }

        } catch (Exception e) {
            System.out.println("Sign up Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navtolog_manual(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/login.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void createwithgoogle(ActionEvent event) {
        try {
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                            new NetHttpTransport(),
                            JacksonFactory.getDefaultInstance(),
                    "",
                    "",
                            Arrays.asList(
                                    "https://www.googleapis.com/auth/userinfo.profile",
                                    "https://www.googleapis.com/auth/userinfo.email"
                            )
                    ).build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            HttpRequest request = new NetHttpTransport()
                    .createRequestFactory(credential)
                    .buildGetRequest(new GenericUrl("https://www.googleapis.com/oauth2/v2/userinfo"));

            String response = request.execute().parseAsString();
            System.out.println(response);

            boolean isNewUser = createUserFromGoogle(response);

            if (isNewUser) {
                navigateToSetPassword(event);
            } else {
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

            String[] parts = name.split(" ");
            User user = new User();
            user.setEmail(email);
            user.setPrenom(parts[0]);
            user.setNom(parts.length > 1 ? parts[1] : "");
            user.setMot_de_passe("GOOGLE_USER");
            user.setAge(0);

            User existing = userservice.findbyMail(email);

            if (existing == null) {
                userservice.create(user);
                User created = userservice.findbyMail(email);
                Session.getInstance().setCurrentUser(created);
                System.out.println("User created with Google!");
                return true;
            } else {
                Session.getInstance().setCurrentUser(existing);
                System.out.println("User already exists, logging in...");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
