package controller;

import enums.StatutTache;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import model.User;
import model.task.Tache;
import service.TaskService;
import service.UserService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class BackOfficeController {

    @FXML private Label lblOverdueCount;
    @FXML private Label lblStuckCount;
    @FXML private Label lblOverloadedCount;
    @FXML private Label lblActiveFilter;
    
    @FXML private PieChart statusPieChart;
    @FXML private LineChart<String, Number> activityLineChart;
    @FXML private BarChart<String, Number> workloadBarChart;
    
    @FXML private ListView<ProblemItem> problemsList;
    @FXML private ListView<UserPerformance> usersPerformanceList;

    private final TaskService taskService = new TaskService();
    private final UserService userService = new UserService();
    
    private ObservableList<ProblemItem> allProblems = FXCollections.observableArrayList();
    private List<Tache> cachedTasks;

    @FXML
    public void initialize() {
        setupLists();
        loadData();
    }

    private void setupLists() {
        problemsList.setCellFactory(param -> new ProblemCell(this));
        usersPerformanceList.setCellFactory(param -> new UserPerformanceCell());
    }

    public void loadData() {
        allProblems.clear();
        
        cachedTasks = taskService.readAll();
        List<User> allUsers = userService.getAllUsers();
        
        int overdue = 0;
        int stuck = 0;
        
        long now = System.currentTimeMillis();
        long threeDaysMs = 3L * 24 * 60 * 60 * 1000;
        
        // Data maps for charts
        Map<StatutTache, Integer> statusCounts = new HashMap<>();
        Map<String, Integer> createdPerDay = new TreeMap<>();
        Map<String, Integer> completedPerDay = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");

        for (Tache t : cachedTasks) {
            // Populate PieChart data
            statusCounts.put(t.getStatut(), statusCounts.getOrDefault(t.getStatut(), 0) + 1);
            
            // Populate LineChart data
            if (t.getCreatedAt() != null) {
                String date = sdf.format(t.getCreatedAt());
                createdPerDay.put(date, createdPerDay.getOrDefault(date, 0) + 1);
            }
            if (t.getStatut() == StatutTache.TERMINE && t.getUpdatedAt() != null) {
                String date = sdf.format(t.getUpdatedAt());
                completedPerDay.put(date, completedPerDay.getOrDefault(date, 0) + 1);
            }

            if (t.getStatut() == StatutTache.TERMINE) continue;
            
            long lastActivity = t.getUpdatedAt() != null ? t.getUpdatedAt().getTime() : 
                               (t.getCreatedAt() != null ? t.getCreatedAt().getTime() : now);

            // 1. Overdue detection
            if (t.getDeadline() != null && t.getDeadline().getTime() < now) {
                overdue++;
                allProblems.add(new ProblemItem("OVERDUE", "Retard Critique", 
                    "La tâche '" + t.getTitre() + "' a dépassé sa deadline.", 
                    "💡 Action suggérée : Étendre la deadline de 48h ou réassigner.", t, null));
            }
            // 2. Stuck detection
            else if (t.getStatut() == StatutTache.EN_COURS && (now - lastActivity > threeDaysMs)) {
                stuck++;
                allProblems.add(new ProblemItem("STUCK", "Tâche inactive / bloquée", 
                    "La tâche '" + t.getTitre() + "' n'a pas progressé depuis >3 jours.", 
                    "💡 Action suggérée : Forcer le statut EN_REVISION pour contrôle.", t, null));
            }
            // 3. Inefficient detection (High difficulty, no updates for 5 days)
            else if (t.getDifficulte() > 3 && (now - lastActivity > 5L * 24 * 60 * 60 * 1000)) {
                allProblems.add(new ProblemItem("INEFFICIENT", "Tâche Complexe Stagnante", 
                    "Tâche '" + t.getTitre() + "' (Diff: " + t.getDifficulte() + ") n'avance pas.", 
                    "💡 Action suggérée : Réassigner à un développeur senior.", t, null));
            }
        }
        
        lblOverdueCount.setText(String.valueOf(overdue));
        lblStuckCount.setText(String.valueOf(stuck));
        
        // Automation 4: Analyze users workload & efficiency
        int overloaded = 0;
        List<UserPerformance> perfList = new ArrayList<>();
        XYChart.Series<String, Number> workloadSeries = new XYChart.Series<>();
        workloadSeries.setName("Tâches Actives");

        for (User u : allUsers) {
            long activeTasks = cachedTasks.stream()
                .filter(t -> t.getAssignedUserId() != null && t.getAssignedUserId() == u.getId() && t.getStatut() != StatutTache.TERMINE)
                .count();
                
            long doneTasks = cachedTasks.stream()
                .filter(t -> t.getAssignedUserId() != null && t.getAssignedUserId() == u.getId() && t.getStatut() == StatutTache.TERMINE)
                .count();

            if (activeTasks > 0 || doneTasks > 0) {
                workloadSeries.getData().add(new XYChart.Data<>(u.getNom(), activeTasks));
            }

            if (activeTasks >= 4) {
                overloaded++;
                allProblems.add(new ProblemItem("OVERLOADED", "Utilisateur Surchargé", 
                    u.getFullName() + " a " + activeTasks + " tâches actives en parallèle.", 
                    "💡 Action suggérée : Délester 2 tâches vers un autre membre.", null, u));
            }
            
            // Calculate a pseudo efficiency score: done / (done + active)
            double eff = (doneTasks + activeTasks) == 0 ? 0 : (double) doneTasks / (doneTasks + activeTasks);
            perfList.add(new UserPerformance(u, (int)activeTasks, (int)doneTasks, eff));
        }
        
        lblOverloadedCount.setText(String.valueOf(overloaded));
        
        problemsList.setItems(allProblems);
        usersPerformanceList.setItems(FXCollections.observableArrayList(perfList));
        lblActiveFilter.setText("Filtre: Tous");
        
        updateCharts(statusCounts, createdPerDay, completedPerDay, workloadSeries);
    }
    
    private void updateCharts(Map<StatutTache, Integer> statusCounts, Map<String, Integer> created, Map<String, Integer> completed, XYChart.Series<String, Number> workloadSeries) {
        // Pie Chart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for(Map.Entry<StatutTache, Integer> entry : statusCounts.entrySet()) {
            PieChart.Data data = new PieChart.Data(entry.getKey().name(), entry.getValue());
            pieData.add(data);
        }
        statusPieChart.setData(pieData);
        // Add interactivity
        for(PieChart.Data d : pieData) {
            d.getNode().setOnMouseClicked(e -> filterByStatus(d.getName()));
            d.getNode().setStyle("-fx-cursor: hand;");
        }

        // Line Chart
        activityLineChart.getData().clear();
        XYChart.Series<String, Number> seriesC = new XYChart.Series<>();
        seriesC.setName("Créées");
        for(Map.Entry<String, Integer> entry : created.entrySet()) seriesC.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        
        XYChart.Series<String, Number> seriesD = new XYChart.Series<>();
        seriesD.setName("Terminées");
        for(Map.Entry<String, Integer> entry : completed.entrySet()) seriesD.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        
        activityLineChart.getData().addAll(seriesC, seriesD);
        
        // Bar Chart
        workloadBarChart.getData().clear();
        workloadBarChart.getData().add(workloadSeries);
    }

    private void filterByStatus(String statusStr) {
        problemsList.setItems(allProblems.filtered(p -> p.task != null && p.task.getStatut().name().equals(statusStr)));
        lblActiveFilter.setText("Filtre Chart: " + statusStr);
    }

    @FXML private void filterOverdue() {
        problemsList.setItems(allProblems.filtered(p -> "OVERDUE".equals(p.type)));
        lblActiveFilter.setText("Filtre: En retard");
    }
    
    @FXML private void filterStuck() {
        problemsList.setItems(allProblems.filtered(p -> "STUCK".equals(p.type)));
        lblActiveFilter.setText("Filtre: Bloquées");
    }
    
    @FXML private void filterOverloaded() {
        problemsList.setItems(allProblems.filtered(p -> "OVERLOADED".equals(p.type)));
        lblActiveFilter.setText("Filtre: Surchargés");
    }

    public void openActionPopup(ProblemItem problem) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Task/backoffice_action_popup.fxml"));
            Parent root = loader.load();
            
            BackOfficeActionController controller = loader.getController();
            controller.setProblem(problem, this);
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED); 
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Data Classes
    public static class ProblemItem {
        public String type, title, description, suggestion;
        public Tache task;
        public User user;
        
        public ProblemItem(String type, String title, String description, String suggestion, Tache task, User user) {
            this.type = type; this.title = title; this.description = description; this.suggestion = suggestion;
            this.task = task; this.user = user;
        }
    }
    
    public static class UserPerformance {
        public User user;
        public int activeTasks, doneTasks;
        public double efficiency;
        public UserPerformance(User user, int activeTasks, int doneTasks, double efficiency) {
            this.user = user; this.activeTasks = activeTasks; this.doneTasks = doneTasks; this.efficiency = efficiency;
        }
    }

    // Cells
    class ProblemCell extends ListCell<ProblemItem> {
        private final BackOfficeController parent;
        public ProblemCell(BackOfficeController parent) { this.parent = parent; }
        
        @Override
        protected void updateItem(ProblemItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                HBox box = new HBox(15);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setStyle("-fx-background-color: #1e293b; -fx-padding: 12; -fx-background-radius: 8;");
                
                Label icon = new Label();
                icon.setStyle("-fx-font-size: 24px;");
                if(item.type.equals("OVERDUE")) icon.setText("🔴");
                else if(item.type.equals("STUCK") || item.type.equals("INEFFICIENT")) icon.setText("🟠");
                else if(item.type.equals("OVERLOADED")) icon.setText("🟡");
                
                VBox text = new VBox(4);
                Label title = new Label(item.title);
                title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                Label desc = new Label(item.description);
                desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
                Label sugg = new Label(item.suggestion);
                sugg.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: bold;"); // Green
                
                text.getChildren().addAll(title, desc, sugg);
                HBox.setHgrow(text, Priority.ALWAYS);
                
                Button btn = new Button("Agir");
                btn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                btn.setOnAction(e -> parent.openActionPopup(item));
                
                box.getChildren().addAll(icon, text, btn);
                setGraphic(box);
                setStyle("-fx-background-color: transparent; -fx-padding: 5;");
            }
        }
    }

    class UserPerformanceCell extends ListCell<UserPerformance> {
        @Override
        protected void updateItem(UserPerformance item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                VBox mainBox = new VBox(8);
                mainBox.setStyle("-fx-background-color: #1e293b; -fx-padding: 10; -fx-background-radius: 8;");
                
                HBox header = new HBox();
                Label name = new Label(item.user.getFullName());
                name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS);
                Label score = new Label(String.format("Efficacité: %.0f%%", item.efficiency * 100));
                score.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");
                header.getChildren().addAll(name, r, score);
                
                HBox pbBox = new HBox();
                ProgressBar pb = new ProgressBar(item.efficiency);
                pb.setPrefWidth(Double.MAX_VALUE);
                HBox.setHgrow(pb, Priority.ALWAYS);
                
                // Color coding
                if(item.efficiency >= 0.7) pb.setStyle("-fx-accent: #10b981;"); // Green
                else if(item.efficiency >= 0.4) pb.setStyle("-fx-accent: #f59e0b;"); // Orange
                else pb.setStyle("-fx-accent: #ef4444;"); // Red
                pbBox.getChildren().add(pb);
                
                Label stats = new Label(item.doneTasks + " terminées | " + item.activeTasks + " en cours");
                stats.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");
                
                mainBox.getChildren().addAll(header, pbBox, stats);
                setGraphic(mainBox);
                setStyle("-fx-background-color: transparent; -fx-padding: 4;");
            }
        }
    }
}
