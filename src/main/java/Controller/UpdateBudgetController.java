package controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Budget;
import service.BudgetService;

import java.sql.SQLException;

public class UpdateBudgetController {

    @FXML
    private Label budgetIdLabel;
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

    private final BudgetService budgetService = new BudgetService();
    private Budget selectedBudget;

    @FXML
    public void initialize() {
        selectedBudget = FinanceSelectionContext.getSelectedBudget();
        if (selectedBudget == null) {
            messageLabel.setText("Aucun budget selectionne.");
            return;
        }

        budgetIdLabel.setText("Budget #" + selectedBudget.getId());
        revenuMensuelField.setText(String.valueOf(selectedBudget.getRevenuMensuel()));
        plafondField.setText(String.valueOf(selectedBudget.getPlafond()));
        moisField.setText(selectedBudget.getMois());
        economiesField.setText(String.valueOf(selectedBudget.getEconomies()));
    }

    @FXML
    public void saveBudget(Event event) {
        if (selectedBudget == null) {
            messageLabel.setText("Aucun budget selectionne.");
            return;
        }

        try {
            Budget updated = new Budget(
                    selectedBudget.getId(),
                    parseDouble(revenuMensuelField.getText(), "revenu mensuel"),
                    parseDouble(plafondField.getText(), "plafond"),
                    requiredText(moisField.getText(), "mois"),
                    parseDouble(economiesField.getText(), "economies"),
                    selectedBudget.getUtilisateurId()
            );

            budgetService.modifier(updated);
            FinanceSelectionContext.setSelectedBudget(updated);
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
