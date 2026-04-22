package controller;

import enums.StatutTaskSpace;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.task.TaskSpace;
import service.TaskSpaceService;
import utils.Session;

import java.util.Date;

public class CreateBoardController {

    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtSprintDuration;
    @FXML private ComboBox<String> comboMode;
    @FXML private ComboBox<String> comboCategory;
    @FXML private Label lblError;

    private BoardHubController hubController;
    private final TaskSpaceService spaceService = new TaskSpaceService();

    @FXML
    public void initialize() {
        // Initialize ComboBoxes
        comboMode.setItems(FXCollections.observableArrayList("Solo", "Equipe"));
        comboCategory.setItems(FXCollections.observableArrayList(
                "Recherche", "Marketing", "Design", "Développement", "Autre"
        ));
        
        // Defaults
        comboMode.setValue("Solo");
        comboCategory.setValue("Développement");
        txtSprintDuration.setText("14");
    }

    public void setHubController(BoardHubController hubController) {
        this.hubController = hubController;
    }

    @FXML
    private void handleCreate(ActionEvent event) {
        try {
            if (!validateForm()) return;

            TaskSpace newSpace = new TaskSpace();
            newSpace.setNom(txtName.getText().trim());
            newSpace.setMode(comboMode.getValue());
            newSpace.setCategory(comboCategory.getValue());
            newSpace.setDescription(txtDescription.getText() != null ? txtDescription.getText().trim() : "");
            newSpace.setDuration(Integer.parseInt(txtSprintDuration.getText()));
            newSpace.setStatus(StatutTaskSpace.ACTIF);
            newSpace.setDateCreation(new Date());
            
            // Set both IDs from the current session user
            if (Session.isLoggedIn()) {
                int currentUserId = Session.getCurrentUser().getId();
                newSpace.setLeaderId(currentUserId);
                newSpace.setUtilisateurId(currentUserId);
            } else {
                // Fallback for testing/unauthenticated cases
                newSpace.setLeaderId(1);
                newSpace.setUtilisateurId(1);
            }

            spaceService.addTaskSpace(newSpace);
            
            if (hubController != null) {
                hubController.loadBoards();
            }
            handleCancel(null);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Une erreur est survenue lors de la création.");
        }
    }

    private boolean validateForm() {
        boolean isValid = true;
        lblError.setVisible(false);
        resetStyles();

        if (txtName.getText() == null || txtName.getText().trim().isEmpty()) {
            txtName.getStyleClass().add("field-error");
            isValid = false;
        }

        if (comboMode.getValue() == null) {
            comboMode.getStyleClass().add("field-error");
            isValid = false;
        }

        if (comboCategory.getValue() == null) {
            comboCategory.getStyleClass().add("field-error");
            isValid = false;
        }

        try {
            int duration = Integer.parseInt(txtSprintDuration.getText());
            if (duration <= 0) throw new Exception();
        } catch (Exception e) {
            txtSprintDuration.getStyleClass().add("field-error");
            isValid = false;
        }

        if (!isValid) {
            showError("Veuillez remplir correctement tous les champs obligatoires (*).");
        }

        return isValid;
    }

    private void resetStyles() {
        txtName.getStyleClass().remove("field-error");
        txtSprintDuration.getStyleClass().remove("field-error");
        comboMode.getStyleClass().remove("field-error");
        comboCategory.getStyleClass().remove("field-error");
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }
}
