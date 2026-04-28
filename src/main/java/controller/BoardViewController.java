package controller;

import enums.StatutTache;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.task.Tache;
import model.task.TaskSpace;
import service.TaskService;
import service.TaskSpaceService;
import service.TaskSpaceUserService;
import utils.Session;

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
    @FXML private Button btnInvite;
    @FXML private Button btnDeleteBoard;

    private TaskSpace currentBoard;
    private String currentUserRole;
    private final TaskService taskService = new TaskService();
    private final TaskSpaceService spaceService = new TaskSpaceService();
    private final TaskSpaceUserService spaceUserService = new TaskSpaceUserService();

    @FXML
    public void initialize() {
        setupDragAndDrop();
    }

    public void setBoard(TaskSpace space) {
        this.currentBoard = space;
        
        if (Session.isLoggedIn()) {
            currentUserRole = spaceUserService.getUserRoleInBoard(Session.getCurrentUser().getId(), space.getId());
            if (currentUserRole == null && Session.getCurrentUser().getId() == space.getLeaderId()) {
                currentUserRole = "LEADER";
            }
        }

        boolean isLeader = "LEADER".equals(currentUserRole);
        btnInvite.setVisible(isLeader);
        btnDeleteBoard.setVisible(isLeader);

        lblBoardTitle.setText(space.getNom());
        lblBoardType.setText(space.getCategory().toUpperCase() + " (" + (currentUserRole != null ? currentUserRole : "VIEWER") + ")");
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

            if (canManageTask(task)) {
                card.setOnDragDetected(event -> {
                    javafx.scene.input.Dragboard db = card.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                    javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                    content.putString(String.valueOf(task.getId()));
                    db.setContent(content);
                    event.consume();
                });
            }

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
                if (canTransitionTo(targetStatus)) {
                    event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                }
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

    private boolean canManageTask(Tache t) {
        if ("LEADER".equals(currentUserRole)) return true;
        return Session.isLoggedIn() && Session.getCurrentUser().getId() == t.getAssignedUserId();
    }

    private boolean canTransitionTo(StatutTache status) {
        if ("LEADER".equals(currentUserRole)) return true;
        return status != StatutTache.TERMINE;
    }

    private void updateTaskStatus(int taskId, StatutTache newStatus) {
        List<Tache> tasks = taskService.getTasksByBoard(currentBoard.getId());
        for (Tache t : tasks) {
            if (t.getId() == taskId) {
                t.setStatut(newStatus);
                taskService.update(t);
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
            controller.setContext(currentBoard.getId(), currentBoard.getLeaderId(), this);
            controller.setBoardContext(currentBoard, currentUserRole);
            controller.fillForm(task);

            MainLayoutController.getInstance().showPopup(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddTask(ActionEvent event) {
        if (!"LEADER".equals(currentUserRole)) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Task/task_form_popup.fxml"));
            Parent root = loader.load();
            TaskFormPopupController controller = loader.getController();
            controller.setContext(currentBoard.getId(), Session.getCurrentUser().getId(), this);
            controller.setBoardContext(currentBoard, currentUserRole);
            MainLayoutController.getInstance().showPopup(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleInviteMember(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Task/invite_member_popup.fxml"));
            Parent root = loader.load();
            InviteMemberController controller = loader.getController();
            controller.setTaskSpaceId(currentBoard.getId());
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED); // Optional, for clean UI
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteBoard(ActionEvent event) {
        if (!"LEADER".equals(currentUserRole)) return;
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le board ?");
        if (alert.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
            spaceService.delete(currentBoard.getId());
            handleBack(null);
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        MainLayoutController.getInstance().loadPage("board_hub.fxml");
    }
}
