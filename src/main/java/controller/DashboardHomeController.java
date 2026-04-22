package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Activite;
import service.ActiviteService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardHomeController {

    @FXML private VBox vboxTodayActivities;
    @FXML private Label lblItemsLeft;
    @FXML private Label lblNextTask;

    private final ActiviteService activiteService = new ActiviteService();
    private int currentUserId = 1;

    @FXML
    public void initialize() {
        loadFirstUserId();
        refresh();
    }

    private void loadFirstUserId() {
        try {
            java.sql.ResultSet rs = utils.MyDatabase.getInstance().getConnection()
                    .createStatement().executeQuery("SELECT id FROM utilisateur LIMIT 1");
            if (rs.next()) currentUserId = rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void refresh() {
        vboxTodayActivities.getChildren().clear();
        try {
            List<Activite> all = activiteService.recuperer();
            LocalDate today = LocalDate.now();
            
            // Filter activities for today
            List<Activite> todayTasks = all.stream()
                .filter(a -> a.getHeureDebutEstimee() != null) // Simplification for today's tasks
                .toList();

            for (Activite a : todayTasks) {
                addActivityCard(a);
            }

            lblItemsLeft.setText(todayTasks.size() + " tâches");
            if (!todayTasks.isEmpty()) {
                lblNextTask.setText(todayTasks.get(0).getTitre());
            }

        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addActivityCard(Activite a) {
        HBox card = new HBox(15);
        card.getStyleClass().add("home-activity-card");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Accent Bar
        Region accent = new Region();
        accent.setPrefWidth(4);
        accent.setPrefHeight(25);
        accent.getStyleClass().add("home-accent-bar");
        if (a.getCouleur() != null) {
            accent.setStyle("-fx-background-color: " + a.getCouleur() + ";");
        }

        VBox texts = new VBox(2);
        Label lblTitle = new Label(a.getTitre());
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        
        String time = a.getHeureDebutEstimee().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + 
                     " - " + a.getHeureFinEstimee().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label lblTime = new Label(time);
        lblTime.setStyle("-fx-text-fill: #71717a; -fx-font-size: 11;");
        
        texts.getChildren().addAll(lblTitle, lblTime);

        card.getChildren().addAll(accent, texts);
        vboxTodayActivities.getChildren().add(card);
    }

    @FXML
    private void handleSeeAll() {
        if (DashboardController.getInstance() != null) {
            DashboardController.getInstance().setView("planning_dashboard.fxml");
        }
    }
}
