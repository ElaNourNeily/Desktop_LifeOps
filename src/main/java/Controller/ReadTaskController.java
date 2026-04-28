package controller;

import javafx.event.Event;

public class ReadTaskController {

    public void openFinance(Event event) {
        ViewNavigator.navigate(event, "/finance/finance.fxml", "LifeOps - Finance");
    }
}
