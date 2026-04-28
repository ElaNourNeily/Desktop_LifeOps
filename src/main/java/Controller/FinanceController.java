package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Budget;
import model.Depense;
import service.BudgetService;
import service.DepenseService;
import service.ExpenseInsightsService;
import service.ExpenseInsightsService.PriorityResult;
import service.ExpenseInsightsService.RecurringCandidate;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FinanceController {

    private static final int CURRENT_UTILISATEUR_ID = 1;
    private static final String SORT_DEFAULT = "Revenu par défaut";
    private static final String SORT_BUDGET_ASC = "Revenu croissant";
    private static final String SORT_BUDGET_DESC = "Revenu decroissant";

    private static final String SORT_DEP_DEFAULT = "Montant par défaut";
    private static final String SORT_DEP_ASC = "Montant croissant";
    private static final String SORT_DEP_DESC = "Montant decroissant";

    @FXML
    private Label financeMessageLabel;
    @FXML
    private Label currentUserLabel;
    @FXML
    private VBox budgetCardsContainer;
    @FXML
    private TextField budgetSearchField;
    @FXML
    private ComboBox<String> sortBudgetComboBox;
    @FXML
    private TextField depenseSearchField;
    @FXML
    private ComboBox<String> sortDepenseComboBox;

    @FXML
    private VBox remindersBanner;
    @FXML
    private Label remindersKickerLabel;
    @FXML
    private Label remindersTitleLabel;
    @FXML
    private Label remindersDetailLabel;
    @FXML
    private Label remindersMetaLabel;

    @FXML
    private Label insightsSubtitleLabel;
    @FXML
    private VBox recurringBox;
    @FXML
    private Label recurringSummaryLabel;
    @FXML
    private VBox priorityBox;
    @FXML
    private Label prioritySummaryLabel;

    private final BudgetService budgetService = new BudgetService();
    private final DepenseService depenseService = new DepenseService();
    private final ExpenseInsightsService insightsService = new ExpenseInsightsService();
    private List<Budget> allBudgets = new ArrayList<>();
    private List<Depense> allDepenses = new ArrayList<>();
    private List<Depense> reminderDepensesToday = List.of();
    private List<Depense> reminderDepensesTomorrow = List.of();
    private boolean remindersDismissed = false;
    private boolean insightsDismissed = false;

    @FXML
    public void initialize() {
        currentUserLabel.setText("Utilisateur courant : #" + CURRENT_UTILISATEUR_ID);
        
        sortBudgetComboBox.getItems().setAll(SORT_DEFAULT, SORT_BUDGET_ASC, SORT_BUDGET_DESC);
        sortBudgetComboBox.setValue(SORT_DEFAULT);
        
        sortDepenseComboBox.getItems().setAll(SORT_DEP_DEFAULT, SORT_DEP_ASC, SORT_DEP_DESC);
        sortDepenseComboBox.setValue(SORT_DEP_DEFAULT);

        budgetSearchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        sortBudgetComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        
        depenseSearchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        sortDepenseComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        
        refreshData();
    }

    @FXML
    public void openTasks(javafx.event.Event event) {
        ViewNavigator.navigate(event, "/readtask.fxml", "LifeOps");
    }

    private void refreshData() {
        try {
            allBudgets = budgetService.recupererParUtilisateur(CURRENT_UTILISATEUR_ID);
            allDepenses = depenseService.recupererParUtilisateur(CURRENT_UTILISATEUR_ID);
            applyFilters();
            refreshRemindersBanner();
            refreshInsights();
        } catch (SQLException e) {
            financeMessageLabel.setText(e.getMessage());
        }
    }

    private void refreshInsights() {
        if (insightsDismissed) {
            if (insightsSubtitleLabel != null) insightsSubtitleLabel.setText("");
            if (recurringBox != null) {
                recurringBox.setVisible(false);
                recurringBox.setManaged(false);
            }
            if (priorityBox != null) {
                priorityBox.setVisible(false);
                priorityBox.setManaged(false);
            }
            return;
        }

        if (insightsSubtitleLabel != null) {
            insightsSubtitleLabel.setText("Detection automatique: abonnements probables + priorisation.");
        }

        // Recurring detection
        List<RecurringCandidate> recurring = insightsService.detectRecurring(allDepenses);
        if (recurringSummaryLabel != null) {
            if (recurring.isEmpty()) {
                recurringSummaryLabel.setText("Aucune recurrence fiable detectee (il faut en general 3 occurrences).");
            } else {
                StringBuilder sb = new StringBuilder();
                int take = Math.min(3, recurring.size());
                for (int i = 0; i < take; i++) {
                    RecurringCandidate c = recurring.get(i);
                    sb.append("- ")
                            .append(c.getLabel())
                            .append(" ~ ")
                            .append(String.format(Locale.US, "%.2f", c.getAmount()))
                            .append(" DT / ")
                            .append(c.getCadence())
                            .append(" (")
                            .append(c.getConfidence())
                            .append("%, prochaine: ")
                            .append(c.getNextExpectedDate())
                            .append(")\n");
                }
                recurringSummaryLabel.setText(sb.toString().trim());
            }
        }

        // Priority suggestion (current month only; stable ranking)
        YearMonth currentMonth = YearMonth.now();
        ZoneId zone = ZoneId.systemDefault();

        List<Depense> monthDepenses = allDepenses.stream()
                .filter(d -> d.getDate() != null)
                .filter(d -> {
                    LocalDate ld = Instant.ofEpochMilli(d.getDate().getTime()).atZone(zone).toLocalDate();
                    return YearMonth.from(ld).equals(currentMonth);
                })
                .toList();

        if (prioritySummaryLabel != null) {
            if (monthDepenses.isEmpty()) {
                prioritySummaryLabel.setText("Aucune depense pour " + currentMonth + ". Ajoutez des depenses pour voir des priorites proposees.");
                return;
            }

            // Rank: HIGH first, then MEDIUM, then LOW. Tie-break by amount desc, then date desc.
            List<Depense> ranked = monthDepenses.stream()
                    .sorted((a, b) -> {
                        PriorityResult pa = insightsService.prioritize(a);
                        PriorityResult pb = insightsService.prioritize(b);
                        int cmp = Integer.compare(priorityRank(pa.getPriority()), priorityRank(pb.getPriority()));
                        if (cmp != 0) return cmp;
                        cmp = Double.compare(b.getMontant(), a.getMontant());
                        if (cmp != 0) return cmp;
                        return b.getDate().compareTo(a.getDate());
                    })
                    .toList();

            StringBuilder sb = new StringBuilder();
            int take = Math.min(8, ranked.size());
            for (int i = 0; i < take; i++) {
                Depense d = ranked.get(i);
                PriorityResult pr = insightsService.prioritize(d);
                sb.append("- ")
                        .append(d.getTitre() == null ? "(sans titre)" : d.getTitre())
                        .append(" : ")
                        .append(String.format(Locale.US, "%.2f", d.getMontant()))
                        .append(" DT -> ")
                        .append(pr.getPriority())
                        .append(" (")
                        .append(pr.getReason())
                        .append(")\n");
            }
            prioritySummaryLabel.setText(sb.toString().trim());
        }
    }

    private int priorityRank(ExpenseInsightsService.Priority p) {
        if (p == null) return 2;
        return switch (p) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }

    @FXML
    public void dismissInsights() {
        insightsDismissed = true;
        refreshInsights();
    }

    private void refreshRemindersBanner() {
        if (remindersBanner == null) {
            return;
        }
        if (remindersDismissed) {
            remindersBanner.setVisible(false);
            remindersBanner.setManaged(false);
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        ZoneId zone = ZoneId.systemDefault();

        reminderDepensesToday = allDepenses.stream()
                .filter(d -> d.getDate() != null)
                .filter(d -> Instant.ofEpochMilli(d.getDate().getTime()).atZone(zone).toLocalDate().equals(today))
                .toList();

        reminderDepensesTomorrow = allDepenses.stream()
                .filter(d -> d.getDate() != null)
                .filter(d -> Instant.ofEpochMilli(d.getDate().getTime()).atZone(zone).toLocalDate().equals(tomorrow))
                .toList();

        int todayCount = reminderDepensesToday.size();
        int tomorrowCount = reminderDepensesTomorrow.size();
        boolean hasAny = todayCount > 0 || tomorrowCount > 0;

        remindersBanner.setVisible(hasAny);
        remindersBanner.setManaged(hasAny);
        if (!hasAny) {
            return;
        }

        if (remindersKickerLabel != null) {
            remindersKickerLabel.setText(todayCount > 0 ? "ALERTE 24H" : "RAPPEL 48H");
        }

        Depense primary = todayCount > 0 ? reminderDepensesToday.get(0) : reminderDepensesTomorrow.get(0);
        LocalDate primaryDate = todayCount > 0 ? today : tomorrow;
        String primaryTitle = primary.getTitre() == null || primary.getTitre().isBlank() ? "Depense" : primary.getTitre();

        if (remindersTitleLabel != null) {
            remindersTitleLabel.setText("Depense imminente: action conseillee.");
        }
        if (remindersDetailLabel != null) {
            remindersDetailLabel.setText("Rappel: " + primaryTitle + " est prevu le " + primaryDate + ".");
        }
        if (remindersMetaLabel != null) {
            int remaining = Math.max(0, (todayCount + tomorrowCount) - 1);
            remindersMetaLabel.setText("+" + remaining + " autre(s) rappel(s) en attente.  Urgent 24H: " + todayCount + " | Rappels 48H: " + tomorrowCount);
        }
    }

    @FXML
    public void markAllRemindersRead() {
        remindersDismissed = true;
        refreshRemindersBanner();
    }

    @FXML
    public void viewRemindersList() {
        int total = reminderDepensesToday.size() + reminderDepensesTomorrow.size();
        if (total == 0) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (!reminderDepensesToday.isEmpty()) {
            sb.append("Aujourd'hui:\n");
            for (Depense d : reminderDepensesToday) {
                sb.append("- ").append(d.getTitre() == null ? "(sans titre)" : d.getTitre())
                        .append(" : ").append(String.format(java.util.Locale.US, "%.2f", d.getMontant()))
                        .append(" DT\n");
            }
            sb.append("\n");
        }
        if (!reminderDepensesTomorrow.isEmpty()) {
            sb.append("Demain:\n");
            for (Depense d : reminderDepensesTomorrow) {
                sb.append("- ").append(d.getTitre() == null ? "(sans titre)" : d.getTitre())
                        .append(" : ").append(String.format(java.util.Locale.US, "%.2f", d.getMontant()))
                        .append(" DT\n");
            }
        }

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Rappels");
        alert.setHeaderText("Depenses a venir");
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    private void applyFilters() {
        // Filter and Sort Budgets
        List<Budget> filteredBudgets = new ArrayList<>(allBudgets);
        String budgetSearch = budgetSearchField == null ? "" : budgetSearchField.getText();
        String normalizedBudgetSearch = budgetSearch == null ? "" : budgetSearch.trim().toLowerCase(Locale.ROOT);

        if (!normalizedBudgetSearch.isEmpty()) {
            filteredBudgets = filteredBudgets.stream()
                    .filter(budget -> matchesBudgetSearch(budget, normalizedBudgetSearch))
                    .toList();
        }

        String selectedBudgetSort = sortBudgetComboBox == null ? SORT_DEFAULT : sortBudgetComboBox.getValue();
        if (SORT_BUDGET_ASC.equals(selectedBudgetSort)) {
            filteredBudgets = filteredBudgets.stream()
                    .sorted(Comparator.comparingDouble(Budget::getRevenuMensuel))
                    .toList();
        } else if (SORT_BUDGET_DESC.equals(selectedBudgetSort)) {
            filteredBudgets = filteredBudgets.stream()
                    .sorted(Comparator.comparingDouble(Budget::getRevenuMensuel).reversed())
                    .toList();
        }

        // Filter and Sort Depenses
        List<Depense> filteredDepenses = new ArrayList<>(allDepenses);
        String depenseSearch = depenseSearchField == null ? "" : depenseSearchField.getText();
        String normalizedDepenseSearch = depenseSearch == null ? "" : depenseSearch.trim().toLowerCase(Locale.ROOT);

        if (!normalizedDepenseSearch.isEmpty()) {
            filteredDepenses = filteredDepenses.stream()
                    .filter(depense -> matchesDepenseSearch(depense, normalizedDepenseSearch))
                    .toList();
        }

        String selectedDepenseSort = sortDepenseComboBox == null ? SORT_DEP_DEFAULT : sortDepenseComboBox.getValue();
        if (SORT_DEP_ASC.equals(selectedDepenseSort)) {
            filteredDepenses = filteredDepenses.stream()
                    .sorted(Comparator.comparingDouble(Depense::getMontant))
                    .toList();
        } else if (SORT_DEP_DESC.equals(selectedDepenseSort)) {
            filteredDepenses = filteredDepenses.stream()
                    .sorted(Comparator.comparingDouble(Depense::getMontant).reversed())
                    .toList();
        }

        // Only show budgets that have matching depenses (if depense search is active)
        if (!normalizedDepenseSearch.isEmpty()) {
            List<Integer> budgetIdsWithMatchingDepenses = filteredDepenses.stream()
                    .map(Depense::getBudgetId)
                    .distinct()
                    .toList();
            
            // If we are searching for depenses, we might want to see the budgets containing them
            // even if they don't match the budget search? 
            // The prompt says "search for budget by ... and for depense by ...".
            // I'll stick to: Budgets must match budget search AND must contain at least one matching depense (if depense search is active).
            filteredBudgets = filteredBudgets.stream()
                    .filter(b -> budgetIdsWithMatchingDepenses.contains(b.getId()))
                    .toList();
        }

        renderBudgetCards(filteredBudgets, filteredDepenses);
        financeMessageLabel.setText("Budgets: " + filteredBudgets.size() + " | Depenses filter: " + filteredDepenses.size());
    }

    private boolean matchesBudgetSearch(Budget budget, String normalizedSearch) {
        // Search by Mois (e.g. 2026 or 01-2026)
        if (budget.getMois() != null && budget.getMois().toLowerCase(Locale.ROOT).contains(normalizedSearch)) {
            return true;
        }

        // Search by Revenu Mensuel
        String revenuStr = String.valueOf((int) budget.getRevenuMensuel());
        return revenuStr.contains(normalizedSearch);
    }

    private boolean matchesDepenseSearch(Depense depense, String normalizedSearch) {
        if (depense.getTitre() != null && depense.getTitre().toLowerCase(Locale.ROOT).contains(normalizedSearch)) {
            return true;
        }
        if (depense.getCategorie() != null && depense.getCategorie().toLowerCase(Locale.ROOT).contains(normalizedSearch)) {
            return true;
        }
        if (depense.getTypePaiement() != null && depense.getTypePaiement().toLowerCase(Locale.ROOT).contains(normalizedSearch)) {
            return true;
        }
        return false;
    }

    private void renderBudgetCards(List<Budget> budgets, List<Depense> depenses) {
        budgetCardsContainer.getChildren().clear();

        if (budgets.isEmpty()) {
            Label emptyState = new Label("Aucun budget ne correspond a votre recherche.");
            emptyState.getStyleClass().add("page-subtitle");
            budgetCardsContainer.getChildren().add(emptyState);
            return;
        }

        for (Budget budget : budgets) {
            VBox card = new VBox(16);
            card.getStyleClass().add("finance-summary-card");

            HBox header = new HBox(12);
            Label title = new Label(budget.getMois());
            title.getStyleClass().add("finance-card-title");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label plafond = new Label("Plafond " + formatMoney(budget.getPlafond()));
            plafond.getStyleClass().add("finance-pill");
            Button editBudgetButton = createActionButton("Modifier", "action-button", event -> openUpdateBudget(event, budget));
            Button deleteBudgetButton = createActionButton("Supprimer", "finance-delete-button", event -> openDeleteBudget(event, budget));
            header.getChildren().addAll(title, spacer, plafond, editBudgetButton, deleteBudgetButton);

            HBox stats = new HBox(12);
            stats.getChildren().addAll(
                    createInfoChip("Revenu", formatMoney(budget.getRevenuMensuel())),
                    createInfoChip("Economies", formatMoney(budget.getEconomies())),
                    createInfoChip("Budget", "#" + budget.getId())
            );

            VBox depensesBox = new VBox(10);
            Label depenseSectionTitle = new Label("Depenses rattachees");
            depenseSectionTitle.getStyleClass().add("finance-section-label");
            List<Depense> depensesDuBudget = depenses.stream()
                    .filter(depense -> depense.getBudgetId() == budget.getId())
                    .toList();

            if (depensesDuBudget.isEmpty()) {
                Label noDepenses = new Label("Aucune depense pour ce budget.");
                noDepenses.getStyleClass().add("page-subtitle");
                depensesBox.getChildren().add(noDepenses);
            } else {
                for (Depense depense : depensesDuBudget) {
                    depensesBox.getChildren().add(createDepenseRow(depense));
                }
            }

            card.getChildren().addAll(header, stats, depenseSectionTitle, depensesBox);
            budgetCardsContainer.getChildren().add(card);
        }
    }

    private VBox createInfoChip(String label, String value) {
        VBox chip = new VBox(2);
        chip.getStyleClass().add("finance-info-chip");

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("finance-chip-label");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("finance-chip-value");

        chip.getChildren().addAll(labelNode, valueNode);
        return chip;
    }

    private HBox createDepenseRow(Depense depense) {
        HBox row = new HBox(12);
        row.getStyleClass().add("finance-depense-row");

        VBox left = new VBox(3);
        Label titre = new Label(depense.getTitre());
        titre.getStyleClass().add("finance-depense-title");
        Label details = new Label(depense.getCategorie() + " • " + depense.getTypePaiement());
        details.getStyleClass().add("page-subtitle");
        left.getChildren().addAll(titre, details);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox right = new VBox(3);
        Label montant = new Label(formatMoney(depense.getMontant()));
        montant.getStyleClass().add("finance-depense-amount");
        Label date = new Label("Date " + depense.getDate());
        date.getStyleClass().add("page-subtitle");
        right.getChildren().addAll(montant, date);

        Button editDepenseButton = createActionButton("Modifier", "action-button", event -> openUpdateDepense(event, depense));
        Button deleteDepenseButton = createActionButton("Supprimer", "finance-delete-button", event -> openDeleteDepense(event, depense));
        row.getChildren().addAll(left, spacer, right, editDepenseButton, deleteDepenseButton);
        return row;
    }

    private Button createActionButton(String text, String styleClass, javafx.event.EventHandler<ActionEvent> handler) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setOnAction(handler);
        return button;
    }

    private void openUpdateBudget(ActionEvent event, Budget budget) {
        FinanceSelectionContext.setSelectedBudget(budget);
        ViewNavigator.navigate(event, "/finance/update_budget.fxml", "LifeOps - Modifier Budget");
    }

    @FXML
    public void openAddBudget(ActionEvent event) {
        ViewNavigator.navigate(event, "/finance/add_budget.fxml", "LifeOps - Ajouter Budget");
    }

    private void openDeleteBudget(ActionEvent event, Budget budget) {
        FinanceSelectionContext.setSelectedBudget(budget);
        ViewNavigator.navigate(event, "/finance/delete_budget.fxml", "LifeOps - Supprimer Budget");
    }

    private void openUpdateDepense(ActionEvent event, Depense depense) {
        FinanceSelectionContext.setSelectedDepense(depense);
        ViewNavigator.navigate(event, "/finance/update_depense.fxml", "LifeOps - Modifier Depense");
    }

    @FXML
    public void openAddDepense(ActionEvent event) {
        ViewNavigator.navigate(event, "/finance/add_depense.fxml", "LifeOps - Ajouter Depense");
    }

    private void openDeleteDepense(ActionEvent event, Depense depense) {
        FinanceSelectionContext.setSelectedDepense(depense);
        ViewNavigator.navigate(event, "/finance/delete_depense.fxml", "LifeOps - Supprimer Depense");
    }

    private String formatMoney(double value) {
        return String.format("%.2f DT", value);
    }

}
