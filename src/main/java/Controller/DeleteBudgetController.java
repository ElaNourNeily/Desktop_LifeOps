package controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.Budget;
import model.Depense;
import service.BudgetService;
import service.DepenseService;

import java.sql.SQLException;
import java.util.List;

public class DeleteBudgetController {

    @FXML
    private Label budgetInfoLabel;
    @FXML
    private Label messageLabel;

    private final BudgetService budgetService = new BudgetService();
    private final DepenseService depenseService = new DepenseService();
    private Budget selectedBudget;

    @FXML
    public void initialize() {
        selectedBudget = FinanceSelectionContext.getSelectedBudget();
        if (selectedBudget == null) {
            messageLabel.setText("Aucun budget selectionne.");
            return;
        }

        budgetInfoLabel.setText("Supprimer le budget " + selectedBudget.getMois() + " ?");
    }

    @FXML
    public void deleteBudget(Event event) {
        if (selectedBudget == null) {
            messageLabel.setText("Aucun budget selectionne.");
            return;
        }

        try {
            List<Depense> depenses = depenseService.recupererParBudget(selectedBudget.getId());
            for (Depense depense : depenses) {
                depenseService.supprimer(depense.getId());
            }
            budgetService.supprimer(selectedBudget.getId());
            FinanceSelectionContext.setSelectedBudget(null);
            ViewNavigator.navigate(event, "/finance/finance.fxml", "LifeOps - Finance");
        } catch (SQLException e) {
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    public void cancel(Event event) {
        ViewNavigator.navigate(event, "/finance/finance.fxml", "LifeOps - Finance");
    }
}
