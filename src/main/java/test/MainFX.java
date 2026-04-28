package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL viewUrl = getClass().getResource("/finance/finance.fxml");
        if (viewUrl == null) {
            throw new IllegalStateException("Vue introuvable: /finance/finance.fxml");
        }

        FXMLLoader loader = new FXMLLoader(viewUrl);
        Parent root = loader.load();

        Scene scene = new Scene(root);
        stage.setTitle("LifeOps");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
