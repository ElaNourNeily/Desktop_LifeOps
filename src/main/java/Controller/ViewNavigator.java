package Controller;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class ViewNavigator {

    private ViewNavigator() {
    }

    public static void navigate(Event event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(ViewNavigator.class.getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger la vue : " + fxmlPath, e);
        }
    }
}