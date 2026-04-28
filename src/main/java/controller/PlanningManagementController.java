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
import model.Activite;
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
        VBox card = new VBox(15);
        card.getStyleClass().add("finance-card");
        
        // Header Row: Date Title + Plafond-style Badge + Action Buttons
        HBox header = new HBox(15);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label lblTitle = new Label(p.getDate().toLocalDate().format(DateTimeFormatter.ofPattern("MM-yyyy")));
        lblTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        
        Region spacer1 = new Region(); HBox.setHgrow(spacer1, Priority.ALWAYS);
        
        Label lblBadge = new Label("Dispo: " + (p.isDisponibilite() ? "OUI" : "NON"));
        lblBadge.getStyleClass().add("badge-purple");
        
        Button btnEdit = new Button("Modifier");
        btnEdit.getStyleClass().add("btn-modifier");
        btnEdit.setOnAction(e -> openForm(p));
        
        Button btnDelete = new Button("Supprimer");
        btnDelete.getStyleClass().add("btn-supprimer");
        btnDelete.setOnAction(e -> {
            try {
                service.supprimer(p.getId());
                refreshAll();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        
        header.getChildren().addAll(lblTitle, spacer1, lblBadge, btnEdit, btnDelete);
        
        // Info Boxes Row: START, END, DAY
        HBox infoBoxes = new HBox(15);
        infoBoxes.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        VBox boxStart = createInfoBox("DEBUT", p.getHeureDebutJournee().toString().substring(0, 5));
        VBox boxEnd = createInfoBox("FIN", p.getHeureFinJournee().toString().substring(0, 5));
        VBox boxDay = createInfoBox("JOUR", p.getDate().toLocalDate().format(DateTimeFormatter.ofPattern("EEEE", Locale.FRENCH)).toUpperCase());
        
        infoBoxes.getChildren().addAll(boxStart, boxEnd, boxDay);
        
        // Activities Section
        VBox activitiesList = new VBox(5);
        Label lblSubtitle = new Label("Activités rattachées");
        lblSubtitle.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-padding: 10 0 5 0;");
        activitiesList.getChildren().add(lblSubtitle);
        
        try {
            List<Activite> activities = activiteService.recupererParPlanning(p.getId());
            if (activities.isEmpty()) {
                Label lblNone = new Label("Aucune activité pour ce planning.");
                lblNone.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8; -fx-italic: true;");
                activitiesList.getChildren().add(lblNone);
            } else {
                for (Activite a : activities) {
                    HBox actItem = new HBox(10);
                    actItem.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    actItem.setStyle("-fx-padding: 5 0; -fx-border-color: transparent transparent #f1f5f9 transparent;");
                    
                    Label dot = new Label("•");
                    dot.setStyle("-fx-text-fill: " + (a.getCouleur() != null ? a.getCouleur() : "#8b5cf6") + ";");
                    
                    Label actTitle = new Label(a.getTitre());
                    actTitle.setStyle("-fx-font-size: 12; -fx-text-fill: #475569;");
                    
                    Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
                    
                    Label actTime = new Label(a.getHeureDebutEstimee().toString().substring(0, 5) + " DT"); // "DT" to mimic screenshot style
                    actTime.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #6366f1;");
                    
                    actItem.getChildren().addAll(dot, actTitle, s, actTime);
                    activitiesList.getChildren().add(actItem);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        
        card.getChildren().addAll(header, infoBoxes, activitiesList);
        vboxPlanningList.getChildren().add(card);
    }

    private VBox createInfoBox(String label, String value) {
        VBox box = new VBox(2);
        box.getStyleClass().add("value-box");
        box.setMinWidth(100);
        
        Label lbl = new Label(label);
        lbl.getStyleClass().add("value-label");
        
        Label val = new Label(value);
        val.getStyleClass().add("value-amount");
        
        box.getChildren().addAll(lbl, val);
        return box;
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

