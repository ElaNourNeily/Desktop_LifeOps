package controller;

import enums.PrioriteTache;
import enums.StatutTache;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import model.task.Tache;
import service.TaskService;
import service.TimeTrackingService;

public class TaskDetailsController {

    @FXML private VBox detailsPanelRoot;
    @FXML private TextField txtTaskTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<StatutTache> comboStatus;
    @FXML private ComboBox<PrioriteTache> comboPriority;
    @FXML private ComboBox<Integer> comboDifficulty;
    @FXML private TextField txtEstimatedTime;
    @FXML private TextField txtRealTime;

    private Tache currentTask;
    private BoardViewController boardController;
    private final TaskService taskService = new TaskService();
    private final TimeTrackingService timeTrackingService = new TimeTrackingService();

    @FXML
    public void initialize() {
        comboStatus.setItems(FXCollections.observableArrayList(StatutTache.values()));
        comboPriority.setItems(FXCollections.observableArrayList(PrioriteTache.values()));
        comboDifficulty.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        
        // Auto-save on change (UX improvement)
        comboStatus.setOnAction(e -> handleUpdate());
        comboPriority.setOnAction(e -> handleUpdate());
        comboDifficulty.setOnAction(e -> handleUpdate());
    }

    public void setTask(Tache task, BoardViewController boardController) {
        this.currentTask = task;
        this.boardController = boardController;

        txtTaskTitle.setText(task.getTitre());
        txtDescription.setText(task.getDescription());
        comboStatus.setValue(task.getStatut());
        comboPriority.setValue(task.getPriorite());
        comboDifficulty.setValue(task.getDifficulte() > 0 ? task.getDifficulte() : 1);
        
        // Listener for title/description to save when focus is lost or changed
        txtTaskTitle.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) handleUpdate();
        });
        txtDescription.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) handleUpdate();
        });
    }

    @FXML
    private void handleUpdate() {
        if (currentTask == null) return;

        currentTask.setTitre(txtTaskTitle.getText());
        currentTask.setDescription(txtDescription.getText());
        
        StatutTache oldStatus = currentTask.getStatut();
        StatutTache status = comboStatus.getValue();
        StatutTache newStatus = status != null ? status : StatutTache.A_FAIRE;
        currentTask.setStatut(newStatus);
        
        PrioriteTache priority = comboPriority.getValue();
        currentTask.setPriorite(priority != null ? priority : PrioriteTache.MOYENNE);
        
        Integer difficulty = comboDifficulty.getValue();
        currentTask.setDifficulte(difficulty != null ? difficulty : 1);

        if (currentTask.getId() == 0) {
            taskService.add(currentTask);
        } else {
            // Automatic real-time tracking integration on status transition
            if (oldStatus != newStatus) {
                if (newStatus == StatutTache.EN_COURS) {
                    timeTrackingService.startTimer(currentTask);
                } else if (newStatus == StatutTache.TERMINE) {
                    timeTrackingService.stopTimer(currentTask);
                } else {
                    taskService.update(currentTask);
                }
            } else {
                taskService.update(currentTask);
            }
        }
        
        boardController.loadTasks(); // Refresh board
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (currentTask != null && currentTask.getId() != 0) {
            taskService.delete(currentTask.getId());
            boardController.loadTasks();
            handleClose();
        }
    }

    @FXML
    private void handleClose() {
        detailsPanelRoot.setVisible(false);
    }
}
