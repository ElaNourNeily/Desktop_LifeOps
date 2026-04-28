package controller;

import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Budget;
import model.Depense;
import service.BudgetService;
import service.CategorizationService;
import service.DepenseService;
import service.ReceiptData;
import service.ReceiptOcrService;
import service.ReceiptParser;

import java.io.File;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

public class AddDepenseController {

    private static final int CURRENT_UTILISATEUR_ID = 1;
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
    @FXML
    private Label currentUserLabel;
    @FXML
    private Button scanReceiptButton;

    private final DepenseService depenseService = new DepenseService();
    private final BudgetService budgetService = new BudgetService();
    private final ReceiptOcrService receiptOcrService = new ReceiptOcrService();
    private final ReceiptParser receiptParser = new ReceiptParser();
    private final CategorizationService categorizationService = new CategorizationService();
    private volatile boolean scanInProgress = false;

    @FXML
    public void initialize() {
        currentUserLabel.setText("Utilisateur courant : #" + CURRENT_UTILISATEUR_ID);
        categorieComboBox.setItems(FXCollections.observableArrayList(DEPENSE_CATEGORIES));
        categorieComboBox.setPromptText("Sélectionnez ou laissez vide pour auto-détection");
        typePaiementComboBox.setItems(FXCollections.observableArrayList("Card", "Cash"));
        budgetComboBox.setCellFactory(param -> new BudgetListCell());
        budgetComboBox.setButtonCell(new BudgetListCell());

        try {
            budgetComboBox.setItems(FXCollections.observableArrayList(
                    budgetService.recupererParUtilisateur(CURRENT_UTILISATEUR_ID)
            ));
        } catch (SQLException e) {
            messageLabel.setText(e.getMessage());
        }

        if (scanReceiptButton != null && !receiptOcrService.isNativeTesseractPresent()) {
            scanReceiptButton.setDisable(true);
            scanReceiptButton.setTooltip(new Tooltip(receiptOcrService.nativeInstallHintShort()));
        }
    }

    @FXML
    public void saveDepense(Event event) {
        try {
            Budget selectedBudget = budgetComboBox.getValue();
            if (selectedBudget == null) {
                throw new IllegalArgumentException("Selectionnez un budget.");
            }

            LocalDate selectedDate = datePicker.getValue();
            if (selectedDate == null) {
                throw new IllegalArgumentException("Selectionnez une date.");
            }

            String titre = requiredText(titreField.getText(), "titre");
            String categorie = categorieComboBox.getValue();

            // Auto-categorize if no category selected
            if (categorie == null || categorie.isBlank()) {
                CategorizationService.CategorizationResult result = categorizationService.categorizeExpense(titre);
                categorie = result.getCategoryName();
                messageLabel.setText("Catégorie auto-détectée: " + categorie + " (confiance: " + result.getConfidence() + "%)");
            }

            Depense depense = new Depense(
                    titre,
                    parseDouble(montantField.getText(), "montant"),
                    categorie,
                    Date.valueOf(selectedDate),
                    requiredSelection(typePaiementComboBox.getValue(), "type de paiement"),
                    CURRENT_UTILISATEUR_ID,
                    selectedBudget.getId()
            );

            depenseService.ajouter(depense);
            ViewNavigator.navigate(event, "/finance/finance.fxml", "LifeOps - Finance");
        } catch (IllegalArgumentException | SQLException e) {
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    public void cancel(Event event) {
        ViewNavigator.navigate(event, "/finance/finance.fxml", "LifeOps - Finance");
    }

    @FXML
    public void scanReceipt(Event event) {
        if (scanInProgress) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Receipt Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(
                    "Image Files",
                    "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.tiff", "*.tif", "*.heic", "*.heif"
            )
        );
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile == null) {
            return;
        }

        scanInProgress = true;
        setScanUiState(true, "Scan en cours (OCR) ...");

        Task<ReceiptData> task = new Task<>() {
            @Override
            protected ReceiptData call() throws Exception {
                String text = receiptOcrService.ocrReceipt(selectedFile);
                return receiptParser.parse(text);
            }
        };

        task.setOnSucceeded(e -> {
            ReceiptData data = task.getValue();
            applyReceiptDataToForm(data);
            setScanUiState(false, "Receipt scanne: champs remplis.");
            scanInProgress = false;
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex == null ? "Erreur OCR inconnue." : ex.getMessage();
            setScanUiState(false, "Erreur scan: " + msg);
            scanInProgress = false;
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void applyReceiptDataToForm(ReceiptData data) {
        if (data == null) {
            return;
        }

        Platform.runLater(() -> {
            if (data.title() != null && !data.title().isBlank()) {
                titreField.setText(data.title());
            }
            if (data.amount() != null) {
                montantField.setText(String.format(java.util.Locale.US, "%.2f", data.amount()));
            }
            if (data.date() != null) {
                datePicker.setValue(data.date());
            }
            if (data.paymentType() != null && !data.paymentType().isBlank()) {
                typePaiementComboBox.setValue(data.paymentType());
            }
            if (data.category() != null && !data.category().isBlank()) {
                categorieComboBox.setValue(data.category());
            }

            // Budget based on parsed receipt date.
            LocalDate d = datePicker.getValue();
            if (d != null) {
                Budget match = ReceiptParser.pickBudgetForDate(budgetComboBox.getItems(), d);
                if (match != null) {
                    budgetComboBox.setValue(match);
                }
            }
        });
    }

    private void setScanUiState(boolean scanning, String message) {
        Platform.runLater(() -> {
            messageLabel.setText(message == null ? "" : message);
            // Keep it simple: lock fields while scanning to avoid racey edits.
            titreField.setDisable(scanning);
            montantField.setDisable(scanning);
            categorieComboBox.setDisable(scanning);
            datePicker.setDisable(scanning);
            typePaiementComboBox.setDisable(scanning);
            budgetComboBox.setDisable(scanning);
            if (scanReceiptButton != null) {
                scanReceiptButton.setDisable(scanning);
            }
        });
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

    private String requiredSelection(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Selectionnez " + fieldName + ".");
        }
        return value;
    }
}