package Controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Budget;
import model.Depense;
import service.BudgetService;
import service.DepenseService;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import service.SmsService;

public class UpdateDepenseController {

    private static final List<String> DEPENSE_CATEGORIES = List.of(
            "Bills",
            "Rent",
            "Alimentation",
            "Transport",
            "Sante",
            "Shopping",
            "Education",
            "Loisirs",
            "Abonnements",
            "Autre"
    );

    @FXML
    private Label depenseIdLabel;
    @FXML
    private TextField titreField;
    @FXML
    private TextField montantField;
    @FXML
    private ComboBox<String> categorieComboBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<String> typePaiementComboBox;
    @FXML
    private ComboBox<Budget> budgetComboBox;
    @FXML
    private Label messageLabel;

    private final DepenseService depenseService = new DepenseService();
    private final BudgetService budgetService = new BudgetService();
    private final SmsService smsService = new SmsService();
    private Depense selectedDepense;

    @FXML
    public void initialize() {
        selectedDepense = FinanceSelectionContext.getSelectedDepense();
        budgetComboBox.setCellFactory(param -> new BudgetListCell());
        budgetComboBox.setButtonCell(new BudgetListCell());
        categorieComboBox.getItems().setAll(DEPENSE_CATEGORIES);
        typePaiementComboBox.getItems().setAll("Card", "Cash");

        if (selectedDepense == null) {
            messageLabel.setText("Aucune depense selectionnee.");
            return;
        }

        try {
            List<Budget> budgets = budgetService.recupererParUtilisateur(selectedDepense.getUtilisateurId());
            budgetComboBox.getItems().setAll(budgets);
            budgets.stream()
                    .filter(budget -> budget.getId() == selectedDepense.getBudgetId())
                    .findFirst()
                    .ifPresent(budgetComboBox::setValue);
        } catch (SQLException e) {
            messageLabel.setText(e.getMessage());
        }

        depenseIdLabel.setText("Depense #" + selectedDepense.getId());
        titreField.setText(selectedDepense.getTitre());
        montantField.setText(String.valueOf(selectedDepense.getMontant()));
        categorieComboBox.setValue(selectedDepense.getCategorie());
        if (selectedDepense.getDate() != null) {
            datePicker.setValue(new Date(selectedDepense.getDate().getTime()).toLocalDate());
        }
        typePaiementComboBox.setValue(selectedDepense.getTypePaiement());
    }

    @FXML
    public void saveDepense(Event event) {
        if (selectedDepense == null) {
            messageLabel.setText("Aucune depense selectionnee.");
            return;
        }

        try {
            Budget selectedBudget = budgetComboBox.getValue();
            if (selectedBudget == null) {
                throw new IllegalArgumentException("Selectionnez un budget.");
            }

            LocalDate selectedDate = datePicker.getValue();
            if (selectedDate == null) {
                throw new IllegalArgumentException("Selectionnez une date.");
            }

            Depense updated = new Depense(
                    selectedDepense.getId(),
                    requiredText(titreField.getText(), "titre"),
                    parseDouble(montantField.getText(), "montant"),
                    requiredSelection(categorieComboBox.getValue(), "categorie"),
                    Date.valueOf(selectedDate),
                    requiredSelection(typePaiementComboBox.getValue(), "type de paiement"),
                    selectedDepense.getUtilisateurId(),
                    selectedBudget.getId(),
                    selectedDepense.isImportant(),
                    selectedDepense.getPhoneNumber()
            );

            depenseService.modifier(updated);

            // Send SMS if it is important AND the date is today
            if (updated.isImportant() && selectedDate.equals(LocalDate.now())) {
                String msg = "Alerte LifeOps: La dépense importante '" + updated.getTitre() + "' de " + updated.getMontant() + " TND a été mise à jour pour aujourd'hui.";
                smsService.sendSms(updated.getPhoneNumber(), msg);
                
                // Mark as sent so the scheduler doesn't send it again
                updated.setSmsSent(true);
                depenseService.modifier(updated);
            }

            FinanceSelectionContext.setSelectedDepense(updated);
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
            String raw = requiredText(value, fieldName);
            return utils.CurrencyUtils.parseAndConvert(raw, 0.0);
        } catch (Exception e) {
            throw new IllegalArgumentException("Valeur invalide pour " + fieldName + ".");
        }
    }

    private String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le champ " + fieldName + " est obligatoire.");
        }
        return value.trim();
    }

    private String requiredSelection(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Selectionnez " + fieldName + ".");
        }
        return value;
    }

}
