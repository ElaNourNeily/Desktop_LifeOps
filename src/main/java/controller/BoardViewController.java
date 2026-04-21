package controller;

import enums.StatutTache;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.task.Tache;
import model.task.TaskSpace;
import service.TaskService;
import service.TaskSpaceService;

import java.io.IOException;
import java.util.List;

public class BoardViewController {

    @FXML private Label lblBoardTitle;
    @FXML private Label lblBoardSubtitle;
    @FXML private Label lblBoardType;
    @FXML private VBox colToDo;
    @FXML private VBox colInProgress;
    @FXML private VBox colReview;
    @FXML private VBox colDone;

    private TaskSpace currentBoard;
    private final TaskService taskService = new TaskService();
    private final TaskSpaceService spaceService = new TaskSpaceService();

    @FXML
    public void initialize() {
        setupDragAndDrop();
    }

    public void setBoard(TaskSpace space) {
        this.currentBoard = space;
        lblBoardTitle.setText(space.getNom());
        // Updated to use Category and Mode
        lblBoardType.setText(space.getCategory().toUpperCase() + " (" + space.getMode() + ")");
        lblBoardSubtitle.setText("Sprint de " + space.getDuration() + " jours");
        loadTasks();
    }

    public void loadTasks() {
        colToDo.getChildren().clear();
        colInProgress.getChildren().clear();
        colReview.getChildren().clear();
        colDone.getChildren().clear();

        List<Tache> tasks = taskService.getTasksByBoard(currentBoard.getId());
        for (Tache task : tasks) {
            addTaskToColumn(task);
        }
    }

    private void addTaskToColumn(Tache task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Task/task_card.fxml"));
            Parent card = loader.load();
            
            TaskCardController controller = loader.getController();
            controller.setData(task, this);

            // Enable Drag
            card.setOnDragDetected(event -> {
                javafx.scene.input.Dragboard db = card.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(String.valueOf(task.getId()));
                db.setContent(content);
                event.consume();
            });

            switch (task.getStatut()) {
                case A_FAIRE -> colToDo.getChildren().add(card);
                case EN_COURS -> colInProgress.getChildren().add(card);
                case EN_REVISION -> colReview.getChildren().add(card);
                case TERMINE -> colDone.getChildren().add(card);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupDragAndDrop() {
        setupColumnDragHandlers(colToDo, StatutTache.A_FAIRE);
        setupColumnDragHandlers(colInProgress, StatutTache.EN_COURS);
        setupColumnDragHandlers(colReview, StatutTache.EN_REVISION);
        setupColumnDragHandlers(colDone, StatutTache.TERMINE);
    }

    private void setupColumnDragHandlers(VBox column, StatutTache targetStatus) {
        column.setOnDragOver(event -> {
            if (event.getGestureSource() != column && event.getDragboard().hasString()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
            }
            event.consume();
        });

        column.setOnDragDropped(event -> {
            javafx.scene.input.Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                int taskId = Integer.parseInt(db.getString());
                updateTaskStatus(taskId, targetStatus);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void updateTaskStatus(int taskId, StatutTache newStatus) {
        List<Tache> tasks = taskService.getTasksByBoard(currentBoard.getId());
        for (Tache t : tasks) {
            if (t.getId() == taskId) {
                t.setStatut(newStatus);
                taskService.updateTask(t);
                loadTasks();
                break;
            }
        }
    }

    public void showTaskDetails(Tache task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Task/task_form_popup.fxml"));
            Parent root = loader.load();
            
            TaskFormPopupController controller = loader.getController();
            controller.setContext(currentBoard.getId(), currentBoard.getUtilisateurId(), this);
            controller.fillForm(task); // Edit mode

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddTask(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Task/task_form_popup.fxml"));
            Parent root = loader.load();
            
            TaskFormPopupController controller = loader.getController();
            controller.setContext(currentBoard.getId(), currentBoard.getUtilisateurId(), this);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteBoard(ActionEvent event) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer ce tableau ?");
        alert.setContentText("Cette action supprimera également toutes les tâches associées.");

        if (alert.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
            spaceService.deleteTaskSpace(currentBoard.getId());
            handleBack(null); // Return to hub
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Task/board_hub.fxml"));
            lblBoardTitle.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
