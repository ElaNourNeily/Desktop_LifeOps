package Controller.user;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import Model.user.User;
import utils.RememberMe;
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
    private HBox menuBackOffice;

    private static MainLayoutController instance;

    @FXML
    public void initialize() {
        instance = this;
        
        // Display the logged-in user's info from the session
        User currentUser = Session.getInstance().getCurrentUser();
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            userEmailLabel.setText(currentUser.getEmail());
            
            // Show/hide BackOffice menu based on user role
            if (menuBackOffice != null) {
                menuBackOffice.setVisible(currentUser.isAdmin());
                menuBackOffice.setManaged(currentUser.isAdmin());
            }
        }
        
        // Load the Dashboard by default
        chargerVue("/Objectifs/Dashboard.fxml");
        // Mark Dashboard as active
        setActiveMenu(menuTableauBord);
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

    @FXML
    void handleMenuClick(MouseEvent event) {
        HBox menuClique = (HBox) event.getSource();

        // Reset all styles
        resetMenuStyles();

        // Activate clicked menu
        setActiveMenu(menuClique);

        // Load corresponding view
        if (menuClique == menuTableauBord) {
            chargerVue("/Objectifs/Dashboard.fxml");
        } else if (menuClique == menuTaches) {
            chargerVueTask("/Task/board_hub.fxml");
        } else if (menuClique == menuFinance) {
            chargerVue("/finance/finance.fxml");
        } else if (menuClique == menuObjectifs) {
            chargerVue("/Objectifs/Objectifs.fxml");
        } else if (menuClique == menuSante) {
            chargerVue("/health/Sante.fxml");
        } else if (menuClique == menuTemps) {
            chargerVue("/Time/dashboard.fxml");
        } else if (menuClique == menuBackOffice) {
            User currentUser = Session.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.isAdmin()) {
                chargerVueTask("/Task/backoffice_dashboard.fxml");
            }
        }
    }

    private void setActiveMenu(HBox menu) {
        if (menu == null) return;
        menu.getStyleClass().clear();
        menu.getStyleClass().add("nav-item-active");
        // Activate the label (second child)
        if (menu.getChildren().size() > 1) {
            javafx.scene.control.Label label = (javafx.scene.control.Label) menu.getChildren().get(1);
            label.getStyleClass().clear();
            label.getStyleClass().add("nav-label-active");
        }
    }

    @FXML
    void handleLogout(MouseEvent event) {
        Session.getInstance().logout();
        try {
            RememberMe.clear();
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/user/login.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void resetMenuStyles() {
        HBox[] menus = {menuTableauBord, menuTaches, menuFinance, menuObjectifs, menuSante, menuTemps, menuBackOffice};

        for (HBox menu : menus) {
            if (menu == null) continue;
            menu.getStyleClass().clear();
            menu.getStyleClass().add("nav-item");

            // Reset label to gray nav-label style
            if (menu.getChildren().size() > 1) {
                javafx.scene.control.Label label = (javafx.scene.control.Label) menu.getChildren().get(1);
                label.getStyleClass().clear();
                label.getStyleClass().add("nav-label");
            }
        }
    }

    private void chargerVue(String fxmlPath) {
        java.net.URL url = getClass().getResource(fxmlPath);
        if (url == null) {
            // FXML file not found → show "under construction" message
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

    private void chargerVueTask(String fxmlPath) {
        java.net.URL url = getClass().getResource(fxmlPath);
        if (url == null) {
            // FXML file not found → show "under construction" message
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
        try {
            Parent view = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/user/edituser.fxml")));
            loadContent(view);
            resetMenuStyles(); // deactivate all menu items
        } catch (IOException e) {
            System.err.println("Erreur chargement edituser.fxml : " + e.getMessage());
        }
    }

    /** Called by EditUserController after saving to refresh sidebar labels. */
    public void refreshUserInfo() {
        User currentUser = Session.getInstance().getCurrentUser();
        if (currentUser != null) {
            if (userNameLabel != null) userNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            if (userEmailLabel != null) userEmailLabel.setText(currentUser.getEmail());
        }
    }
}