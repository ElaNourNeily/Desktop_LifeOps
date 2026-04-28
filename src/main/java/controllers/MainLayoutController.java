package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import model.User;
import utils.Session;

import java.io.IOException;
import java.util.Objects;

public class MainLayoutController {

    @FXML
    private StackPane contentArea;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label userEmailLabel;

    // Menu items
    @FXML
    private HBox menuTableauBord;
    @FXML
    private HBox menuTaches;
    @FXML
    private HBox menuFinance;
    @FXML
    private HBox menuObjectifs;
    @FXML
    private HBox menuSante;
    @FXML
    private HBox menuTemps;

    @FXML
    public void initialize() {
        // Charger le Dashboard par défaut
        chargerVue("/Dashboard.fxml");
        // Marquer Tableau de bord comme actif
        resetMenuStyles();
        menuTableauBord.setStyle("-fx-padding: 11 14; -fx-cursor: hand; -fx-background-color: #2d1f4e; -fx-background-radius: 8; -fx-border-color: #8b5cf6; -fx-border-width: 0 0 0 3px;");
        if (menuTableauBord.getChildren().size() > 1) {
            javafx.scene.control.Label label = (javafx.scene.control.Label) menuTableauBord.getChildren().get(1);
            label.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 14px; -fx-font-weight: bold;");
        }

        // Display the logged-in user's info from the session
        User currentUser = Session.getInstance().getCurrentUser();
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            userEmailLabel.setText(currentUser.getEmail());
        }
    }

    @FXML
    void handleMenuClick(MouseEvent event) {
        HBox menuClique = (HBox) event.getSource();

        // Réinitialiser tous les styles
        resetMenuStyles();

        // Activer le menu cliqué
        menuClique.getStyleClass().clear();
        menuClique.getStyleClass().add("menu-item-active");
        menuClique.setStyle("-fx-padding: 12 15; -fx-cursor: hand; -fx-background-color: #2d1f4e; -fx-background-radius: 8; -fx-border-color: #8b5cf6; -fx-border-width: 0 0 0 3px;");

        // Mettre le texte en violet clair et gras
        if (menuClique.getChildren().size() > 1) {
            javafx.scene.control.Label label = (javafx.scene.control.Label) menuClique.getChildren().get(1);
            label.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 14px; -fx-font-weight: bold;");
        }

        // Charger la vue correspondante
        if (menuClique == menuTableauBord) {
            chargerVue("/Dashboard.fxml");
        } else if (menuClique == menuTaches) {
            chargerVue("/Taches.fxml"); // À créer
        } else if (menuClique == menuFinance) {
            chargerVue("/Finance.fxml"); // À créer
        } else if (menuClique == menuObjectifs) {
            chargerVue("/Objectifs.fxml");
        } else if (menuClique == menuSante) {
            chargerVue("/Sante.fxml"); // À créer
        } else if (menuClique == menuTemps) {
            chargerVue("/Temps.fxml"); // À créer
        }
    }

    @FXML
    void handleLogout(MouseEvent event) {
        Session.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/login.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void resetMenuStyles() {
        HBox[] menus = {menuTableauBord, menuTaches, menuFinance, menuObjectifs, menuSante, menuTemps};

        for (HBox menu : menus) {
            menu.getStyleClass().clear();
            menu.getStyleClass().add("menu-item");
            menu.setStyle("-fx-padding: 12 15; -fx-cursor: hand;");

            // Remettre le texte en gris
            if (menu.getChildren().size() > 1) {
                javafx.scene.control.Label label = (javafx.scene.control.Label) menu.getChildren().get(1);
                label.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 14px;");
            }
        }
    }

    private void chargerVue(String fxmlPath) {
        java.net.URL url = getClass().getResource(fxmlPath);
        if (url == null) {
            // Fichier FXML non trouvé → afficher message "en construction"
            String nomVue = fxmlPath.replace("/", "").replace(".fxml", "");
            javafx.scene.layout.VBox placeholder = new javafx.scene.layout.VBox(15);
            placeholder.setAlignment(javafx.geometry.Pos.CENTER);
            placeholder.setStyle("-fx-background-color: #121417;");

            javafx.scene.control.Label icone = new javafx.scene.control.Label("🚧");
            icone.setStyle("-fx-font-size: 48px;");

            javafx.scene.control.Label msg = new javafx.scene.control.Label(nomVue + " — en cours de développement");
            msg.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 16px;");

            placeholder.getChildren().addAll(icone, msg);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(placeholder);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(url);
            Parent vue = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(vue);
        } catch (IOException e) {
            System.err.println("Erreur chargement : " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    void navtomodify(MouseEvent event) {

    }
}