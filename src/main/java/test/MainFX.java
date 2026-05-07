package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.user.User;
import service.user.UserService;
import utils.RememberMe;
import utils.Session;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Check for a saved "remember me" session
        String savedEmail = RememberMe.load();
        if (savedEmail != null) {
            UserService userService = new UserService();
            User user = userService.getUserByEmail(savedEmail);
            if (user != null) {
                Session.getInstance().setCurrentUser(user);
                Parent root = FXMLLoader.load(getClass().getResource("/user/MainLayout.fxml"));
                stage.setTitle("LifeOps");
                stage.setScene(new Scene(root));
                stage.show();
                return;
            }
        }

        // No saved session → show login
        Parent root = FXMLLoader.load(getClass().getResource("/user/login.fxml"));
        stage.setTitle("LifeOps - Connexion");
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
