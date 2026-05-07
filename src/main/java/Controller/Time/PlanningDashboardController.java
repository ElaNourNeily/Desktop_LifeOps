package Controller.Time;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import model.Time.Activite;
import model.Time.Planning;
import service.Time.ActiviteService;
import service.Time.PlanningService;

import java.sql.Date;
import java.sql.Time;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import javafx.stage.FileChooser;

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
    @FXML private javafx.scene.control.Button btnFilterTermine;
    @FXML private javafx.scene.control.Button btnFilterAttente;
    @FXML private javafx.scene.control.Button btnFilterReset;

    // Search
    @FXML private javafx.scene.control.TextField txtSearch;
    private String searchKeyword = "";

    // Active filter: "ALL", "HAUTE", "MOYENNE", "BASSE", "EN_COURS", "ATTENTE"
    private String currentFilter = "ALL";

    @FXML private javafx.scene.control.Button btnIA;
    @FXML private Label lblWeather;

    private final PlanningService planningService = new PlanningService();
    private final ActiviteService activiteService = new ActiviteService();
    private final service.Time.AIService aiService = new service.Time.AIService("AIzaSyD8dXGwrl5TjTOAWw6uaq0s2x_5VrX0WmI");
    private final service.Time.external.WeatherService weatherService = new service.Time.external.WeatherService();
    
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
        refreshWeather();

        // Wire real-time search
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                searchKeyword = newVal == null ? "" : newVal.trim().toLowerCase();
                refreshGrid();
            });
        }
    }

    private void refreshWeather() {
        double lat = 36.8065;
        double lon = 10.1815;
        
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
            if (rs.next()) {
                currentUserId = rs.getInt("id");
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
        ColumnConstraints timeCol = new ColumnConstraints();
        timeCol.setPrefWidth(60);
        timeCol.setMinWidth(60);
        calendarGrid.getColumnConstraints().add(timeCol);

        int numCols = viewMode.equals("DAY") ? 1 : 7;
        double dayPercent = 100.0 / (numCols); // Grid will handle the fixed timeCol automatically if we don't set it to percent
        
        for (int i = 0; i < numCols; i++) {
            ColumnConstraints dayCol = new ColumnConstraints();
            dayCol.setHgrow(Priority.ALWAYS);
            if (!viewMode.equals("DAY")) {
                dayCol.setPercentWidth(90.0 / 7.0); // Roughly 12.8% each, leaving space for time column
            }
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

                Label lblFullDate = new Label(date.format(DateTimeFormatter.ofPattern("MM/yyyy")));
                lblFullDate.setStyle("-fx-font-size: 9; -fx-text-fill: rgba(113,113,122,0.5); -fx-font-weight: bold;");

                // CLICK → Switch to Day View for this date
                final LocalDate clickedDate = date;
                dayHeader.setOnMouseClicked(e -> switchToDayView(clickedDate));
                lblDayNum.setOnMouseClicked(e -> switchToDayView(clickedDate));

                dayHeader.getChildren().addAll(lblDayName, lblDayNum, lblFullDate);
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
                List<Planning> plannings = planningService.recupererParSemaine(currentUserId, Date.valueOf(currentDayView));
                List<Activite> dayActs = new ArrayList<>();
                for (Planning p : plannings) {
                    if (p.getDate().toLocalDate().equals(currentDayView)) {
                        dayActs.addAll(activiteService.recupererParPlanning(p.getId()));
                    }
                }
                layoutDayActivities(dayActs, 1, currentDayView);
            } else {
                List<Planning> plannings = planningService.recupererParSemaine(currentUserId, Date.valueOf(currentMonday));
                for (Planning p : plannings) {
                    int dayIndex = p.getDate().toLocalDate().getDayOfWeek().getValue();
                    List<Activite> dayActs = activiteService.recupererParPlanning(p.getId());
                    layoutDayActivities(dayActs, dayIndex, p.getDate().toLocalDate());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void layoutDayActivities(List<Activite> activities, int colIndex, LocalDate date) {
        // 1. Filter
        List<Activite> filtered = new ArrayList<>();
        for (Activite a : activities) {
            if (passesFilter(a, date)) filtered.add(a);
        }
        
        // 2. Sort by start time
        filtered.sort((a, b) -> a.getHeureDebutEstimee().compareTo(b.getHeureDebutEstimee()));

        // 3. Find overlapping groups
        List<List<Activite>> groups = new ArrayList<>();
        for (Activite a : filtered) {
            boolean added = false;
            for (List<Activite> group : groups) {
                if (overlapsAny(a, group)) {
                    group.add(a);
                    added = true;
                    break;
                }
            }
            if (!added) {
                List<Activite> newGroup = new ArrayList<>();
                newGroup.add(a);
                groups.add(newGroup);
            }
        }

        // 4. For each group, calculate side-by-side positions
        for (List<Activite> group : groups) {
            for (int i = 0; i < group.size(); i++) {
                addActivityToGrid(group.get(i), colIndex, i, group.size());
            }
        }
    }

    private boolean overlapsAny(Activite a, List<Activite> group) {
        for (Activite other : group) {
            if (a.getHeureDebutEstimee().before(other.getHeureFinEstimee()) && 
                other.getHeureDebutEstimee().before(a.getHeureFinEstimee())) {
                return true;
            }
        }
        return false;
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
    @FXML private void handleFilterTermine() { currentFilter = "TERMINE";  refreshGrid(); updateFilterStyle(); }
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
        if (btnFilterTermine != null) btnFilterTermine.setStyle(btnFilterTermine.getStyle().replaceAll("-fx-border.*?;", "") + resetStyle);
        btnFilterAttente.setStyle(btnFilterAttente.getStyle().replaceAll("-fx-border.*?;", "") + resetStyle);
        // Highlight active
        switch (currentFilter) {
            case "HAUTE"    -> btnFilterHaute.setStyle(btnFilterHaute.getStyle() + activeBase + "-fx-border-color: #f43f5e;");
            case "MOYENNE"  -> btnFilterMoyenne.setStyle(btnFilterMoyenne.getStyle() + activeBase + "-fx-border-color: #f59e0b;");
            case "BASSE"    -> btnFilterBasse.setStyle(btnFilterBasse.getStyle() + activeBase + "-fx-border-color: #10b981;");
            case "EN_COURS" -> btnFilterEnCours.setStyle(btnFilterEnCours.getStyle() + activeBase + "-fx-border-color: #8b5cf6;");
            case "TERMINE"  -> { if (btnFilterTermine != null) btnFilterTermine.setStyle(btnFilterTermine.getStyle() + activeBase + "-fx-border-color: #3b82f6;"); }
            case "ATTENTE"  -> btnFilterAttente.setStyle(btnFilterAttente.getStyle() + activeBase + "-fx-border-color: #71717a;");
        }
    }

    private boolean passesFilter(Activite a, LocalDate date) {
        // Search keyword check
        if (!searchKeyword.isEmpty()) {
            String title = a.getTitre() != null ? a.getTitre().toLowerCase() : "";
            String cat   = a.getCategorie() != null ? a.getCategorie().toLowerCase() : "";
            if (!title.contains(searchKeyword) && !cat.contains(searchKeyword)) return false;
        }

        // Get the dynamic status as displayed in the UI
        String dynamicStatus = a.getStatutDynamique(java.sql.Date.valueOf(date));

        // Priority / State filter
        return switch (currentFilter) {
            case "HAUTE"    -> a.getPriorite() >= 3; // Adjusted for 1-3 scale
            case "MOYENNE"  -> a.getPriorite() == 2;
            case "BASSE"    -> a.getPriorite() <= 1;
            case "EN_COURS" -> "En cours".equals(dynamicStatus);
            case "TERMINE"  -> "Terminé".equals(dynamicStatus);
            case "ATTENTE"  -> "En attente".equals(dynamicStatus);
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
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/Time/" + fxml));
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
            // 1. Try to find existing planning for this date
            Planning existing = planningService.recupererParDate(date, currentUserId);
            if (existing != null) return existing.getId();
            
            // 2. If not found, create it
            Planning p = new Planning(Date.valueOf(date), true, Time.valueOf("08:00:00"), Time.valueOf("20:00:00"), currentUserId);
            planningService.ajouter(p);
            return p.getId();
        } catch (SQLException e) {
            // 3. Fallback: If insertion failed (e.g. race condition/duplicate), try one last time to fetch it
            try {
                Planning p = planningService.recupererParDate(date, currentUserId);
                if (p != null) return p.getId();
            } catch (SQLException e2) {
                e2.printStackTrace();
            }
            System.err.println("[DASHBOARD] Critical error finding/creating planning: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    private void addActivityToGrid(Activite a, int colIndex, int indexInOverlap, int totalInOverlap) {
        if (a.getHeureDebutEstimee() == null || a.getHeureFinEstimee() == null) return;
        
        int startHour = a.getHeureDebutEstimee().toLocalTime().getHour();
        int startMin = a.getHeureDebutEstimee().toLocalTime().getMinute();
        long durationMin = (a.getHeureFinEstimee().getTime() - a.getHeureDebutEstimee().getTime()) / (60 * 1000);
        
        VBox card = new VBox(4);
        card.getStyleClass().add("activity-block");
        if (a.isSuggestedByAi()) card.getStyleClass().add("activity-block-ai");
        
        String color = a.getCouleur() != null ? a.getCouleur() : "#8b5cf6";
        card.setStyle("-fx-background-color: " + color + "; -fx-opacity: 0.9; -fx-background-radius: 12; -fx-padding: 8 10;");
        
        // Top row: title + time
        HBox topRow = new HBox();
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblTitle = new Label(a.getTitre() != null ? a.getTitre().toUpperCase() : "");
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        
        if (a.isSuggestedByAi()) {
            Label lblIA = new Label("🤖 IA");
            lblIA.getStyleClass().add("badge-ai-suggestion");
            topRow.getChildren().add(lblIA);
            topRow.getChildren().add(new Label(" ")); // small gap
        }

        Label lblTime = new Label(a.getHeureDebutEstimee().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        lblTime.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 10; -fx-font-weight: bold;");
        topRow.getChildren().addAll(lblTitle, spacer, lblTime);

        // Subtitle
        Label lblSub = new Label((a.getCategorie() != null ? a.getCategorie() : "Activité") + " + 1");
        lblSub.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 9;");

        // Status logic
        LocalDate cardDate = viewMode.equals("DAY") ? currentDayView : currentMonday.plusDays(colIndex - 1);
        String status = a.getStatutDynamique(java.sql.Date.valueOf(cardDate));
        
        Label lblStatus = new Label(status);
        String statusColor = "#71717a"; // Default gray
        if ("En cours".equals(status)) statusColor = "#10b981"; // Green
        else if ("Terminé".equals(status)) {
            statusColor = "#3b82f6"; // Blue
            card.setOpacity(0.6); // Dim completed tasks
        }
        lblStatus.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 9; -fx-font-weight: bold; -fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 4; -fx-padding: 1 4;");

        card.getChildren().addAll(topRow, lblSub, lblStatus);
        
        // Context Menu
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem doneItem = new javafx.scene.control.MenuItem("✅ Marquer comme Terminé");
        doneItem.setOnAction(e -> {
            try {
                a.setEtat("terminé");
                a.setHeureFinReelle(Time.valueOf(LocalTime.now()));
                if (a.getHeureDebutReelle() == null) a.setHeureDebutReelle(a.getHeureDebutEstimee());
                activiteService.modifier(a);
                refresh();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        javafx.scene.control.MenuItem focusItem = new javafx.scene.control.MenuItem("🚀 Lancer Focus (Pomodoro)");
        focusItem.setOnAction(e -> openFocusSession(a));
        javafx.scene.control.MenuItem editItem = new javafx.scene.control.MenuItem("🔧 Modifier");
        editItem.setOnAction(e -> {
            LocalDate actDate = viewMode.equals("DAY") ? currentDayView : currentMonday.plusDays(colIndex - 1);
            openForm("activite_form.fxml", "Modifier Activité", a, -1, actDate);
        });
        contextMenu.getItems().addAll(doneItem, new javafx.scene.control.SeparatorMenuItem(), focusItem, editItem);
        card.setOnContextMenuRequested(e -> contextMenu.show(card, e.getScreenX(), e.getScreenY()));
        
        card.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                LocalDate actDate = viewMode.equals("DAY") ? currentDayView : currentMonday.plusDays(colIndex - 1);
                openForm("activite_form.fxml", "Modifier Activité", a, -1, actDate);
            }
        });

        // Layout geometry
        double topOffset = (startMin / 60.0) * 60;
        double height = Math.max(45, (durationMin / 60.0) * 60);
        int rowSpan = (int) Math.ceil((startMin + durationMin) / 60.0);
        if (rowSpan < 1) rowSpan = 1;

        card.setTranslateY(topOffset);
        card.setPrefHeight(height);
        card.setMinHeight(height);
        card.setMaxHeight(height);

        // Side-by-side logic: use dynamic width and translation based on overlap
        int numCols = viewMode.equals("DAY") ? 1 : 7;
        javafx.beans.binding.DoubleBinding colWidth = calendarGrid.widthProperty().subtract(60).divide(numCols);
        
        card.maxWidthProperty().bind(colWidth.divide(totalInOverlap).subtract(5));
        card.translateXProperty().bind(colWidth.divide(totalInOverlap).multiply(indexInOverlap));
        
        calendarGrid.add(card, colIndex, startHour, 1, rowSpan);
    }

    @FXML
    private void handleSeeAll() {
        if (DashboardController.getInstance() != null) {
            DashboardController.getInstance().setView("advanced_stats.fxml");
        }
    }

    @FXML
    private void handleAIOptimize() {
        List<String> choices = new ArrayList<>();
        choices.add("Aujourd'hui uniquement (" + (viewMode.equals("DAY") ? currentDayView : LocalDate.now()) + ")");
        choices.add("Toute la semaine (du " + currentMonday + " au " + currentMonday.plusDays(6) + ")");

        javafx.scene.control.ChoiceDialog<String> scopeDialog = new javafx.scene.control.ChoiceDialog<>(choices.get(0), choices);
        scopeDialog.setTitle("IA Assistance");
        scopeDialog.setHeaderText("Portée de l'optimisation");
        scopeDialog.setContentText("Que voulez-vous optimiser ?");

        scopeDialog.showAndWait().ifPresent(scope -> {
            boolean onlyDay = scope.startsWith("Aujourd'hui");
            
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("Organise ma productivité");
            dialog.setTitle("IA Assistance");
            dialog.setHeaderText("Instructions pour l'IA (" + (onlyDay ? "Journée" : "Semaine") + ")");
            dialog.setContentText("Que voulez-vous demander à l'IA ?");
            
            dialog.showAndWait().ifPresent(userRequest -> {
                btnIA.setDisable(true);
                btnIA.setText("🤖 Analyse en cours...");
                
                final LocalDate start, end;
                if (onlyDay) {
                    start = viewMode.equals("DAY") ? currentDayView : LocalDate.now();
                    end = start;
                } else {
                    start = currentMonday;
                    end = currentMonday.plusDays(6);
                }
                
                javafx.concurrent.Task<com.google.gson.JsonObject> task = new javafx.concurrent.Task<>() {
                    @Override
                    protected com.google.gson.JsonObject call() throws Exception {
                        return aiService.optimizePlanning(userRequest, start, end, currentUserId);
                    }
                };
            
            task.setOnSucceeded(e -> {
                com.google.gson.JsonObject result = task.getValue();
                btnIA.setDisable(false);
                btnIA.setText("🤖 Optimiser avec l'IA");

                // Build a detailed breakdown of suggestions per day
                StringBuilder details = new StringBuilder();
                details.append(result.get("summary").getAsString()).append("\n\n");
                details.append("--- DÉTAILS DES CHANGEMENTS ---\n");

                com.google.gson.JsonArray suggestions = result.getAsJsonArray("suggestions");
                if (suggestions != null) {
                    for (com.google.gson.JsonElement dayElem : suggestions) {
                        com.google.gson.JsonObject day = dayElem.getAsJsonObject();
                        details.append("\n📅 ").append(day.get("date").getAsString()).append(" :\n");

                        com.google.gson.JsonArray newActs = day.getAsJsonArray("new_activities");
                        if (newActs != null && !newActs.isEmpty()) {
                            details.append("   ➕ Nouvelles : ");
                            for (com.google.gson.JsonElement a : newActs) {
                                details.append(a.getAsJsonObject().get("title").getAsString()).append(", ");
                            }
                            details.setLength(details.length() - 2); // remove last comma
                            details.append("\n");
                        }

                        com.google.gson.JsonArray mods = day.getAsJsonArray("modifications");
                        if (mods != null && !mods.isEmpty()) {
                            details.append("   🔄 Modifiées : ");
                            for (com.google.gson.JsonElement m : mods) {
                                details.append(m.getAsJsonObject().get("original_title").getAsString()).append(", ");
                            }
                            details.setLength(details.length() - 2);
                            details.append("\n");
                        }
                    }
                }
                details.append("\nVoulez-vous appliquer ces changements ?");

                // Show confirmation dialog before applying
                javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Suggestions de l'IA");
                confirm.setHeaderText("Plan d'optimisation généré");
                
                // Use a ScrollPane for the detailed content if it's too long
                javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(details.toString());
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setPrefHeight(300);
                confirm.getDialogPane().setContent(textArea);
                
                javafx.scene.control.ButtonType btnAppliquer = new javafx.scene.control.ButtonType("Appliquer");
                javafx.scene.control.ButtonType btnAnnuler = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                confirm.getButtonTypes().setAll(btnAppliquer, btnAnnuler);

                confirm.showAndWait().ifPresent(response -> {
                    if (response == btnAppliquer) {
                        try {
                            com.google.gson.JsonObject report = aiService.saveConfirmedSuggestions(result, currentUserId, userRequest);
                            handleFilterReset(); // Reset filters to show the new activities
                            refresh();
                            
                            javafx.scene.control.Alert success = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                            success.setTitle("Optimisation Terminée");
                            success.setHeaderText("Rapport d'optimisation");
                            
                            StringBuilder reportMsg = new StringBuilder();
                            reportMsg.append("✅ L'optimisation a été appliquée avec succès !\n\n");
                            reportMsg.append("📊 Rapport :\n");
                            reportMsg.append(" - Activités ajoutées : ").append(report.get("nb_ajouts").getAsInt()).append("\n");
                            reportMsg.append(" - Activités modifiées : ").append(report.get("nb_modifications").getAsInt()).append("\n");
                            reportMsg.append(" - Anciennes suggestions supprimées : ").append(report.get("nb_suppressions").getAsInt()).append("\n\n");
                            reportMsg.append("📝 Résumé : ").append(report.get("resume").getAsString());
                            
                            success.setContentText(reportMsg.toString());
                            success.showAndWait();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            showError("Erreur lors de l'enregistrement : " + ex.getMessage());
                        }
                    }
                });
            });
            
            task.setOnFailed(e -> {
                btnIA.setDisable(false);
                btnIA.setText("🤖 Optimiser avec l'IA");
                showError("L'IA n'a pas pu traiter votre demande : " + task.getException().getMessage());
                task.getException().printStackTrace();
            });
            
            new Thread(task).start();
        });
    });
}

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
    private void openFocusSession(Activite a) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/Time/focus_session.fxml"));
            javafx.scene.Parent root = loader.load();
            FocusSessionController ctrl = loader.getController();
            ctrl.setActivite(a);
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Focus Session - " + a.getTitre());
            stage.setScene(new javafx.scene.Scene(root));
            stage.setResizable(false);
            stage.setOnHidden(e -> refresh()); // Refresh dashboard after focus session
            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // ─── FEATURE 4 : EXPORT / IMPORT ──────────────────────────────
    
    @FXML
    private void handleExportPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Planning (PDF)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(lblCurrentDateTitle.getScene().getWindow());

        if (file != null) {
            try {
                PdfWriter writer = new PdfWriter(new FileOutputStream(file));
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                // 1. Title
                DeviceRgb purple = new DeviceRgb(139, 92, 246);
                Paragraph title = new Paragraph("LIFEOPS - PLANNING")
                        .setFontColor(purple)
                        .setFontSize(22)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER);
                document.add(title);

                String period = lblCurrentDateTitle.getText();
                Paragraph subtitle = new Paragraph("Période : " + period)
                        .setFontColor(ColorConstants.GRAY)
                        .setFontSize(14)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20);
                document.add(subtitle);

                // 2. Table: Time | Activity | Category | Priority | State
                Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 4f, 2f, 1.5f, 1.5f}))
                        .useAllAvailableWidth();

                DeviceRgb headerBg = new DeviceRgb(31, 31, 35);
                String[] headers = {"HEURE", "ACTIVITÉ", "CATÉGORIE", "PRIORITÉ", "ÉTAT"};
                for (String h : headers) {
                    table.addHeaderCell(new Cell()
                            .add(new Paragraph(h).setBold().setFontColor(ColorConstants.WHITE).setFontSize(11))
                            .setBackgroundColor(headerBg)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(8));
                }

                // 3. Data
                List<Planning> dataList = new ArrayList<>();
                if (viewMode.equals("DAY")) {
                    Planning p = planningService.recupererParDate(currentDayView, currentUserId);
                    if (p != null) dataList.add(p);
                } else {
                    dataList.addAll(planningService.recupererParSemaine(currentUserId, java.sql.Date.valueOf(currentMonday)));
                }

                DeviceRgb subHeaderBg = new DeviceRgb(243, 244, 246);
                for (Planning p : dataList) {
                    // Day sub-header row
                    table.addCell(new Cell(1, 5)
                            .add(new Paragraph(p.getDate().toString().toUpperCase()).setBold().setFontSize(10))
                            .setBackgroundColor(subHeaderBg)
                            .setPadding(5));

                    List<Activite> acts = activiteService.recupererParPlanning(p.getId());
                    acts.sort((a1, a2) -> a1.getHeureDebutEstimee().compareTo(a2.getHeureDebutEstimee()));

                    for (Activite a : acts) {
                        if (!passesFilter(a, p.getDate().toLocalDate())) continue;
                        table.addCell(new Cell().add(new Paragraph(
                                a.getHeureDebutEstimee().toString().substring(0, 5) + " - " +
                                a.getHeureFinEstimee().toString().substring(0, 5)).setFontSize(10)));
                        table.addCell(new Cell().add(new Paragraph(a.getTitre()).setFontSize(10)));
                        table.addCell(new Cell().add(new Paragraph(a.getCategorie()).setFontSize(10)));
                        table.addCell(new Cell().add(new Paragraph(String.valueOf(a.getPriorite())).setFontSize(10)));
                        table.addCell(new Cell().add(new Paragraph(a.getStatutDynamique(p.getDate())).setFontSize(10)));
                    }
                }
                document.add(table);

                // 4. Footer
                Paragraph footer = new Paragraph(
                        "\nGénéré par LifeOps AI - " +
                        java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                        .setFontColor(ColorConstants.LIGHT_GRAY)
                        .setFontSize(8)
                        .setItalic()
                        .setTextAlignment(TextAlignment.RIGHT);
                document.add(footer);

                document.close();

                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Export Réussi");
                alert.setContentText("Le planning PDF a été généré avec succès.");
                alert.show();

            } catch (Exception e) {
                e.printStackTrace();
                showError("Erreur lors de la génération du PDF : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportICS() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en iCalendar (.ics)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier iCalendar", "*.ics"));
        File file = fileChooser.showSaveDialog(lblCurrentDateTitle.getScene().getWindow());
        
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("BEGIN:VCALENDAR");
                writer.println("VERSION:2.0");
                writer.println("PRODID:-//LifeOps//Planning//FR");
                
                List<Planning> weekPlannings = planningService.recupererParSemaine(currentUserId, java.sql.Date.valueOf(currentMonday));
                for (Planning p : weekPlannings) {
                    List<Activite> acts = activiteService.recupererParPlanning(p.getId());
                    for (Activite a : acts) {
                        writer.println("BEGIN:VEVENT");
                        writer.println("SUMMARY:" + a.getTitre());
                        writer.println("DESCRIPTION:" + a.getCategorie() + " - Priorité:" + a.getPriorite());
                        
                        String dStr = p.getDate().toString().replace("-", "");
                        String sTime = a.getHeureDebutEstimee().toString().replace(":", "") + "Z";
                        String eTime = a.getHeureFinEstimee().toString().replace(":", "") + "Z";
                        
                        writer.println("DTSTART:" + dStr + "T" + sTime);
                        writer.println("DTEND:" + dStr + "T" + eTime);
                        writer.println("END:VEVENT");
                    }
                }
                writer.println("END:VCALENDAR");
                
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Export Réussi");
                alert.setContentText("Fichier .ics généré avec succès.");
                alert.show();
            } catch (Exception e) {
                showError("Erreur d'export : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleImportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importer depuis CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        File file = fileChooser.showOpenDialog(lblCurrentDateTitle.getScene().getWindow());
        
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                int count = 0;
                // Format attendu: Date (YYYY-MM-DD), Titre, Début (HH:mm), Fin (HH:mm), Catégorie
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Date")) continue; // Skip header
                    String[] parts = line.split(",");
                    if (parts.length >= 5) {
                        LocalDate date = LocalDate.parse(parts[0].trim());
                        String titre = parts[1].trim();
                        Time start = Time.valueOf(parts[2].trim() + ":00");
                        Time end = Time.valueOf(parts[3].trim() + ":00");
                        String cat = parts[4].trim();
                        
                        Planning targetP = planningService.recupererParDate(date, currentUserId);
                        if (targetP == null) {
                            targetP = new Planning(java.sql.Date.valueOf(date), true, Time.valueOf("08:00:00"), Time.valueOf("20:00:00"), currentUserId);
                            planningService.ajouter(targetP);
                        }
                        
                        Activite a = new Activite(0, titre, 0, 2, "en_attente", start, end, "moyen", cat, "#8b5cf6", false, targetP.getId(), 0);
                        activiteService.ajouter(a);
                        count++;
                    }
                }
                refresh();
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Import Réussi");
                alert.setContentText(count + " activités importées avec succès.");
                alert.show();
            } catch (Exception e) {
                showError("Erreur d'import : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
