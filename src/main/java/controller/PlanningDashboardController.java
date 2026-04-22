package controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import model.Activite;
import model.Planning;
import service.ActiviteService;
import service.PlanningService;

import java.sql.Date;
import java.sql.Time;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanningDashboardController {

    @FXML private Label lblCurrentDateTitle;
    @FXML private Label lblCompletionValue;
    @FXML private AnchorPane progressFill;
    @FXML private HBox gridHeader;
    @FXML private GridPane calendarGrid;

    @FXML private Label lblTotalActivities;
    @FXML private Label lblAvgActivities;
    @FXML private Label lblFocusTime;
    @FXML private Label lblDispoRate;

    // View Toggle Buttons
    @FXML private javafx.scene.control.Button btnViewJour;
    @FXML private javafx.scene.control.Button btnViewSemaine;

    // Filter Buttons
    @FXML private javafx.scene.control.Button btnFilterHaute;
    @FXML private javafx.scene.control.Button btnFilterMoyenne;
    @FXML private javafx.scene.control.Button btnFilterBasse;
    @FXML private javafx.scene.control.Button btnFilterEnCours;
    @FXML private javafx.scene.control.Button btnFilterAttente;
    @FXML private javafx.scene.control.Button btnFilterReset;

    // Search
    @FXML private javafx.scene.control.TextField txtSearch;
    private String searchKeyword = "";

    // Active filter: "ALL", "HAUTE", "MOYENNE", "BASSE", "EN_COURS", "ATTENTE"
    private String currentFilter = "ALL";

    private final PlanningService planningService = new PlanningService();
    private final ActiviteService activiteService = new ActiviteService();
    
    // Navigation Jump
    private static LocalDate jumpToDate;
    public static void setJumpToDate(LocalDate date) { jumpToDate = date; }

    private int currentUserId = -1; 
    private LocalDate currentMonday;
    
    // View Mode: "WEEK" or "DAY"
    private String viewMode = "WEEK";
    private LocalDate currentDayView;

    @FXML
    public void initialize() {
        loadFirstUserId();
        if (jumpToDate != null) {
            currentMonday = jumpToDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            jumpToDate = null;
        } else {
            currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        currentDayView = LocalDate.now();
        setupGrid();
        refresh();

        // Wire real-time search
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                searchKeyword = newVal == null ? "" : newVal.trim().toLowerCase();
                refreshGrid();
            });
        }
    }

    private void loadFirstUserId() {
        try {
            java.sql.ResultSet rs = utils.MyDatabase.getInstance().getConnection()
                    .createStatement().executeQuery("SELECT id FROM utilisateur LIMIT 1");
            if (rs.next()) {
                currentUserId = rs.getInt("id");
                System.out.println("[DASHBOARD] Detected User ID: " + currentUserId);
            } else {
                System.err.println("[DASHBOARD] WARNING: No users found in 'utilisateur' table. Foreign key errors expected.");
                currentUserId = 1; 
            }
        } catch (java.sql.SQLException e) {
            System.err.println("[DASHBOARD] DB Connection error: " + e.getMessage());
            currentUserId = 1;
        }
    }

    private void setupGrid() {
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();
        calendarGrid.getChildren().clear();

        // Time column
        ColumnConstraints timeCol = new ColumnConstraints(60);
        calendarGrid.getColumnConstraints().add(timeCol);

        int numCols = viewMode.equals("DAY") ? 1 : 7;
        for (int i = 0; i < numCols; i++) {
            ColumnConstraints dayCol = new ColumnConstraints();
            dayCol.setHgrow(Priority.ALWAYS);
            dayCol.setMinWidth(viewMode.equals("DAY") ? 300 : 120);
            calendarGrid.getColumnConstraints().add(dayCol);
        }

        // 24 Hour rows
        for (int i = 0; i < 24; i++) {
            RowConstraints row = new RowConstraints(60);
            calendarGrid.getRowConstraints().add(row);
            
            Label lblHour = new Label(String.format("%02d:00", i));
            lblHour.getStyleClass().add("page-subtitle");
            lblHour.setPadding(new Insets(5, 0, 0, 10));
            calendarGrid.add(lblHour, 0, i);

            for (int col = 1; col <= numCols; col++) {
                javafx.scene.layout.Pane cellPane = new javafx.scene.layout.Pane();
                cellPane.getStyleClass().add("calendar-cell");
                GridPane.setHgrow(cellPane, Priority.ALWAYS);
                GridPane.setVgrow(cellPane, Priority.ALWAYS);
                cellPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                int finalRow = i;
                int finalCol = col;
                LocalDate cellDate = viewMode.equals("DAY") ? currentDayView : currentMonday.plusDays(finalCol - 1);
                cellPane.setOnMouseClicked(e -> {
                    int pId = findOrCreatePlanningId(cellDate);
                    if (pId != -1) {
                        openForm("activite_form.fxml", "Nouvelle Activité", pId, finalRow, cellDate);
                    }
                });
                calendarGrid.add(cellPane, col, i);
            }
        }
    }

    private void refresh() {
        if (viewMode.equals("DAY")) {
            // Day view header: "Dimanche 19 avril 2026"
            DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", java.util.Locale.FRENCH);
            String capitalised = currentDayView.format(dayFmt);
            capitalised = capitalised.substring(0, 1).toUpperCase() + capitalised.substring(1);
            lblCurrentDateTitle.setText(capitalised);
        } else {
            DateTimeFormatter dayMonthFmt = DateTimeFormatter.ofPattern("d MMM", java.util.Locale.FRENCH);
            DateTimeFormatter fullFmt = DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.FRENCH);
            String range = currentMonday.format(dayMonthFmt) + " - " + currentMonday.plusDays(6).format(fullFmt);
            lblCurrentDateTitle.setText(range);
        }

        refreshHeader();
        refreshGrid();
        refreshCharts();
    }

    private void refreshHeader() {
        gridHeader.getChildren().clear();
        
        Label lblTrack = new Label("TRACK");
        lblTrack.setPrefWidth(60);
        lblTrack.setAlignment(javafx.geometry.Pos.CENTER);
        lblTrack.setStyle("-fx-text-fill: #71717a; -fx-font-size: 10; -fx-font-weight: bold;");
        gridHeader.getChildren().add(lblTrack);

        if (viewMode.equals("DAY")) {
            // Single day column header
            VBox dayHeader = new VBox();
            dayHeader.setAlignment(javafx.geometry.Pos.CENTER);
            HBox.setHgrow(dayHeader, Priority.ALWAYS);
            
            Label lblDayName = new Label(currentDayView.format(DateTimeFormatter.ofPattern("EEE", java.util.Locale.FRENCH)).toUpperCase());
            lblDayName.setStyle("-fx-font-size: 10; -fx-text-fill: rgba(113,113,122,0.7); -fx-font-weight: bold;");
            
            Label lblDayNum = new Label(currentDayView.format(DateTimeFormatter.ofPattern("dd")));
            if (currentDayView.equals(LocalDate.now())) {
                lblDayNum.setStyle("-fx-background-color: #8b5cf6; -fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-alignment: center; -fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(139, 92, 246, 0.6), 15, 0, 0, 0);");
            } else {
                lblDayNum.setStyle("-fx-font-size: 18; -fx-text-fill: white; -fx-font-weight: bold;");
            }
            dayHeader.getChildren().addAll(lblDayName, lblDayNum);
            gridHeader.getChildren().add(dayHeader);
        } else {
            // Week view: 7 day headers, clicking the number switches to day view
            LocalDate date = currentMonday;
            for (int i = 0; i < 7; i++) {
                VBox dayHeader = new VBox();
                dayHeader.setAlignment(javafx.geometry.Pos.CENTER);
                HBox.setHgrow(dayHeader, Priority.ALWAYS);
                dayHeader.setStyle("-fx-cursor: hand;");
                
                Label lblDayName = new Label(date.format(DateTimeFormatter.ofPattern("EEE", java.util.Locale.FRENCH)).toUpperCase());
                lblDayName.setStyle("-fx-font-size: 10; -fx-text-fill: rgba(113,113,122,0.7); -fx-font-weight: bold;");
                
                Label lblDayNum = new Label(date.format(DateTimeFormatter.ofPattern("dd")));
                
                if (date.equals(LocalDate.now())) {
                    lblDayNum.setStyle("-fx-background-color: #8b5cf6; -fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-alignment: center; -fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(139, 92, 246, 0.6), 15, 0, 0, 0);");
                } else {
                    lblDayNum.setStyle("-fx-font-size: 18; -fx-text-fill: white; -fx-font-weight: bold;");
                }

                // CLICK → Switch to Day View for this date
                final LocalDate clickedDate = date;
                dayHeader.setOnMouseClicked(e -> switchToDayView(clickedDate));
                lblDayNum.setOnMouseClicked(e -> switchToDayView(clickedDate));

                dayHeader.getChildren().addAll(lblDayName, lblDayNum);
                gridHeader.getChildren().add(dayHeader);
                date = date.plusDays(1);
            }
        }
    }

    private void switchToDayView(LocalDate date) {
        viewMode = "DAY";
        currentDayView = date;
        setupGrid();
        refresh();
        updateToggleButtons();
    }

    private void switchToWeekView() {
        viewMode = "WEEK";
        currentMonday = currentDayView.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        setupGrid();
        refresh();
        updateToggleButtons();
    }

    private void updateToggleButtons() {
        if (btnViewJour == null || btnViewSemaine == null) return;
        if (viewMode.equals("DAY")) {
            btnViewJour.getStyleClass().remove("toggle-btn-active");
            btnViewJour.getStyleClass().add("toggle-btn-active");
            btnViewSemaine.getStyleClass().remove("toggle-btn-active");
        } else {
            btnViewSemaine.getStyleClass().remove("toggle-btn-active");
            btnViewSemaine.getStyleClass().add("toggle-btn-active");
            btnViewJour.getStyleClass().remove("toggle-btn-active");
        }
    }

    private void refreshGrid() {
        // Clear only activity cards, not interactive background cells
        calendarGrid.getChildren().removeIf(node -> 
            GridPane.getColumnIndex(node) != null && 
            GridPane.getColumnIndex(node) > 0 && 
            !node.getStyleClass().contains("calendar-cell")
        );

        try {
            if (viewMode.equals("DAY")) {
                // Show activities for only this day
                List<Planning> plannings = planningService.recupererParSemaine(currentUserId, Date.valueOf(currentDayView));
                for (Planning p : plannings) {
                    if (p.getDate().toLocalDate().equals(currentDayView)) {
                        List<Activite> activities = activiteService.recupererParPlanning(p.getId());
                        for (Activite a : activities) {
                            if (passesFilter(a)) addActivityToGrid(a, 1);
                        }
                    }
                }
            } else {
                List<Planning> plannings = planningService.recupererParSemaine(currentUserId, Date.valueOf(currentMonday));
                for (Planning p : plannings) {
                    int dayIndex = p.getDate().toLocalDate().getDayOfWeek().getValue();
                    List<Activite> activities = activiteService.recupererParPlanning(p.getId());
                    for (Activite a : activities) {
                        if (passesFilter(a)) addActivityToGrid(a, dayIndex);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void refreshCharts() {
        try {
            List<Planning> weeklyPlannings = planningService.recupererParSemaine(currentUserId, Date.valueOf(currentMonday));
            int totalActivities = 0;
            long totalMinutes = 0;

            for (Planning p : weeklyPlannings) {
                List<Activite> activities = activiteService.recupererParPlanning(p.getId());
                totalActivities += activities.size();
                for (Activite a : activities) {
                    if (a.getHeureDebutEstimee() != null && a.getHeureFinEstimee() != null) {
                        totalMinutes += (a.getHeureFinEstimee().getTime() - a.getHeureDebutEstimee().getTime()) / (60 * 1000);
                    }
                }
            }

            lblTotalActivities.setText(String.valueOf(totalActivities));
            lblAvgActivities.setText(String.format("%.1f", totalActivities / 7.0));

            int focusHours = (int) (totalMinutes / 60);
            lblFocusTime.setText(focusHours + "h");

            double rate = activiteService.getCompletionRate(currentUserId);
            lblCompletionValue.setText((int)(rate * 100) + "%");
            progressFill.setPrefWidth(280 * rate);

            double dispoRate = Math.max(0, 1.0 - (totalMinutes / 5040.0));
            lblDispoRate.setText((int)(dispoRate * 100) + "%");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePrev() {
        if (viewMode.equals("DAY")) {
            currentDayView = currentDayView.minusDays(1);
        } else {
            currentMonday = currentMonday.minusWeeks(1);
        }
        setupGrid();
        refresh();
    }

    @FXML
    private void handleNext() {
        if (viewMode.equals("DAY")) {
            currentDayView = currentDayView.plusDays(1);
        } else {
            currentMonday = currentMonday.plusWeeks(1);
        }
        setupGrid();
        refresh();
    }

    @FXML
    private void handleToday() {
        currentDayView = LocalDate.now();
        currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        setupGrid();
        refresh();
    }

    // ─── Filter Handlers ─────────────────────────────────────────────────────
    @FXML private void handleFilterHaute()   { currentFilter = "HAUTE";    refreshGrid(); updateFilterStyle(); }
    @FXML private void handleFilterMoyenne() { currentFilter = "MOYENNE";  refreshGrid(); updateFilterStyle(); }
    @FXML private void handleFilterBasse()   { currentFilter = "BASSE";    refreshGrid(); updateFilterStyle(); }
    @FXML private void handleFilterEnCours() { currentFilter = "EN_COURS"; refreshGrid(); updateFilterStyle(); }
    @FXML private void handleFilterAttente() { currentFilter = "ATTENTE";  refreshGrid(); updateFilterStyle(); }
    @FXML private void handleFilterReset() {
        currentFilter = "ALL";
        searchKeyword = "";
        if (txtSearch != null) txtSearch.clear();
        refreshGrid();
        updateFilterStyle();
    }

    private void updateFilterStyle() {
        if (btnFilterHaute == null) return;
        String activeBase = "-fx-border-width: 2; -fx-border-radius: 20; ";
        String resetStyle = "-fx-border-color: transparent; ";
        // Reset all borders
        btnFilterHaute.setStyle(btnFilterHaute.getStyle().replaceAll("-fx-border.*?;", "") + resetStyle);
        btnFilterMoyenne.setStyle(btnFilterMoyenne.getStyle().replaceAll("-fx-border.*?;", "") + resetStyle);
        btnFilterBasse.setStyle(btnFilterBasse.getStyle().replaceAll("-fx-border.*?;", "") + resetStyle);
        btnFilterEnCours.setStyle(btnFilterEnCours.getStyle().replaceAll("-fx-border.*?;", "") + resetStyle);
        btnFilterAttente.setStyle(btnFilterAttente.getStyle().replaceAll("-fx-border.*?;", "") + resetStyle);
        // Highlight active
        switch (currentFilter) {
            case "HAUTE"    -> btnFilterHaute.setStyle(btnFilterHaute.getStyle() + activeBase + "-fx-border-color: #f43f5e;");
            case "MOYENNE"  -> btnFilterMoyenne.setStyle(btnFilterMoyenne.getStyle() + activeBase + "-fx-border-color: #f59e0b;");
            case "BASSE"    -> btnFilterBasse.setStyle(btnFilterBasse.getStyle() + activeBase + "-fx-border-color: #10b981;");
            case "EN_COURS" -> btnFilterEnCours.setStyle(btnFilterEnCours.getStyle() + activeBase + "-fx-border-color: #8b5cf6;");
            case "ATTENTE"  -> btnFilterAttente.setStyle(btnFilterAttente.getStyle() + activeBase + "-fx-border-color: #71717a;");
        }
    }

    private boolean passesFilter(Activite a) {
        // Search keyword check
        if (!searchKeyword.isEmpty()) {
            String title = a.getTitre() != null ? a.getTitre().toLowerCase() : "";
            String cat   = a.getCategorie() != null ? a.getCategorie().toLowerCase() : "";
            if (!title.contains(searchKeyword) && !cat.contains(searchKeyword)) return false;
        }
        // Priority / State filter
        return switch (currentFilter) {
            case "HAUTE"    -> a.getPriorite() >= 4;
            case "MOYENNE"  -> a.getPriorite() == 2 || a.getPriorite() == 3;
            case "BASSE"    -> a.getPriorite() <= 1;
            case "EN_COURS" -> "en_cours".equalsIgnoreCase(a.getEtat());
            case "ATTENTE"  -> "en_attente".equalsIgnoreCase(a.getEtat());
            default         -> true;
        };
    }

    @FXML
    private void handleViewJour() {
        currentDayView = viewMode.equals("WEEK") ? LocalDate.now() : currentDayView;
        switchToDayView(currentDayView);
    }

    @FXML
    private void handleViewSemaine() {
        switchToWeekView();
    }

    @FXML
    private void handleNewPlanning() {
        openForm("planning_form.fxml", "Nouveau Planning", null, -1, null);
    }

    private void openForm(String fxml, String title, Object data, int hour, LocalDate dayDate) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/" + fxml));
            javafx.scene.Parent root = loader.load();
            
            Object controller = loader.getController();
            if (controller instanceof ActiviteFormController) {
                ActiviteFormController c = (ActiviteFormController) controller;
                c.setOnSave(() -> refresh());
                if (data instanceof Activite) {
                    c.setActivite((Activite) data);
                } else if (data instanceof Integer) {
                    c.setPlanningId((Integer) data);
                    c.setInitialTime(hour);
                }
                
                if (dayDate != null) {
                    c.setDateSubtitle(java.sql.Date.valueOf(dayDate));
                }
            } else if (controller instanceof PlanningFormController) {
                PlanningFormController c = (PlanningFormController) controller;
                c.setUserId(currentUserId);
                c.setOnSave(d -> javafx.application.Platform.runLater(this::refresh));
            }

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setTitle(title);
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int findOrCreatePlanningId(LocalDate date) {
        try {
            List<Planning> plannings = planningService.recupererParSemaine(currentUserId, Date.valueOf(date));
            for (Planning p : plannings) {
                if (p.getDate().toLocalDate().equals(date)) return p.getId();
            }
            
            Planning p = new Planning(Date.valueOf(date), true, Time.valueOf("08:00:00"), Time.valueOf("20:00:00"), currentUserId);
            planningService.ajouter(p);
            System.out.println("[DASHBOARD] Created placeholder planning for " + date);
            return p.getId();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void addActivityToGrid(Activite a, int colIndex) {
        if (a.getHeureDebutEstimee() == null || a.getHeureFinEstimee() == null) return;
        
        int startHour = a.getHeureDebutEstimee().toLocalTime().getHour();
        int startMin = a.getHeureDebutEstimee().toLocalTime().getMinute();
        long durationMin = (a.getHeureFinEstimee().getTime() - a.getHeureDebutEstimee().getTime()) / (60 * 1000);
        
        VBox card = new VBox(4);
        card.getStyleClass().add("activity-block");
        String color = a.getCouleur() != null ? a.getCouleur() : "#8b5cf6";
        card.setStyle("-fx-background-color: " + color + "; -fx-opacity: 0.9; -fx-background-radius: 12; -fx-padding: 8 10;");
        
        // Top row: title + time
        HBox topRow = new HBox();
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblTitle = new Label(a.getTitre() != null ? a.getTitre().toUpperCase() : "");
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblTime = new Label(a.getHeureDebutEstimee().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        lblTime.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 10; -fx-font-weight: bold;");
        topRow.getChildren().addAll(lblTitle, spacer, lblTime);

        // Subtitle
        Label lblSub = new Label((a.getCategorie() != null ? a.getCategorie() : "Activité") + " + 1");
        lblSub.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 9;");

        card.getChildren().addAll(topRow, lblSub);
        
        GridPane.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        
        card.setOnMouseClicked(e -> {
            LocalDate actDate = viewMode.equals("DAY") ? currentDayView : currentMonday.plusDays(colIndex - 1);
            openForm("activite_form.fxml", "Modifier Activité", a, -1, actDate);
        });

        double topOffset = (startMin / 60.0) * 60;
        double height = Math.max(45, (durationMin / 60.0) * 60);
        
        int rowSpan = (int) Math.ceil((startMin + durationMin) / 60.0);
        if (rowSpan < 1) rowSpan = 1;

        card.setTranslateY(topOffset);
        card.setPrefHeight(height);
        card.setMinHeight(height);
        card.setMaxHeight(height);
        
        calendarGrid.add(card, colIndex, startHour, 1, rowSpan);
    }

    @FXML
    private void handleSeeAll() {
        if (DashboardController.getInstance() != null) {
            DashboardController.getInstance().setView("planning_management.fxml");
        }
    }
}
