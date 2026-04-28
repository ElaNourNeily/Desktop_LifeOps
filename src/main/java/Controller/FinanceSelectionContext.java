package Controller;

import model.Budget;
import model.Depense;

public final class FinanceSelectionContext {

    private static Budget selectedBudget;
    private static Depense selectedDepense;

    private FinanceSelectionContext() {
    }

    public static Budget getSelectedBudget() {
        return selectedBudget;
    }

    public static void setSelectedBudget(Budget budget) {
        selectedBudget = budget;
    }

    public static Depense getSelectedDepense() {
        return selectedDepense;
    }

    public static void setSelectedDepense(Depense depense) {
        selectedDepense = depense;
    }
}