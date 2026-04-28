package Controller;

import javafx.scene.control.ListCell;
import model.Budget;

public class BudgetListCell extends ListCell<Budget> {

    @Override
    protected void updateItem(Budget item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
        } else {
            setText("Budget #" + item.getId() + " - " + item.getMois());
        }
    }
}