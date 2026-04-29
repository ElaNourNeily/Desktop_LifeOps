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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pusher.client.channel.Channel;
import service.TaskService;
import service.TaskSpaceService;
import service.TaskSpaceUserService;
import service.TimeTrackingService;
import service.PusherService;
import service.TaskPriorityService;
import utils.Session;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @FXML private Button btnAISuggest;
    private Tache selectedTask;

    private TaskSpace currentBoard;
    private String currentUserRole;
    private final TaskService taskService = new TaskService();
    private final TaskSpaceService spaceService = new TaskSpaceService();
    private final TaskSpaceUserService spaceUserService = new TaskSpaceUserService();
    private final TimeTrackingService timeTrackingService = new TimeTrackingService();
    private final TaskPriorityService taskPriorityService = new TaskPriorityService();

    // Realtime
    private final PusherService pusherService = new PusherService();
    private final Gson gson = new Gson();
    private final Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
    private Channel boardChannel;
    private final Map<Integer, Parent> taskCardById = new HashMap<>();
    private final Map<Integer, Integer> taskRank = new HashMap<>();
    private final Map<Integer, Double> taskScore = new HashMap<>();
    private final Map<Integer, Boolean> taskRecommended = new HashMap<>();

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
        if (btnAISuggest != null) {
            btnAISuggest.setVisible(isLeader && space.isTeam());
        }

        lblBoardTitle.setText(space.getNom());
        lblBoardType.setText(space.getCategory().toUpperCase() + " (" + (currentUserRole != null ? currentUserRole : "VIEWER") + ")");
        lblBoardSubtitle.setText("Sprint de " + space.getDuration() + " jours");
        loadTasks();

        setupRealtimeIfTeam();
    }

    public void loadTasks() {
        colToDo.getChildren().clear();
        colInProgress.getChildren().clear();
        colReview.getChildren().clear();
        colDone.getChildren().clear();
        taskCardById.clear();
        taskRank.clear();
        taskScore.clear();
        taskRecommended.clear();

        List<Tache> tasks = taskService.getTasksByBoard(currentBoard.getId());
        List<TaskPriorityService.Recommendation> recommendations = taskPriorityService.getRecommendedTasks(tasks);
        for (int i = 0; i < recommendations.size(); i++) {
            TaskPriorityService.Recommendation rec = recommendations.get(i);
            int id = rec.getTask().getId();
            taskRank.put(id, i + 1);
            taskScore.put(id, rec.getScore());
            taskRecommended.put(id, i < 3);
        }

        List<Tache> todo = tasks.stream()
                .filter(t -> t.getStatut() == StatutTache.A_FAIRE)
                .sorted(Comparator.comparingDouble(this::scoreOf).reversed())
                .toList();
        List<Tache> inProgress = tasks.stream()
                .filter(t -> t.getStatut() == StatutTache.EN_COURS)
                .sorted(Comparator.comparingDouble(this::scoreOf).reversed())
                .toList();
        List<Tache> review = tasks.stream()
                .filter(t -> t.getStatut() == StatutTache.EN_REVISION)
                .sorted(Comparator.comparingDouble(this::scoreOf).reversed())
                .toList();
        List<Tache> done = tasks.stream()
                .filter(t -> t.getStatut() == StatutTache.TERMINE)
                .sorted(Comparator.comparingDouble(this::scoreOf).reversed())
                .toList();

        todo.forEach(this::addTaskToColumn);
        inProgress.forEach(this::addTaskToColumn);
        review.forEach(this::addTaskToColumn);
        done.forEach(this::addTaskToColumn);
    }

    private Parent addTaskToColumn(Tache task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Task/task_card.fxml"));
            Parent card = loader.load();
            
            TaskCardController controller = loader.getController();
            controller.setData(
                    task,
                    this,
                    taskRank.getOrDefault(task.getId(), -1),
                    scoreOf(task),
                    taskRecommended.getOrDefault(task.getId(), false)
            );

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
            taskCardById.put(task.getId(), card);
            return card;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private double scoreOf(Tache task) {
        return taskScore.getOrDefault(task.getId(), taskPriorityService.calculateScore(task));
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
                StatutTache oldStatus = t.getStatut();
                t.setStatut(newStatus);

                // Automatic real-time tracking integration
                if (newStatus == StatutTache.EN_COURS) {
                    timeTrackingService.startTimer(t);
                } else if (newStatus == StatutTache.TERMINE) {
                    timeTrackingService.stopTimer(t);
                } else {
                    taskService.update(t);
                }

                // realtime: task-moved
                if (pusherService.isEnabled() && oldStatus != newStatus) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("boardId", currentBoard.getId());
                    data.put("taskId", t.getId());
                    data.put("userId", Session.isLoggedIn() ? Session.getCurrentUser().getId() : null);
                    data.put("from", oldStatus != null ? oldStatus.getValeur() : null);
                    data.put("to", newStatus.getValeur());
                    pusherService.triggerEvent(pusherService.channelForBoard(currentBoard.getId()), "task-moved", data);
                }

                loadTasks();
                break;
            }
        }
    }

    private void setupRealtimeIfTeam() {
        if (currentBoard == null || !currentBoard.isTeam()) return;
        if (!Session.isLoggedIn()) return;

        // Validate membership before subscribing
        boolean isMember = spaceUserService.getUserRoleInBoard(Session.getCurrentUser().getId(), currentBoard.getId()) != null
                || Session.getCurrentUser().getId() == currentBoard.getLeaderId();
        if (!isMember) return;

        if (!pusherService.isEnabled()) return;

        String channelName = pusherService.channelForBoard(currentBoard.getId());

        // Cleanup previous
        if (boardChannel != null) {
            try { pusherService.unsubscribe(channelName); } catch (Exception ignored) {}
            boardChannel = null;
        }

        pusherService.connectIfNeeded(new com.pusher.client.connection.ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(com.pusher.client.connection.ConnectionStateChange change) {
                // no-op
            }

            @Override
            public void onError(String message, String code, Exception e) {
                // no-op (fallback: user can still manually refresh / actions still work)
            }
        });
        boardChannel = pusherService.subscribe(channelName, (event) -> {
            Map<String, Object> payload = gson.fromJson(event.getData(), mapType);
            if (payload == null) return;

            // ignore self events to prevent duplicates
            Object uid = payload.get("userId");
            if (uid != null && Session.isLoggedIn()) {
                try {
                    long sender = ((Number) uid).longValue();
                    if (sender == Session.getCurrentUser().getId()) return;
                } catch (Exception ignored) {}
            }

            String ev = event.getEventName();
            javafx.application.Platform.runLater(() -> applyRealtimeEvent(ev, payload));
        }, "task-created", "task-updated", "task-deleted", "task-moved", "member-invited", "board-updated");
    }

    private void applyRealtimeEvent(String ev, Map<String, Object> payload) {
        switch (ev) {
            case "task-created", "task-updated", "task-deleted", "task-moved", "board-updated" -> loadTasks();
            case "member-invited" -> { }
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

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            
            final double[] xOffset = {0};
            final double[] yOffset = {0};
            root.setOnMousePressed(e -> {
                xOffset[0] = e.getSceneX();
                yOffset[0] = e.getSceneY();
            });
            root.setOnMouseDragged(e -> {
                stage.setX(e.getScreenX() - xOffset[0]);
                stage.setY(e.getScreenY() - yOffset[0]);
            });

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
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
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            
            final double[] xOffset = {0};
            final double[] yOffset = {0};
            root.setOnMousePressed(e -> {
                xOffset[0] = e.getSceneX();
                yOffset[0] = e.getSceneY();
            });
            root.setOnMouseDragged(e -> {
                stage.setX(e.getScreenX() - xOffset[0]);
                stage.setY(e.getScreenY() - yOffset[0]);
            });

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
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
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT); // Optional, for clean UI
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
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
        if (currentBoard != null && pusherService.isEnabled()) {
            try { pusherService.unsubscribe(pusherService.channelForBoard(currentBoard.getId())); } catch (Exception ignored) {}
        }
        MainLayoutController.getInstance().loadPage("board_hub.fxml");
    }

    public void setSelectedTask(Tache task) {
        this.selectedTask = task;
    }

    @FXML
    private void handleAISuggestAssignment(ActionEvent event) {
        if (selectedTask == null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Veuillez d'abord sélectionner une tâche (clic simple sur la carte).");
            alert.show();
            return;
        }
        
        List<model.User> users = spaceUserService.getMembersByBoard(currentBoard.getId());
        // For a TEAM board, we should include the leader as well in case they are not in the members table
        if (users.stream().noneMatch(u -> u.getId() == currentBoard.getLeaderId())) {
            model.User leader = new service.UserService().getById(currentBoard.getLeaderId());
            if (leader != null) users.add(leader);
        }

        service.AIService aiService = new service.AIService();
        List<model.task.Recommendation> recommendations = aiService.getRecommendations(selectedTask, users);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Task/ai_assignment_popup.fxml"));
            Parent root = loader.load();
            AIAssignmentPopupController controller = loader.getController();
            controller.initData(selectedTask, recommendations, () -> {
                loadTasks(); // refresh UI after assignment
            });
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            
            // Allow dragging the undecorated popup if needed
            final double[] xOffset = {0};
            final double[] yOffset = {0};
            root.setOnMousePressed(e -> {
                xOffset[0] = e.getSceneX();
                yOffset[0] = e.getSceneY();
            });
            root.setOnMouseDragged(e -> {
                stage.setX(e.getScreenX() - xOffset[0]);
                stage.setY(e.getScreenY() - yOffset[0]);
            });

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
