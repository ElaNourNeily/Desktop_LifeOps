package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import model.Activite;
import model.Planning;
import service.ActiviteService;
import service.PlanningService;

import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class ActiviteManagementController {

    @FXML private TableView<Activite> activiteTable;
    @FXML private TableColumn<Activite, Integer> colId;
    @FXML private TableColumn<Activite, String> colTitre;
    @FXML private TableColumn<Activite, Integer> colDuree;
    @FXML private TableColumn<Activite, String> colEtat;
    @FXML private TableColumn<Activite, Integer> colPlanningId;

    @FXML private TextField txtTitre;
    @FXML private TextField txtDuree;
    @FXML private ComboBox<String> comboEtat;
    @FXML private TextField txtPriorite;
    @FXML private TextField txtCategorie;
    @FXML private ComboBox<Planning> comboPlanning;
    @FXML private TextField txtHeureDebut;
    @FXML private TextField txtHeureFin;
    @FXML private TextField txtSearch;

    @FXML private FlowPane colorPalette;
    @FXML private javafx.scene.shape.Rectangle selectedColorRect;
    @FXML private Label selectedColorLabel;

    private String selectedColor = "#3498db";
    private Rectangle currentSelected = null;

    private final ActiviteService service = new ActiviteService();
    private final PlanningService planningService = new PlanningService();
    private Activite selectedActivite = null;

    private static final String[] PALETTE_COLORS = {
        "#e74c3c", "#c0392b", "#e91e63", "#9c27b0", "#673ab7",
        "#3f51b5", "#2196f3", "#03a9f4", "#00bcd4", "#009688",
        "#4caf50", "#8bc34a", "#cddc39", "#ffeb3b", "#ffc107",
        "#ff9800", "#ff5722", "#795548", "#607d8b", "#2c3e50",
        "#1abc9c", "#27ae60", "#2980b9", "#8e44ad", "#d35400",
        "#f39c12", "#16a085", "#e67e22", "#34495e", "#7f8c8d"
    };

    private static final String ERROR_STYLE   = "-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6;";
    private static final String NORMAL_STYLE  = "";

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("duree"));
        colEtat.setCellValueFactory(new PropertyValueFactory<>("etat"));
        colPlanningId.setCellValueFactory(new PropertyValueFactory<>("planningId"));

        comboEtat.setItems(FXCollections.observableArrayList("en_attente", "en_cours", "termine"));
        refreshTable();
        loadPlannings();
        buildColorPalette();

        activiteTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) { selectedActivite = newVal; fillForm(newVal); }
        });

        // Clear error style when user types
        txtTitre.textProperty().addListener((o, ov, nv) -> clearError(txtTitre));
        txtDuree.textProperty().addListener((o, ov, nv) -> clearError(txtDuree));
        txtPriorite.textProperty().addListener((o, ov, nv) -> clearError(txtPriorite));
        txtCategorie.textProperty().addListener((o, ov, nv) -> clearError(txtCategorie));
        txtHeureDebut.textProperty().addListener((o, ov, nv) -> clearError(txtHeureDebut));
        txtHeureFin.textProperty().addListener((o, ov, nv) -> clearError(txtHeureFin));
        comboEtat.valueProperty().addListener((o, ov, nv) -> clearError(comboEtat));
        comboPlanning.valueProperty().addListener((o, ov, nv) -> clearError(comboPlanning));

        // Real-time search
        txtSearch.textProperty().addListener((o, ov, nv) -> handleSearch());
    }

    // ─── Validation ────────────────────────────────────────────────────────
    private List<String> validate() {
        List<String> errors = new ArrayList<>();
        resetAllErrors();

        // Titre: non vide, au moins 2 caractères
        if (txtTitre.getText().trim().isEmpty()) {
            markError(txtTitre);
            errors.add("• Le titre est obligatoire.");
        } else if (txtTitre.getText().trim().length() < 2) {
            markError(txtTitre);
            errors.add("• Le titre doit contenir au moins 2 caractères.");
        }

        // Durée: obligatoire, entier positif
        try {
            int duree = Integer.parseInt(txtDuree.getText().trim());
            if (duree <= 0) { markError(txtDuree); errors.add("• La durée doit être un entier positif (ex: 30)."); }
        } catch (NumberFormatException e) {
            markError(txtDuree);
            errors.add("• La durée doit être un nombre entier (ex: 60).");
        }

        // Priorité: obligatoire, entier entre 1 et 5
        try {
            int prio = Integer.parseInt(txtPriorite.getText().trim());
            if (prio < 1 || prio > 5) { markError(txtPriorite); errors.add("• La priorité doit être entre 1 et 5."); }
        } catch (NumberFormatException e) {
            markError(txtPriorite);
            errors.add("• La priorité doit être un nombre entier entre 1 et 5.");
        }

        // État: obligatoire
        if (comboEtat.getValue() == null) {
            markError(comboEtat);
            errors.add("• Veuillez sélectionner un état.");
        }

        // Heure début: format HH:mm:ss
        Time heureDebut = null;
        try {
            heureDebut = Time.valueOf(txtHeureDebut.getText().trim());
        } catch (Exception e) {
            markError(txtHeureDebut);
            errors.add("• Heure de début invalide. Format attendu: HH:mm:ss (ex: 09:00:00).");
        }

        // Heure fin: format HH:mm:ss + doit être après heureDebut
        Time heureFin = null;
        try {
            heureFin = Time.valueOf(txtHeureFin.getText().trim());
        } catch (Exception e) {
            markError(txtHeureFin);
            errors.add("• Heure de fin invalide. Format attendu: HH:mm:ss (ex: 10:00:00).");
        }

        if (heureDebut != null && heureFin != null && !heureFin.after(heureDebut)) {
            markError(txtHeureFin);
            errors.add("• L'heure de fin doit être après l'heure de début.");
        }

        // Catégorie: non vide
        if (txtCategorie.getText().trim().isEmpty()) {
            markError(txtCategorie);
            errors.add("• La catégorie est obligatoire.");
        }

        // Planning: obligatoire
        if (comboPlanning.getValue() == null) {
            markError(comboPlanning);
            errors.add("• Veuillez sélectionner un planning.");
        }

        return errors;
    }

    private void markError(Control field) {
        field.setStyle(ERROR_STYLE);
    }

    private void clearError(Control field) {
        field.setStyle(NORMAL_STYLE);
    }

    private void resetAllErrors() {
        for (Control c : new Control[]{txtTitre, txtDuree, txtPriorite, txtCategorie, txtHeureDebut, txtHeureFin, comboEtat, comboPlanning}) {
            c.setStyle(NORMAL_STYLE);
        }
    }

    // ─── Color Palette ──────────────────────────────────────────────────────
    private void buildColorPalette() {
        colorPalette.getChildren().clear();
        for (String hex : PALETTE_COLORS) {
            Rectangle swatch = new Rectangle(26, 26);
            swatch.setArcWidth(6); swatch.setArcHeight(6);
            swatch.setFill(Color.web(hex));
            swatch.setStroke(Color.TRANSPARENT);
            swatch.setStrokeWidth(3);
            swatch.setStyle("-fx-cursor: hand;");
            swatch.setOnMouseClicked(e -> selectColor(hex, swatch));
            swatch.setOnMouseEntered(e -> { if (swatch != currentSelected) { swatch.setScaleX(1.2); swatch.setScaleY(1.2); } });
            swatch.setOnMouseExited(e -> { if (swatch != currentSelected) { swatch.setScaleX(1.0); swatch.setScaleY(1.0); } });
            colorPalette.getChildren().add(swatch);
        }
        if (!colorPalette.getChildren().isEmpty()) {
            selectColor(PALETTE_COLORS[0], (Rectangle) colorPalette.getChildren().get(0));
        }
    }

    private void selectColor(String hex, Rectangle swatch) {
        if (currentSelected != null) {
            currentSelected.setStroke(Color.TRANSPARENT);
            currentSelected.setScaleX(1.0); currentSelected.setScaleY(1.0);
        }
        swatch.setStroke(Color.WHITE);
        swatch.setScaleX(1.2); swatch.setScaleY(1.2);
        currentSelected = swatch;
        selectedColor = hex;
        selectedColorRect.setFill(Color.web(hex));
        selectedColorLabel.setText(hex.toUpperCase());
    }

    // ─── DB ─────────────────────────────────────────────────────────────────
    private void loadPlannings() {
        try {
            comboPlanning.setItems(FXCollections.observableArrayList(planningService.recuperer()));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void refreshTable() {
        try {
            activiteTable.setItems(FXCollections.observableArrayList(service.recuperer()));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void fillForm(Activite a) {
        txtTitre.setText(a.getTitre());
        txtDuree.setText(String.valueOf(a.getDuree()));
        comboEtat.setValue(a.getEtat());
        txtPriorite.setText(String.valueOf(a.getPriorite()));
        txtCategorie.setText(a.getCategorie());
        for (Planning p : comboPlanning.getItems()) {
            if (p.getId() == a.getPlanningId()) { comboPlanning.setValue(p); break; }
        }
        txtHeureDebut.setText(a.getHeureDebutEstimee().toString());
        txtHeureFin.setText(a.getHeureFinEstimee().toString());
        String couleur = a.getCouleur();
        if (couleur != null) {
            selectedColor = couleur;
            selectedColorRect.setFill(Color.web(couleur));
            selectedColorLabel.setText(couleur.toUpperCase());
            for (int i = 0; i < PALETTE_COLORS.length; i++) {
                if (PALETTE_COLORS[i].equalsIgnoreCase(couleur)) {
                    selectColor(couleur, (Rectangle) colorPalette.getChildren().get(i)); break;
                }
            }
        }
    }

    // ─── CRUD Handlers ──────────────────────────────────────────────────────
    @FXML
    private void handleAdd() {
        List<String> errors = validate();
        if (!errors.isEmpty()) { showValidationError(errors); return; }

        try {
            Activite a = new Activite(
                txtTitre.getText().trim(),
                Integer.parseInt(txtDuree.getText().trim()),
                Integer.parseInt(txtPriorite.getText().trim()),
                comboEtat.getValue(),
                Time.valueOf(txtHeureDebut.getText().trim()),
                Time.valueOf(txtHeureFin.getText().trim()),
                "moyen",
                txtCategorie.getText().trim(),
                selectedColor,
                false,
                comboPlanning.getValue().getId()
            );
            service.ajouter(a);
            refreshTable();
            handleClear();
            showInfo("Succès", "✔ Activité ajoutée avec succès !");
        } catch (SQLException e) {
            showAlert("Erreur base de données", e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedActivite == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner une activité dans le tableau avant de modifier.");
            return;
        }
        List<String> errors = validate();
        if (!errors.isEmpty()) { showValidationError(errors); return; }

        try {
            selectedActivite.setTitre(txtTitre.getText().trim());
            selectedActivite.setDuree(Integer.parseInt(txtDuree.getText().trim()));
            selectedActivite.setEtat(comboEtat.getValue());
            selectedActivite.setPriorite(Integer.parseInt(txtPriorite.getText().trim()));
            selectedActivite.setCategorie(txtCategorie.getText().trim());
            selectedActivite.setPlanningId(comboPlanning.getValue().getId());
            selectedActivite.setHeureDebutEstimee(Time.valueOf(txtHeureDebut.getText().trim()));
            selectedActivite.setHeureFinEstimee(Time.valueOf(txtHeureFin.getText().trim()));
            selectedActivite.setCouleur(selectedColor);
            service.modifier(selectedActivite);
            refreshTable();
            handleClear();
            showInfo("Succès", "✔ Activité modifiée avec succès !");
        } catch (SQLException e) {
            showAlert("Erreur base de données", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedActivite == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner une activité dans le tableau pour la supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer \"" + selectedActivite.getTitre() + "\" ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    service.supprimer(selectedActivite.getId());
                    refreshTable(); handleClear();
                    showInfo("Succès", "✔ Activité supprimée !");
                } catch (SQLException e) {
                    showAlert("Erreur", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleClear() {
        txtTitre.clear(); txtDuree.clear(); txtPriorite.clear();
        txtCategorie.clear(); txtHeureDebut.clear(); txtHeureFin.clear();
        comboPlanning.setValue(null); comboEtat.setValue(null);
        selectedActivite = null;
        resetAllErrors();
        buildColorPalette();
    }

    @FXML
    private void handleSearch() {
        try {
            String keyword = txtSearch.getText().trim();
            if (keyword.isEmpty()) {
                refreshTable();
            } else {
                activiteTable.setItems(FXCollections.observableArrayList(service.rechercher(keyword)));
            }
        } catch (SQLException e) {
            showAlert("Erreur recherche", e.getMessage());
        }
    }

    @FXML private javafx.scene.control.Button btnSortPrio;
    @FXML private javafx.scene.control.Button btnSortTime;
    @FXML private javafx.scene.control.Button btnSortEtat;
    @FXML private javafx.scene.control.Button btnSortDuree;

    private void setActiveSortButton(javafx.scene.control.Button active) {
        for (javafx.scene.control.Button b : new javafx.scene.control.Button[]{btnSortPrio, btnSortTime, btnSortEtat, btnSortDuree}) {
            if (b == null) continue;
            b.getStyleClass().remove("action-button-active");
        }
        if (active != null) active.getStyleClass().add("action-button-active");
    }

    @FXML
    private void handleSortPriorite() {
        try {
            List<Activite> list = new java.util.ArrayList<>(service.recuperer());
            list.sort((a, b) -> Integer.compare(b.getPriorite(), a.getPriorite())); // desc
            activiteTable.setItems(javafx.collections.FXCollections.observableArrayList(list));
            setActiveSortButton(btnSortPrio);
        } catch (SQLException e) { showAlert("Erreur tri", e.getMessage()); }
    }

    @FXML
    private void handleSortHeure() {
        try {
            List<Activite> list = new java.util.ArrayList<>(service.recuperer());
            list.sort(java.util.Comparator.comparing(a -> a.getHeureDebutEstimee() != null ? a.getHeureDebutEstimee() : java.sql.Time.valueOf("00:00:00")));
            activiteTable.setItems(javafx.collections.FXCollections.observableArrayList(list));
            setActiveSortButton(btnSortTime);
        } catch (SQLException e) { showAlert("Erreur tri", e.getMessage()); }
    }

    @FXML
    private void handleSortEtat() {
        try {
            List<Activite> list = new java.util.ArrayList<>(service.recuperer());
            // Order: en_cours → en_attente → termine
            java.util.Map<String, Integer> order = new java.util.HashMap<>();
            order.put("en_cours", 0); order.put("en_attente", 1); order.put("termine", 2);
            list.sort((a, b) -> order.getOrDefault(a.getEtat(), 9) - order.getOrDefault(b.getEtat(), 9));
            activiteTable.setItems(javafx.collections.FXCollections.observableArrayList(list));
            setActiveSortButton(btnSortEtat);
        } catch (SQLException e) { showAlert("Erreur tri", e.getMessage()); }
    }

    @FXML
    private void handleSortDuree() {
        try {
            List<Activite> list = new java.util.ArrayList<>(service.recuperer());
            list.sort(java.util.Comparator.comparingInt(Activite::getDuree));
            activiteTable.setItems(javafx.collections.FXCollections.observableArrayList(list));
            setActiveSortButton(btnSortDuree);
        } catch (SQLException e) { showAlert("Erreur tri", e.getMessage()); }
    }

    @FXML
    private void handleResetSort() {
        refreshTable();
        setActiveSortButton(null);
    }

    @FXML
    private void handleSort() {
        handleSortPriorite(); // Keep backward compat
    }

    // ─── Alerts ─────────────────────────────────────────────────────────────
    private void showValidationError(List<String> errors) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Erreurs de saisie");
        alert.setHeaderText("Veuillez corriger les champs suivants :");
        alert.setContentText(String.join("\n", errors));
        alert.showAndWait();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(content); alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(content); alert.showAndWait();
    }
}
