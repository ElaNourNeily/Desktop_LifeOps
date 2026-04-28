package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainLayoutController {

    private static MainLayoutController instance;

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        instance = this;
        // Load the initial page (using board_hub since dashboard.fxml doesn't exist yet)
        loadPage("board_hub.fxml");
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    public void loadContent(Parent view) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(view);
    }

    public void showPopup(Parent popupView) {
        contentArea.getChildren().add(popupView);
    }

    public void closePopup() {
        if (contentArea.getChildren().size() > 1) {
            contentArea.getChildren().remove(contentArea.getChildren().size() - 1);
        }
    }

    public void loadPage(String fxml) {
        try {
            java.net.URL resource = getClass().getResource("/Task/" + fxml);
            if (resource == null) {
                System.err.println("File not found: /Task/" + fxml);
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            loadContent(view);

        } catch (IOException e) {
            System.err.println("Error loading page: " + fxml);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDashboardClick() {
        loadPage("dashboard.fxml");
    }

    @FXML
    private void handleTachesClick() {
        loadPage("board_hub.fxml");
    }

    @FXML
    private void handleFinanceClick() {
        // Placeholder for Finance module
        // loadPage("finance.fxml");
    }

    @FXML
    private void handleObjectifsClick() {
        // Placeholder for Objectifs module
        // loadPage("objectifs.fxml");
    }

    @FXML
    private void handleSanteClick() {
        // Placeholder for Sante module
        // loadPage("sante.fxml");
    }

    @FXML
    private void handleTempsClick() {
        // Placeholder for Temps module
        // loadPage("temps.fxml");
    }

    @FXML
    private void handleCompteClick() {
        loadPage("backoffice_dashboard.fxml");
    }
}
