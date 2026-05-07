package Controller;

import Controller.user.MainLayoutController;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public final class ViewNavigator {

    private ViewNavigator() {
    }

    /**
     * Loads an FXML into the MainLayout contentArea.
     * Falls back to replacing the whole scene if MainLayoutController is not available.
     */
    public static void navigate(Event event, String fxmlPath, String title) {
        try {
            Parent view = FXMLLoader.load(ViewNavigator.class.getResource(fxmlPath));
            MainLayoutController layout = MainLayoutController.getInstance();
            if (layout != null) {
                layout.loadContent(view);
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger la vue : " + fxmlPath, e);
        }
    }
}