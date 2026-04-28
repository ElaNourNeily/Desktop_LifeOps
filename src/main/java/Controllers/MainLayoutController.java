package Controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;

public class MainLayoutController {

    @FXML private StackPane contentArea;

    // Menu items
    @FXML private HBox menuTableauBord;
    @FXML private HBox menuTaches;
    @FXML private HBox menuFinance;
    @FXML private HBox menuObjectifs;
    @FXML private HBox menuSante;
    @FXML private HBox menuTemps;

    private HBox menuActif = null;

    @FXML
    public void initialize() {
        // Charger le Dashboard par défaut
        chargerVue("/Dashboard.fxml");

        // Marquer Tableau de bord comme actif
        HBox[] menus = {menuTableauBord, menuTaches, menuFinance, menuObjectifs, menuSante, menuTemps};
        for (HBox menu : menus) {
            ajouterHoverEffect(menu);
        }

        resetMenuStyles();
        activerMenu(menuTableauBord);
    }

    private void ajouterHoverEffect(HBox menu) {
        menu.setOnMouseEntered(e -> {
            if (menu != menuActif) {
                TranslateTransition tt = new TranslateTransition(Duration.millis(150), menu);
                tt.setToX(4);
                tt.play();
                menu.setStyle("-fx-padding: 12 16; -fx-cursor: hand; -fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 10;");
                if (menu.getChildren().size() > 1) {
                    javafx.scene.control.Label label = (javafx.scene.control.Label) menu.getChildren().get(1);
                    label.setStyle("-fx-text-fill: #c0c3c8; -fx-font-size: 14px;");
                }
            }
        });
        menu.setOnMouseExited(e -> {
            if (menu != menuActif) {
                TranslateTransition tt = new TranslateTransition(Duration.millis(150), menu);
                tt.setToX(0);
                tt.play();
                menu.setStyle("-fx-padding: 12 16; -fx-cursor: hand;");
                if (menu.getChildren().size() > 1) {
                    javafx.scene.control.Label label = (javafx.scene.control.Label) menu.getChildren().get(1);
                    label.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 14px;");
                }
            }
        });
    }

    @FXML
    void handleMenuClick(MouseEvent event) {
        HBox menuClique = (HBox) event.getSource();

        // Réinitialiser tous les styles
        resetMenuStyles();

        // Activer le menu cliqué
        activerMenu(menuClique);

        // Charger la vue correspondante avec animation
        if (menuClique == menuTableauBord) {
            chargerVueAnimee("/Dashboard.fxml");
        } else if (menuClique == menuTaches) {
            chargerVueAnimee("/Taches.fxml");
        } else if (menuClique == menuFinance) {
            chargerVueAnimee("/Finance.fxml");
        } else if (menuClique == menuObjectifs) {
            chargerVueAnimee("/Objectifs.fxml");
        } else if (menuClique == menuSante) {
            chargerVueAnimee("/Sante.fxml");
        } else if (menuClique == menuTemps) {
            chargerVueAnimee("/Temps.fxml");
        }
    }

    private void activerMenu(HBox menu) {
        menuActif = menu;
        menu.getStyleClass().clear();
        menu.getStyleClass().add("menu-item-active");
        menu.setStyle("-fx-padding: 12 16; -fx-cursor: hand; -fx-background-color: rgba(139, 92, 246, 0.1); -fx-background-radius: 10; -fx-border-color: #8b5cf6; -fx-border-width: 0 0 0 4px;");
        menu.setTranslateX(0);

        if (menu.getChildren().size() > 1) {
            javafx.scene.control.Label label = (javafx.scene.control.Label) menu.getChildren().get(1);
            label.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 14px; -fx-font-weight: bold;");
        }
    }

    private void resetMenuStyles() {
        HBox[] menus = {menuTableauBord, menuTaches, menuFinance, menuObjectifs, menuSante, menuTemps};
        
        for (HBox menu : menus) {
            menu.getStyleClass().clear();
            menu.getStyleClass().add("menu-item");
            menu.setStyle("-fx-padding: 12 16; -fx-cursor: hand;");
            menu.setTranslateX(0);
            
            // Remettre le texte en gris
            if (menu.getChildren().size() > 1) {
                javafx.scene.control.Label label = (javafx.scene.control.Label) menu.getChildren().get(1);
                label.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 14px;");
            }
        }
        menuActif = null;
    }

    private void chargerVueAnimee(String fxmlPath) {
        // Fade out current content, then load new
        if (!contentArea.getChildren().isEmpty()) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), contentArea);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                chargerVue(fxmlPath);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(250), contentArea);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
        } else {
            chargerVue(fxmlPath);
        }
    }

    private void chargerVue(String fxmlPath) {
        java.net.URL url = getClass().getResource(fxmlPath);
        if (url == null) {
            // Fichier FXML non trouvé → afficher message "en construction"
            String nomVue = fxmlPath.replace("/", "").replace(".fxml", "");
            javafx.scene.layout.VBox placeholder = new javafx.scene.layout.VBox(20);
            placeholder.setAlignment(javafx.geometry.Pos.CENTER);
            placeholder.setStyle("-fx-background-color: #121417;");

            javafx.scene.control.Label icone = new javafx.scene.control.Label("🚧");
            icone.setStyle("-fx-font-size: 52px;");

            javafx.scene.control.Label msg = new javafx.scene.control.Label(nomVue);
            msg.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

            javafx.scene.control.Label sub = new javafx.scene.control.Label("Cette section est en cours de développement");
            sub.setStyle("-fx-text-fill: #5a5d61; -fx-font-size: 14px;");

            placeholder.getChildren().addAll(icone, msg, sub);
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
}
