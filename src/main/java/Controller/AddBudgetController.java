package controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Budget;
import service.BudgetService;

import java.sql.SQLException;

public class AddBudgetController {

    private static final int CURRENT_UTILISATEUR_ID = 1;

    @FXML
    private TextField revenuMensuelField;
    @FXML
    private TextField plafondField;
    @FXML
    private TextField moisField;
    @FXML
    private TextField economiesField;
    @FXML
    private Label messageLabel;
    @FXML
    private Label currentUserLabel;

    private final BudgetService budgetService = new BudgetService();

    @FXML
    public void initialize() {
        currentUserLabel.setText("Utilisateur courant : #" + CURRENT_UTILISATEUR_ID);
    }

    @FXML
    public void saveBudget(Event event) {
        try {
            Budget budget = new Budget(
                    parseDouble(revenuMensuelField.getText(), "revenu mensuel"),
                    parseDouble(plafondField.getText(), "plafond"),
                    requiredText(moisField.getText(), "mois"),
                    parseDouble(economiesField.getText(), "economies"),
                    CURRENT_UTILISATEUR_ID
            );

            budgetService.ajouter(budget);
            ViewNavigator.navigate(event, "/finance/finance.fxml", "LifeOps - Finance");
        } catch (IllegalArgumentException | SQLException e) {
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    public void cancel(Event event) {
        ViewNavigator.navigate(event, "/finance/finance.fxml", "LifeOps - Finance");
    }

    private double parseDouble(String value, String fieldName) {
        try {
            return Double.parseDouble(requiredText(value, fieldName));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valeur invalide pour " + fieldName + ".");
        }
    }

    private String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le champ " + fieldName + " est obligatoire.");
        }
        return value.trim();
    }
}
