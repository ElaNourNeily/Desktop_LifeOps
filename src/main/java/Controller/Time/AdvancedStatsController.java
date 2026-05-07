package Controller.Time;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import service.Time.ActiviteService;

import java.sql.SQLException;
import java.util.Map;

public class AdvancedStatsController {

    @FXML private PieChart pieCategory;
    @FXML private Label lblAccuracy;
    @FXML private BarChart<String, Integer> barWeeklyTrend;

    private final ActiviteService activiteService = new ActiviteService();
    private int currentUserId = 1;

    @FXML
    public void initialize() {
        loadFirstUserId();
        loadData();
    }

    private void loadFirstUserId() {
        try {
            java.sql.ResultSet rs = utils.MyDatabase.getInstance().getConnection()
                    .createStatement().executeQuery("SELECT id FROM utilisateur LIMIT 1");
            if (rs.next()) {
                currentUserId = rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        try {
            // 1. Time by Category
            Map<String, Integer> catData = activiteService.getTimeByCategory(currentUserId);
            for (Map.Entry<String, Integer> entry : catData.entrySet()) {
                pieCategory.getData().add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + "m)", entry.getValue()));
            }

            // 2. Scheduling Accuracy
            double accuracy = activiteService.getSchedulingAccuracy(currentUserId);
            lblAccuracy.setText(String.format("%.0f%%", accuracy));
            
            // Premium coloring based on score
            if (accuracy < 50) lblAccuracy.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 72; -fx-font-weight: bold;");
            else if (accuracy < 80) lblAccuracy.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 72; -fx-font-weight: bold;");
            else lblAccuracy.setStyle("-fx-text-fill: #10b981; -fx-font-size: 72; -fx-font-weight: bold;");

            // 3. Weekly Trend
            Map<String, Integer> trend = activiteService.getWeeklyProductivityTrend(currentUserId);
            XYChart.Series<String, Integer> series = new XYChart.Series<>();
            series.setName("Activités terminées");
            for (Map.Entry<String, Integer> entry : trend.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            barWeeklyTrend.getData().add(series);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        if (DashboardController.getInstance() != null) {
            DashboardController.getInstance().setView("planning_dashboard.fxml");
        }
    }
}
