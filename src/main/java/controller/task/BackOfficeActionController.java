package controller.task;

import enums.StatutTache;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.user.User;
import model.task.Tache;
import service.task.TaskService;
import service.user.UserService;

public class BackOfficeActionController {

    @FXML private Label lblTitle;
    @FXML private Label lblDescription;
    @FXML private Label lblSuggestion;
    @FXML private VBox actionArea;

    private ProblemItem problem;
    private Object parentController; // Not used in automated mode
    private final TaskService taskService = new TaskService();
    private final UserService userService = new UserService();
    
    // UI Elements
    private DatePicker datePicker;
    private ComboBox<User> userCombo;

    public static class ProblemItem {
        public String type, title, description, suggestion;
        public Tache task;
        public User user;
        public ProblemItem(String type, String title, String description, String suggestion, Tache task, User user) {
            this.type = type; this.title = title; this.description = description; this.suggestion = suggestion;
            this.task = task; this.user = user;
        }
    }

    public void setProblem(ProblemItem problem, Object parentController) {
        this.problem = problem;
        this.parentController = parentController;
        
        lblTitle.setText("Action: " + problem.title);
        lblDescription.setText(problem.description);
        lblSuggestion.setText(problem.suggestion);
        
        buildActionUI();
    }

    private void buildActionUI() {
        actionArea.getChildren().clear();
        
        switch (problem.type) {
            case "OVERDUE":
                Label l1 = new Label("Nouvelle date (Optionnel) :");
                l1.setStyle("-fx-text-fill: white;");
                datePicker = new DatePicker();
                if (problem.task.getDeadline() != null) {
                    datePicker.setValue(new java.sql.Timestamp(problem.task.getDeadline().getTime()).toLocalDateTime().toLocalDate());
                }
                
                Label l2 = new Label("Réassigner à :");
                l2.setStyle("-fx-text-fill: white; -fx-padding: 10 0 0 0;");
                userCombo = new ComboBox<>(FXCollections.observableArrayList(userService.getAllUsers()));
                userCombo.setPromptText("Choisir un utilisateur...");
                if (problem.task.getAssignedUserId() != null) {
                    for(User u : userCombo.getItems()) {
                        if(u.getId() == problem.task.getAssignedUserId()) {
                            userCombo.setValue(u);
                            break;
                        }
                    }
                }
                
                actionArea.getChildren().addAll(l1, datePicker, l2, userCombo);
                break;
                
            case "STUCK":
                Label l3 = new Label("Action recommandée : Forcer le statut à EN_REVISION.");
                l3.setStyle("-fx-text-fill: white; -fx-wrap-text: true;");
                actionArea.getChildren().add(l3);
                break;
                
            case "INEFFICIENT":
                Label l4 = new Label("Réassigner à (Recommandé: Développeur Senior) :");
                l4.setStyle("-fx-text-fill: white;");
                userCombo = new ComboBox<>(FXCollections.observableArrayList(userService.getAllUsers()));
                userCombo.setPromptText("Choisir un utilisateur...");
                actionArea.getChildren().addAll(l4, userCombo);
                break;
                
            case "OVERLOADED":
                Label l5 = new Label("L'utilisateur " + problem.user.getFullName() + " a trop de tâches.");
                l5.setStyle("-fx-text-fill: white; -fx-wrap-text: true;");
                actionArea.getChildren().add(l5);
                break;
        }
    }

    @FXML
    private void handleConfirm() {
        try {
            switch (problem.type) {
                case "OVERDUE":
                    if (datePicker.getValue() != null) {
                        problem.task.setDeadline(java.sql.Date.valueOf(datePicker.getValue()));
                    }
                    if (userCombo.getValue() != null) {
                        problem.task.setAssignedUserId(userCombo.getValue().getId());
                    }
                    taskService.update(problem.task);
                    break;
                    
                case "STUCK":
                    problem.task.setStatut(StatutTache.EN_REVISION);
                    taskService.update(problem.task);
                    break;
                    
                case "INEFFICIENT":
                    if (userCombo.getValue() != null) {
                        problem.task.setAssignedUserId(userCombo.getValue().getId());
                        taskService.update(problem.task);
                    }
                    break;
                    
                case "OVERLOADED":
                    showAlert("Information", "Veuillez réassigner ses tâches manuellement depuis le Board.");
                    break;
            }
            
            // Parent controller no longer manually reloaded in n8n automated setup
            // handleCancel();
            handleCancel();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Une erreur s'est produite lors de l'action.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleCancel() {
        ((javafx.stage.Stage) lblTitle.getScene().getWindow()).close();
    }
}
