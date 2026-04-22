package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Planning;
import service.ActiviteService;
import service.PlanningService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlanningManagementController {

    @FXML private VBox vboxPlanningList;
    @FXML private BarChart<String, Number> barChartWeeklyWork;
    @FXML private PieChart pieChartPriority;
    @FXML private Label lblEfficiency;
    @FXML private AnchorPane efficiencyProgress;

    private final PlanningService service = new PlanningService();
    private final ActiviteService activiteService = new ActiviteService();
    private int currentUserId = 1;

    @FXML
    public void initialize() {
        loadFirstUserId();
        refreshAll();
    }

    private void loadFirstUserId() {
        try {
            java.sql.ResultSet rs = utils.MyDatabase.getInstance().getConnection()
                    .createStatement().executeQuery("SELECT id FROM utilisateur LIMIT 1");
            if (rs.next()) currentUserId = rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void refreshAll() {
        refreshPlanningList();
        refreshStatistics();
    }

    private void refreshPlanningList() {
        System.out.println("[DEBUG] Refreshing Planning List for User ID: " + currentUserId);
        vboxPlanningList.getChildren().clear();
        try {
            List<Planning> all = service.recupererParUtilisateur(currentUserId);
            System.out.println("[DEBUG] Found " + all.size() + " planning records.");
            all.sort((a, b) -> b.getDate().compareTo(a.getDate()));

            String lastWeekKey = "";
            for (Planning p : all) {
                LocalDate localDate = p.getDate().toLocalDate();
                int weekNum = localDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                String weekKey = "W" + weekNum + " SEMAINE DU " + localDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)).toUpperCase();

                if (!weekKey.equals(lastWeekKey)) {
                    addWeekHeader(weekKey);
                    lastWeekKey = weekKey;
                }
                addPlanningItem(p);
            }
            vboxPlanningList.requestLayout(); 
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addWeekHeader(String title) {
        HBox header = new HBox();
        header.setStyle("-fx-background-color: rgba(139, 92, 246, 0.05); -fx-padding: 10 20; -fx-background-radius: 10;");
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label lblWeek = new Label(title.substring(0, 3));
        lblWeek.setStyle("-fx-background-color: rgba(139, 92, 246, 0.2); -fx-text-fill: #a78bfa; -fx-padding: 5 10; -fx-background-radius: 8; -fx-font-weight: bold;");
        
        Label lblTitle = new Label(title.substring(4));
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 0 0 0 15;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label lblLink = new Label("Vue hebdomadaire →");
        lblLink.setStyle("-fx-text-fill: #8b5cf6; -fx-font-size: 11; -fx-font-weight: bold; -fx-cursor: hand;");

        header.getChildren().addAll(lblWeek, lblTitle, spacer, lblLink);
        vboxPlanningList.getChildren().add(header);
    }

    private void addPlanningItem(Planning p) {
        HBox item = new HBox(20); // More spacing
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        item.setStyle("-fx-padding: 18 30; " +
                     "-fx-background-color: rgba(255,255,255,0.01); " +
                     "-fx-border-color: transparent transparent rgba(255,255,255,0.03) transparent; " +
                     "-fx-background-radius: 12;");
        
        // Date Column
        VBox colDate = new VBox(2);
        colDate.setPrefWidth(160);
        Label lblDate = new Label(p.getDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblDate.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        Label lblDay = new Label(p.getDate().toLocalDate().format(DateTimeFormatter.ofPattern("EEEE", Locale.FRENCH)).toUpperCase());
        lblDay.setStyle("-fx-text-fill: #71717a; -fx-font-size: 10; -fx-font-weight: bold; -fx-letter-spacing: 1;");
        colDate.getChildren().addAll(lblDate, lblDay);

        // Time Range Column
        HBox colTime = new HBox(12);
        colTime.setPrefWidth(220);
        colTime.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblIcon = new Label("🕒");
        lblIcon.setStyle("-fx-text-fill: #8b5cf6; -fx-font-size: 14;");
        Label lblRange = new Label(p.getHeureDebutJournee().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " — " + p.getHeureFinJournee().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        lblRange.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-weight: bold; -fx-font-size: 13;");
        colTime.getChildren().addAll(lblIcon, lblRange);

        // Status Tag
        HBox colStatus = new HBox(8);
        colStatus.setPrefWidth(130);
        colStatus.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        boolean isDispo = p.isDisponibilite();
        String statusColor = isDispo ? "#10b981" : "#f43f5e";
        String statusBg = isDispo ? "rgba(16, 185, 129, 0.08)" : "rgba(244, 63, 94, 0.08)";
        colStatus.setStyle("-fx-background-color: " + statusBg + "; -fx-background-radius: 20; -fx-padding: 6 15;");
        
        Label dot = new Label("•"); dot.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 16;");
        Label lblStatus = new Label(isDispo ? "DISPONIBLE" : "OCCUPÉ"); 
        lblStatus.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-weight: bold; -fx-font-size: 10;");
        colStatus.getChildren().addAll(dot, lblStatus);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // ACTION BUTTONS
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button btnCalendar = new Button("📅");
        btnCalendar.getStyleClass().add("btn-ghost");
        btnCalendar.setStyle("-fx-font-size: 14;");
        btnCalendar.setOnAction(e -> {
            if (DashboardController.getInstance() != null) {
                DashboardController.getInstance().jumpToCalendar(p.getDate().toLocalDate());
            }
        });

        Button btnEdit = new Button("✎");
        btnEdit.getStyleClass().add("btn-ghost");
        btnEdit.setStyle("-fx-font-size: 16;");
        btnEdit.setOnAction(e -> openForm(p));

        Button btnDelete = new Button("🗑");
        btnDelete.getStyleClass().add("btn-delete-ghost");
        btnDelete.setStyle("-fx-font-size: 16;");
        btnDelete.setOnAction(e -> {
            try {
                service.supprimer(p.getId());
                refreshAll();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        actionBox.getChildren().addAll(btnCalendar, btnEdit, btnDelete);

        item.getChildren().addAll(colDate, colTime, colStatus, spacer, actionBox);
        vboxPlanningList.getChildren().add(item);
    }

    private void refreshStatistics() {
        try {
            double rate = activiteService.getCompletionRate(currentUserId);
            lblEfficiency.setText(String.format("%.1f%%", rate * 100));
            efficiencyProgress.setPrefWidth(120 * rate);

            Map<String, Integer> minutesData = service.getWeeklyWorkedMinutes(currentUserId);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            minutesData.forEach((week, mins) -> series.getData().add(new XYChart.Data<>(week, mins)));
            barChartWeeklyWork.getData().setAll(series);

            Map<String, Integer> priorityData = activiteService.getPriorityDistribution(currentUserId);
            pieChartPriority.getData().clear();
            priorityData.forEach((label, count) -> {
                PieChart.Data data = new PieChart.Data(label, count);
                pieChartPriority.getData().add(data);
            });
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleAdd() {
        openForm(null);
    }

    @FXML
    private void handleSwitchToCalendar() {
        if (DashboardController.getInstance() != null) {
            DashboardController.getInstance().setView("planning_dashboard.fxml");
        }
    }

    private void openForm(Planning p) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/planning_form.fxml"));
            javafx.scene.Parent root = loader.load();
            PlanningFormController ctrl = loader.getController();
            ctrl.setUserId(currentUserId);
            if (p != null) ctrl.setPlanning(p); // Pass the planning for editing
            ctrl.setOnSave(date -> javafx.application.Platform.runLater(this::refreshAll));
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }
}

