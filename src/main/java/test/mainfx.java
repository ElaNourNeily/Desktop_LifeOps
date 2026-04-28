package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.User;
import Service.Userservice;
import utils.RememberMe;
import utils.Session;

public class mainfx extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Check if user has a saved "remember me" session
        String savedEmail = RememberMe.load();

        if (savedEmail != null) {
            // Try to auto-login with the saved email
            Userservice userservice = new Userservice();
            User user = userservice.findbyMail(savedEmail);

            if (user != null) {
                // User still exists in DB — auto-login
                Session.getInstance().setCurrentUser(user);
                System.out.println("Auto-login: Welcome back " + user.getPrenom() + " " + user.getNom());

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainLayout.fxml"));
                Parent root = loader.load();
                stage.setTitle("LifeOps");
                stage.setScene(new Scene(root));
                stage.show();
                return;
            } else {
                // User was deleted from DB — clear stale token
                RememberMe.clear();
            }
        }

        // No saved session — show login screen
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
        Parent root = loader.load();
        stage.setTitle("LifeOps — Connexion");
        stage.setScene(new Scene(root));
        stage.show();
    }
}
