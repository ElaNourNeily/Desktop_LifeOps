package controller;

import enums.PrioriteTache;
import enums.StatutTache;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import model.User;
import model.task.Tache;
import model.task.TaskSpace;
import service.TaskService;
import service.TaskSpaceUserService;
import utils.Session;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class TaskFormPopupController {

    @FXML private Label lblFormTitle;
    @FXML private Label lblError;
    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<PrioriteTache> comboPriority;
    @FXML private ComboBox<Integer> comboDifficulty;
    @FXML private ComboBox<StatutTache> comboStatus;
    @FXML private ComboBox<User> comboAssignee;
    @FXML private DatePicker dateDeadline;
    @FXML private Button btnSubmit;
    @FXML private Button btnDelete;

    private Tache editingTask;
    private int taskSpaceId;
    private int utilisateurId;
    private BoardViewController boardController;
    private final TaskService taskService = new TaskService();
    private final TaskSpaceUserService spaceUserService = new TaskSpaceUserService();

    @FXML
    public void initialize() {
        comboPriority.setItems(FXCollections.observableArrayList(PrioriteTache.values()));
        comboDifficulty.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        comboStatus.setItems(FXCollections.observableArrayList(StatutTache.values()));
        
        setupDeadlinePicker();
        setupAssigneeComboBox();

        // Defaults
        comboPriority.setValue(PrioriteTache.MOYENNE);
        comboDifficulty.setValue(1);
        comboStatus.setValue(StatutTache.A_FAIRE);
        dateDeadline.setValue(LocalDate.now());
    }

    private void setupAssigneeComboBox() {
        comboAssignee.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getFullName();
            }
            @Override
            public User fromString(String string) {
                return null;
            }
        });
    }

    public void setContext(int taskSpaceId, int utilisateurId, BoardViewController boardController) {
        this.taskSpaceId = taskSpaceId;
        this.utilisateurId = utilisateurId;
        this.boardController = boardController;
        
        // Load team members
        List<User> members = spaceUserService.getMembersByBoard(taskSpaceId);
        comboAssignee.setItems(FXCollections.observableArrayList(members));
    }

    public void setBoardContext(TaskSpace board, String currentUserRole) {
        // Permissions logic
        boolean isLeader = "LEADER".equals(currentUserRole);
        
        // If not leader, they can't assign tasks
        comboAssignee.setDisable(!isLeader);
        
        // If editing an existing task and not leader nor assignee, disable everything
        if (editingTask != null && !isLeader) {
            boolean isAssignee = Session.isLoggedIn() && Session.getCurrentUser().getId() == editingTask.getAssignedUserId();
            if (!isAssignee) {
                txtTitle.setDisable(true);
                txtDescription.setDisable(true);
                comboPriority.setDisable(true);
                comboDifficulty.setDisable(true);
                dateDeadline.setDisable(true);
                btnSubmit.setVisible(false);
                btnDelete.setVisible(false);
            } else {
                // Assignee can only change status
                txtTitle.setDisable(true);
                txtDescription.setDisable(true);
                comboPriority.setDisable(true);
                comboDifficulty.setDisable(true);
                dateDeadline.setDisable(true);
                
                // Restriction: Member cannot set to DONE
                comboStatus.setItems(FXCollections.observableArrayList(
                    StatutTache.A_FAIRE, StatutTache.EN_COURS, StatutTache.EN_REVISION
                ));
            }
        }
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

        if (task.getAssignedUserId() != null) {
            for (User u : comboAssignee.getItems()) {
                if (u.getId() == task.getAssignedUserId()) {
                    comboAssignee.setValue(u);
                    break;
                }
            }
        }
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        if (!validateForm()) return;

        Tache task = extractTaskFromForm();
        if (editingTask == null) {
            taskService.add(task);
        } else {
            task.setId(editingTask.getId());
            taskService.update(task);
        }
        
        if (boardController != null) {
            boardController.loadTasks();
        }
        handleClose();
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
        
        if (comboAssignee.getValue() != null) {
            t.setAssignedUserId(comboAssignee.getValue().getId());
        }
        
        return t;
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

        if (!isValid) {
            showError("Veuillez remplir correctement les champs obligatoires.");
        }
        return isValid;
    }

    private void resetStyles() {
        txtTitle.getStyleClass().remove("field-error");
        comboPriority.getStyleClass().remove("field-error");
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (editingTask == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la tâche ?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            taskService.delete(editingTask.getId());
            if (boardController != null) boardController.loadTasks();
            handleClose();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) txtTitle.getScene().getWindow();
        stage.close();
    }
}
