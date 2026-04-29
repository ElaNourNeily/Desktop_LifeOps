package controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.User;
import model.task.Recommendation;
import model.task.Tache;
import service.TaskService;
import service.UserService;

import java.util.List;

public class AIAssignmentPopupController {

    @FXML
    private VBox recommendationsContainer;

    @FXML
    private Button ignoreButton;

    private Tache currentTask;
    private List<Recommendation> recommendations;
    private Runnable onAssignedCallback;
    private final TaskService taskService = new TaskService();
    private final UserService userService = new UserService(); // Assumes exists

    public void initData(Tache task, List<Recommendation> recs, Runnable onAssignedCallback) {
        this.currentTask = task;
        this.recommendations = recs;
        this.onAssignedCallback = onAssignedCallback;
        populateRecommendations();
    }

    private void populateRecommendations() {
        recommendationsContainer.getChildren().clear();

        if (recommendations == null || recommendations.isEmpty()) {
            Label noRecs = new Label("No recommendations available.");
            noRecs.setStyle("-fx-text-fill: white;");
            recommendationsContainer.getChildren().add(noRecs);
            return;
        }

        for (int i = 0; i < recommendations.size(); i++) {
            Recommendation rec = recommendations.get(i);
            User user = userService.getById(rec.getUserId()); // Assuming getById exists
            if (user == null) {
                // fallback if not found, just show ID
                user = new User();
                user.setId(rec.getUserId());
                user.setNom("User " + rec.getUserId());
                user.setPrenom("");
            }

            VBox card = new VBox(5);
            card.getStyleClass().add("suggestion-card");
            
            if (i == 0) {
                card.getStyleClass().add("task-card-recommended");
            }

            HBox headerRow = new HBox(10);
            headerRow.setAlignment(Pos.CENTER_LEFT);
            Label nameLabel = new Label((i == 0 ? "⭐ " : "") + user.getPrenom() + " " + user.getNom());
            nameLabel.getStyleClass().add("suggestion-title");
            
            Label scoreLabel = new Label("Score: " + rec.getScore());
            scoreLabel.setStyle("-fx-text-fill: #facc15; -fx-font-weight: bold; -fx-font-size: 11px;");
            
            Label timeLabel = new Label("Est. Time: " + rec.getPredictedTime() + "h");
            timeLabel.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 11px;");
            
            HBox spacer = new HBox();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            final User finalUser = user;
            Button assignBtn = new Button("Assigner");
            assignBtn.getStyleClass().add("btn-suggestion-accept");
            assignBtn.setOnAction(e -> handleAssign(finalUser));
            
            headerRow.getChildren().addAll(nameLabel, spacer, scoreLabel, timeLabel, assignBtn);

            Label reasonLabel = new Label("Raison: " + rec.getReason());
            reasonLabel.getStyleClass().add("suggestion-reason");
            reasonLabel.setWrapText(true);

            card.getChildren().addAll(headerRow, reasonLabel);
            recommendationsContainer.getChildren().add(card);
        }
    }

    private void handleAssign(User user) {
        taskService.assignTaskToUser(currentTask, user);
        if (onAssignedCallback != null) {
            onAssignedCallback.run();
        }
        closePopup();
    }

    @FXML
    private void handleIgnore() {
        closePopup();
    }

    private void closePopup() {
        if (ignoreButton.getScene() != null && ignoreButton.getScene().getWindow() instanceof Stage) {
            ((Stage) ignoreButton.getScene().getWindow()).close();
        }
    }
}
