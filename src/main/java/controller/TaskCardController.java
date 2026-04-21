package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.task.Tache;

public class TaskCardController {

    @FXML private VBox taskCardRoot;
    @FXML private Label lblTitle;
    @FXML private Label lblEstimatedTime;
    @FXML private Label lblDifficulty;
    @FXML private Region priorityIndicator;

    private Tache task;
    private BoardViewController boardController;

    public void setData(Tache task, BoardViewController boardController) {
        this.task = task;
        this.boardController = boardController;

        lblTitle.setText(task.getTitre());
        lblEstimatedTime.setText(task.getDeadline() != null ? task.getDeadline().toString().substring(0, 10) : "No Date");
        lblDifficulty.setText(task.getDifficulte() + "/5");

        // Styling based on priority
        priorityIndicator.getStyleClass().removeAll("priority-high", "priority-medium", "priority-low");
        switch (task.getPriorite()) {
            case HAUTE, URGENTE -> priorityIndicator.getStyleClass().add("priority-high");
            case MOYENNE -> priorityIndicator.getStyleClass().add("priority-medium");
            case BASSE -> priorityIndicator.getStyleClass().add("priority-low");
        }
    }

    @FXML
    private void handleCardClick() {
        boardController.showTaskDetails(task);
    }
}
