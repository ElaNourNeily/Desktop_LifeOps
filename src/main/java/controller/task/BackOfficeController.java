package controller.task;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import service.task.AdminAutomationService;

import java.util.concurrent.CompletableFuture;

public class BackOfficeController {

    @FXML private ToggleButton tglAutoAssign;
    @FXML private ToggleButton tglAutoRebalance;
    @FXML private ToggleButton tglAutoCleanup;
    @FXML private VBox alertsContainer;

    private final AdminAutomationService automationService = new AdminAutomationService();

    @FXML
    public void initialize() {
        setupToggles();
        loadAlerts(); // Dummy implementation based on real state in a real app
    }

    private void setupToggles() {
        // Fetch current status
        CompletableFuture.runAsync(() -> {
            boolean assign = automationService.getToggleStatus("auto_assign");
            boolean rebalance = automationService.getToggleStatus("auto_rebalance");
            boolean cleanup = automationService.getToggleStatus("auto_cleanup");

            Platform.runLater(() -> {
                setToggleState(tglAutoAssign, assign);
                setToggleState(tglAutoRebalance, rebalance);
                setToggleState(tglAutoCleanup, cleanup);

                // Add listeners AFTER setting initial state to avoid firing events
                bindToggle(tglAutoAssign, "auto_assign");
                bindToggle(tglAutoRebalance, "auto_rebalance");
                bindToggle(tglAutoCleanup, "auto_cleanup");
            });
        });
    }

    private void setToggleState(ToggleButton toggle, boolean state) {
        toggle.setSelected(state);
        toggle.setText(state ? "ON" : "OFF");
    }

    private void bindToggle(ToggleButton toggle, String apiName) {
        toggle.setOnAction(e -> {
            boolean newState = toggle.isSelected();
            toggle.setText(newState ? "ON" : "OFF");
            
            // Send update to FastAPI -> n8n
            CompletableFuture.runAsync(() -> {
                boolean success = automationService.updateToggle(apiName, newState);
                if (!success) {
                    Platform.runLater(() -> {
                        setToggleState(toggle, !newState); // revert if failed
                    });
                }
            });
        });
    }

    private void loadAlerts() {
        alertsContainer.getChildren().clear();

        // Simulate reading real problems from DB
        alertsContainer.getChildren().add(createAlertCard(
                "🔴 Critique",
                "3 Utilisateurs Surchargés",
                "Plus de 4 tâches en cours. Risque de retard élevé.",
                "trigger-rebalance",
                "Rééquilibrer Auto"
        ));

        alertsContainer.getChildren().add(createAlertCard(
                "🟠 Attention",
                "Deadlines Proches",
                "5 tâches risquent de dépasser la date limite (Probabilité > 80%).",
                "trigger-optimize",
                "Optimiser Deadlines"
        ));
    }

    private HBox createAlertCard(String severity, String title, String desc, String endpoint, String btnText) {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: #1e293b; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8;");

        VBox text = new VBox(5);
        Label lblTitle = new Label(severity + " - " + title);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label lblDesc = new Label(desc);
        lblDesc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        text.getChildren().addAll(lblTitle, lblDesc);

        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);

        Button btnFix = new Button("⚡ " + btnText);
        btnFix.setStyle("-fx-background-color: transparent; -fx-border-color: #8b5cf6; -fx-text-fill: #a78bfa; -fx-border-radius: 8; -fx-cursor: hand;");
        
        btnFix.setOnAction(e -> {
            btnFix.setText("⏳ Processing...");
            btnFix.setDisable(true);
            CompletableFuture.runAsync(() -> {
                boolean ok = automationService.triggerAction(endpoint);
                Platform.runLater(() -> {
                    if (ok) {
                        box.setVisible(false);
                        box.setManaged(false);
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "L'action n8n a été déclenchée avec succès.");
                    } else {
                        btnFix.setText("⚡ " + btnText);
                        btnFix.setDisable(false);
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de contacter n8n.");
                    }
                });
            });
        });

        box.getChildren().addAll(text, r, btnFix);
        return box;
    }

    @FXML
    private void actionAutoAssign(ActionEvent event) {
        triggerQuickAction("trigger-auto-assign", "Assignation Automatique");
    }

    @FXML
    private void actionRebalance(ActionEvent event) {
        triggerQuickAction("trigger-rebalance", "Rééquilibrage de Charge");
    }

    @FXML
    private void actionCleanup(ActionEvent event) {
        triggerQuickAction("trigger-cleanup", "Nettoyage des Utilisateurs");
    }

    @FXML
    private void actionOptimize(ActionEvent event) {
        triggerQuickAction("trigger-optimize", "Optimisation des Deadlines");
    }

    private void triggerQuickAction(String endpoint, String actionName) {
        CompletableFuture.runAsync(() -> {
            boolean ok = automationService.triggerAction(endpoint);
            Platform.runLater(() -> {
                if (ok) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Action '" + actionName + "' déclenchée sur n8n.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de déclencher l'action sur n8n.");
                }
            });
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
