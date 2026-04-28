package controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.Depense;
import service.DepenseService;

import java.sql.SQLException;

public class DeleteDepenseController {

    @FXML
    private Label depenseInfoLabel;
    @FXML
    private Label messageLabel;

    private final DepenseService depenseService = new DepenseService();
    private Depense selectedDepense;

    @FXML
    public void initialize() {
        selectedDepense = FinanceSelectionContext.getSelectedDepense();
        if (selectedDepense == null) {
            messageLabel.setText("Aucune depense selectionnee.");
            return;
        }

        depenseInfoLabel.setText("Supprimer la depense " + selectedDepense.getTitre() + " ?");
    }

    @FXML
    public void deleteDepense(Event event) {
        if (selectedDepense == null) {
            messageLabel.setText("Aucune depense selectionnee.");
            return;
        }

        try {
            depenseService.supprimer(selectedDepense.getId());
            FinanceSelectionContext.setSelectedDepense(null);
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
