package controller;

import enums.PrioriteTache;
import enums.StatutTache;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.task.Tache;
import service.TaskService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class TaskFormPopupController {

    @FXML private Label lblFormTitle;
    @FXML private Label lblError;
    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<PrioriteTache> comboPriority;
    @FXML private ComboBox<Integer> comboDifficulty;
    @FXML private ComboBox<StatutTache> comboStatus;
    @FXML private DatePicker dateDeadline;
    @FXML private Button btnSubmit;
    @FXML private Button btnDelete;

    private Tache editingTask;
    private int taskSpaceId;
    private int utilisateurId;
    private BoardViewController boardController;
    private final TaskService taskService = new TaskService();

    @FXML
    public void initialize() {
        // Populate ComboBoxes
        comboPriority.setItems(FXCollections.observableArrayList(PrioriteTache.values()));
        comboDifficulty.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        comboStatus.setItems(FXCollections.observableArrayList(StatutTache.values()));
        
        // Disable past dates for better UX
        setupDeadlinePicker();

        // Default values
        comboPriority.setValue(PrioriteTache.MOYENNE);
        comboDifficulty.setValue(1);
        comboStatus.setValue(StatutTache.A_FAIRE);
        dateDeadline.setValue(LocalDate.now());
    }

    private void setupDeadlinePicker() {
        dateDeadline.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #2a2d32; -fx-text-fill: #555555;"); 
                }
            }
        });
    }

    public void setContext(int taskSpaceId, int utilisateurId, BoardViewController boardController) {
        this.taskSpaceId = taskSpaceId;
        this.utilisateurId = utilisateurId;
        this.boardController = boardController;
    }

    public void fillForm(Tache task) {
        this.editingTask = task;
        this.taskSpaceId = task.getTaskSpaceId();
        this.utilisateurId = task.getUtilisateurId();

        lblFormTitle.setText("Modifier la Tâche");
        btnSubmit.setText("Mettre à jour");
        btnDelete.setVisible(true);

        txtTitle.setText(task.getTitre());
        txtDescription.setText(task.getDescription());
        comboPriority.setValue(task.getPriorite());
        comboDifficulty.setValue(task.getDifficulte());
        comboStatus.setValue(task.getStatut());
        
        if (task.getDeadline() != null) {
            LocalDate localDate = new java.sql.Timestamp(task.getDeadline().getTime()).toLocalDateTime().toLocalDate();
            dateDeadline.setValue(localDate);
        }
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        if (!validateForm()) return;

        Tache task = extractTaskFromForm();
        
        if (editingTask == null) {
            taskService.addTask(task);
        } else {
            task.setId(editingTask.getId());
            taskService.updateTask(task);
        }
        
        if (boardController != null) {
            boardController.loadTasks();
        }
        handleClose();
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (editingTask == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer cette tâche ?");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette tâche ? Cette action est irréversible.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            taskService.deleteTask(editingTask.getId());
            if (boardController != null) {
                boardController.loadTasks();
            }
            handleClose();
        }
    }

    private boolean validateForm() {
        boolean isValid = true;
        lblError.setVisible(false);
        resetStyles();

        if (txtTitle.getText() == null || txtTitle.getText().trim().isEmpty()) {
            txtTitle.getStyleClass().add("field-error");
            isValid = false;
        }
        if (comboPriority.getValue() == null) {
            comboPriority.getStyleClass().add("field-error");
            isValid = false;
        }
        if (dateDeadline.getValue() == null || dateDeadline.getValue().isBefore(LocalDate.now())) {
            dateDeadline.getStyleClass().add("field-error");
            isValid = false;
        }

        if (!isValid) {
            showError("Veuillez remplir correctement tous les champs obligatoires (*).");
        }
        return isValid;
    }

    private void resetStyles() {
        txtTitle.getStyleClass().remove("field-error");
        comboPriority.getStyleClass().remove("field-error");
        dateDeadline.getStyleClass().remove("field-error");
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }

    private Tache extractTaskFromForm() {
        Tache t = new Tache();
        t.setTitre(txtTitle.getText().trim());
        t.setDescription(txtDescription.getText() != null ? txtDescription.getText().trim() : "");
        t.setPriorite(comboPriority.getValue());
        t.setDifficulte(comboDifficulty.getValue() != null ? comboDifficulty.getValue() : 1);
        t.setStatut(comboStatus.getValue());
        
        if (dateDeadline.getValue() != null) {
            t.setDeadline(java.sql.Date.valueOf(dateDeadline.getValue()));
        }
        
        t.setTaskSpaceId(taskSpaceId);
        t.setUtilisateurId(utilisateurId);
        
        return t;
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) txtTitle.getScene().getWindow();
        stage.close();
    }
}
