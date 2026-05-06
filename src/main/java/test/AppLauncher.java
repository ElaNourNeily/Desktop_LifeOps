package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

public class AppLauncher extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Chargement du fichier ReadSante.fxml
        URL url = new File("src/main/resources/ReadSante.fxml").toURI().toURL();
        Parent root = FXMLLoader.load(url);

        Scene scene = new Scene(root);
        
        // Optionnel : Vous pouvez ajouter un thème lumineux par défaut 
        // root.getStyleClass().add("light-mode");

        primaryStage.setTitle("LifeOps - Gestion de Santé");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
