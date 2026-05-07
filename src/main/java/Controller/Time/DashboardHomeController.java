package Controller.Time;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Time.Activite;
import service.Time.ActiviteService;
import service.Time.PlanningService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardHomeController {

    @FXML private VBox vboxTodayActivities;
    @FXML private VBox vboxReminders;
    @FXML private Label lblItemsLeft;
    @FXML private Label lblNextTask;
    @FXML private Label lblWeather;

    private final ActiviteService activiteService = new ActiviteService();
    private final PlanningService planningService = new PlanningService();
    private final service.Time.external.WeatherService weatherService = new service.Time.external.WeatherService();
    private int currentUserId = 1;

    @FXML
    public void initialize() {
        loadFirstUserId();
        refresh();
        refreshWeather();
    }

    private void refreshWeather() {
        if (lblWeather != null) {
            lblWeather.setCursor(javafx.scene.Cursor.HAND);
            lblWeather.setOnMouseClicked(e -> openWeatherDetails());
            
            new Thread(new javafx.concurrent.Task<String>() {
                @Override protected String call() {
                    return weatherService.getMeteoAujourdHui(36.8065, 10.1815);
                }
                @Override protected void succeeded() {
                    lblWeather.setText(getValue());
                }
            }).start();
        }
    }

    private void openWeatherDetails() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/Time/weather_details.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
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
        vboxReminders.getChildren().clear();
        try {
            // Correct logic: Find planning for TODAY only
            LocalDate today = LocalDate.now();
            model.Time.Planning todayPlanning = planningService.recupererParDate(today, currentUserId);
            
            if (todayPlanning != null) {
                List<Activite> todayTasks = activiteService.recupererParPlanning(todayPlanning.getId());
                
                // Sort by start time
                todayTasks.sort((a1, a2) -> a1.getHeureDebutEstimee().compareTo(a2.getHeureDebutEstimee()));

                for (Activite a : todayTasks) {
                    addActivityCard(a);
                    // Check for reminders
                    if (a.getMinutesRappel() > 0) {
                        addReminderCard(a);
                    }
                }

                lblItemsLeft.setText(todayTasks.stream().filter(a -> !"Terminé".equals(a.getStatutDynamique(java.sql.Date.valueOf(today)))).count() + " tâches");
                todayTasks.stream()
                    .filter(a -> "En cours".equals(a.getStatutDynamique(java.sql.Date.valueOf(today))))
                    .findFirst()
                    .ifPresentOrElse(
                        a -> lblNextTask.setText(a.getTitre()),
                        () -> lblNextTask.setText("Aucune")
                    );
            } else {
                lblItemsLeft.setText("0 tâches");
                lblNextTask.setText("Libre");
                Label lblEmpty = new Label("Aucune activité prévue pour aujourd'hui.");
                lblEmpty.setStyle("-fx-text-fill: #71717a; -fx-font-style: italic; -fx-padding: 20;");
                vboxTodayActivities.getChildren().add(lblEmpty);
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

        // Status badge
        String status = a.getStatutDynamique(java.sql.Date.valueOf(LocalDate.now()));
        Label lblStat = new Label(status);
        String sColor = "#71717a";
        if ("En cours".equals(status)) sColor = "#10b981";
        else if ("Terminé".equals(status)) sColor = "#3b82f6";
        lblStat.setStyle("-fx-text-fill: " + sColor + "; -fx-font-size: 10; -fx-font-weight: bold; -fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 4; -fx-padding: 2 6;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        card.getChildren().addAll(accent, texts, spacer, lblStat);
        vboxTodayActivities.getChildren().add(card);
    }

    private void addReminderCard(Activite a) {
        HBox card = new HBox(10);
        card.getStyleClass().add("home-activity-card");
        card.setStyle("-fx-background-color: rgba(245, 158, 11, 0.05); -fx-border-color: rgba(245, 158, 11, 0.1); -fx-border-radius: 10;");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label lblBell = new Label("🔔");
        VBox texts = new VBox(2);
        Label lblTitle = new Label(a.getTitre());
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");
        
        Label lblRappel = new Label("Rappel " + a.getMinutesRappel() + " min avant");
        lblRappel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 10;");

        texts.getChildren().addAll(lblTitle, lblRappel);
        card.getChildren().addAll(lblBell, texts);
        vboxReminders.getChildren().add(card);
    }

    @FXML
    private void handleSeeAll() {
        if (DashboardController.getInstance() != null) {
            DashboardController.getInstance().setView("planning_dashboard.fxml");
        }
    }
}
