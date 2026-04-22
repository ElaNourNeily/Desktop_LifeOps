package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.Activite;
import model.Planning;
import service.ActiviteService;
import service.PlanningService;

import java.sql.SQLException;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DashboardController {

    private static DashboardController instance;

    public static DashboardController getInstance() {
        return instance;
    }

    @FXML
    private AnchorPane contentArea;
    @FXML
    private Label totalActivitiesLabel;
    @FXML
    private Label totalPlanningsLabel;
    @FXML
    private Label nextActivityLabel;

    private final ActiviteService activiteService = new ActiviteService();
    private final PlanningService planningService = new PlanningService();

    @FXML
    public void initialize() {
        instance = this;
        refreshData();
        handleHome(); // Load summary by default
    }

    public void setView(String fxml) {
        loadView(fxml);
    }

    private void refreshData() {
        try {
            List<Activite> activities = activiteService.recuperer();
            List<Planning> plannings = planningService.recuperer();

            if (totalActivitiesLabel != null)
                totalActivitiesLabel.setText(String.valueOf(activities.size()));
            if (totalPlanningsLabel != null)
                totalPlanningsLabel.setText(String.valueOf(plannings.size()));

            if (nextActivityLabel != null) {
                if (!activities.isEmpty()) {
                    nextActivityLabel.setText(activities.get(0).getTitre());
                } else {
                    nextActivityLabel.setText("Aucune activité");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/" + fxml));
            contentArea.getChildren().setAll(root);
            // Ensure the root fills the contentArea
            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleHome() {
        loadView("dashboard_home.fxml");
    }

    @FXML
    private void handleActivities() {
        loadView("planning_dashboard.fxml");
    }

    @FXML
    private void handlePlannings() {
        loadView("planning_management.fxml");
    }

    public void jumpToCalendar(java.time.LocalDate date) {
        PlanningDashboardController.setJumpToDate(date);
        loadView("planning_dashboard.fxml");
    }
}
