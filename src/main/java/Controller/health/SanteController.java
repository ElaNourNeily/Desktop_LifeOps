package Controller.health;

import Model.health.BilanSante;
import Model.health.SuiviSante;
import controller.user.MainLayoutController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import service.health.BilanSanteService;
import service.health.SuiviSanteService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class SanteController {

    @FXML private VBox santeListContainer;
    @FXML private VBox bilanListContainer;
    @FXML private Button btnTabSuivi;
    @FXML private Button btnTabBilan;
    @FXML private HBox suiviFilterBar;
    @FXML private HBox bilanFilterBar;
    @FXML private Button btnFilterJour;
    @FXML private Button btnFilterMois;
    @FXML private Button btnFilterAnnee;
    @FXML private Button btnToggleBurnout;
    @FXML private Button btnActionPrimary;   // context-aware primary action button

    private final SuiviSanteService suiviService = new SuiviSanteService();
    private final BilanSanteService bilanService = new BilanSanteService();

    private List<SuiviSante> allSuivis;
    private List<BilanSante> allBilans;
    private String currentFilter = "Tout";
    private boolean burnoutOnly = false;
    private boolean onSuivisTab = true;

    @FXML
    public void initialize() {
        loadData();
        showSuivis(null);
    }

    private void loadData() {
        try {
            allSuivis = suiviService.recuperer();
            allBilans = bilanService.recuperer();
            applyFilters(null);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Filters ──────────────────────────────────────────────────────────

    @FXML void filterByJour(ActionEvent e)  { currentFilter = "Jour";  setActiveFilter(btnFilterJour);  applyFilters(null); }
    @FXML void filterByMois(ActionEvent e)  { currentFilter = "Mois";  setActiveFilter(btnFilterMois);  applyFilters(null); }
    @FXML void filterByAnnee(ActionEvent e) { currentFilter = "Année"; setActiveFilter(btnFilterAnnee); applyFilters(null); }

    private void setActiveFilter(Button active) {
        String base   = "-fx-background-color: #1c1e24; -fx-text-fill: #8a8d91; -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;";
        String active_ = "-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand; -fx-font-weight: bold;";
        btnFilterJour.setStyle(base); btnFilterMois.setStyle(base); btnFilterAnnee.setStyle(base);
        active.setStyle(active_);
    }

    @FXML void toggleBurnoutFilter(ActionEvent e) {
        burnoutOnly = !burnoutOnly;
        btnToggleBurnout.setStyle(burnoutOnly
            ? "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 6 14; -fx-cursor: hand; -fx-font-weight: bold;"
            : "-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-border-color: rgba(239,68,68,0.4); -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 6 14; -fx-cursor: hand;");
        applyFilters(null);
    }

    @FXML void applyFilters(ActionEvent e) {
        santeListContainer.getChildren().clear();
        LocalDate now = LocalDate.now();

        List<SuiviSante> filtered = allSuivis.stream().filter(s -> {
            if ("Jour".equals(currentFilter))  return s.getDate().equals(now);
            if ("Mois".equals(currentFilter))  return s.getDate().getMonth() == now.getMonth() && s.getDate().getYear() == now.getYear();
            if ("Année".equals(currentFilter)) return s.getDate().getYear() == now.getYear();
            return true;
        }).sorted((a, b) -> b.getDate().compareTo(a.getDate())).collect(Collectors.toList());

        if (filtered.isEmpty()) {
            santeListContainer.getChildren().add(emptyLabel("Aucun suivi trouvé. Cliquez sur \"+ Nouveau suivi\" pour commencer."));
        } else {
            filtered.forEach(s -> santeListContainer.getChildren().add(createSuiviCard(s)));
        }

        bilanListContainer.getChildren().clear();
        List<BilanSante> filteredBilans = allBilans.stream()
            .filter(b -> !burnoutOnly || b.isRisqueBurnout())
            .collect(Collectors.toList());

        if (filteredBilans.isEmpty()) {
            bilanListContainer.getChildren().add(emptyLabel("Aucun bilan trouvé. Cliquez sur \"🪄 Bilan IA\" pour générer votre premier bilan."));
        } else {
            filteredBilans.forEach(b -> bilanListContainer.getChildren().add(createBilanCard(b)));
        }
    }

    @FXML void resetFilters(ActionEvent e) {
        currentFilter = "Tout"; burnoutOnly = false;
        String base = "-fx-background-color: #1c1e24; -fx-text-fill: #8a8d91; -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;";
        btnFilterJour.setStyle(base); btnFilterMois.setStyle(base); btnFilterAnnee.setStyle(base);
        btnToggleBurnout.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-border-color: rgba(239,68,68,0.4); -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 6 14; -fx-cursor: hand;");
        applyFilters(null);
    }

    // ── Tabs ─────────────────────────────────────────────────────────────

    @FXML void showSuivis(ActionEvent e) {
        onSuivisTab = true;
        santeListContainer.setVisible(true);  santeListContainer.setManaged(true);
        bilanListContainer.setVisible(false); bilanListContainer.setManaged(false);
        suiviFilterBar.setVisible(true);  suiviFilterBar.setManaged(true);
        bilanFilterBar.setVisible(false); bilanFilterBar.setManaged(false);
        btnTabSuivi.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-weight: bold;");
        btnTabBilan.setStyle("-fx-background-color: #1c1e24; -fx-text-fill: #8a8d91; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
        // Switch action button to "Nouveau suivi"
        if (btnActionPrimary != null) {
            btnActionPrimary.setText("+ Nouveau suivi");
            btnActionPrimary.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 10; -fx-padding: 9 18; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(139,92,246,0.35), 8, 0, 0, 2);");
            btnActionPrimary.setOnAction(ev -> openCreate(ev));
        }
        applyFilters(null);
    }

    @FXML void showBilans(ActionEvent e) {
        onSuivisTab = false;
        santeListContainer.setVisible(false); santeListContainer.setManaged(false);
        bilanListContainer.setVisible(true);  bilanListContainer.setManaged(true);
        suiviFilterBar.setVisible(false); suiviFilterBar.setManaged(false);
        bilanFilterBar.setVisible(true);  bilanFilterBar.setManaged(true);
        btnTabBilan.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-weight: bold;");
        btnTabSuivi.setStyle("-fx-background-color: #1c1e24; -fx-text-fill: #8a8d91; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
        // Switch action button to "Bilan IA"
        if (btnActionPrimary != null) {
            btnActionPrimary.setText("🪄 Bilan IA");
            btnActionPrimary.setStyle("-fx-background-color: rgba(139,92,246,0.15); -fx-text-fill: #a78bfa; -fx-border-color: rgba(139,92,246,0.3); -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 9 18; -fx-font-weight: bold;");
            btnActionPrimary.setOnAction(ev -> openCreateBilan(ev));
        }
        applyFilters(null);
    }

    // ── Actions ───────────────────────────────────────────────────────────

    @FXML void openCreate(ActionEvent e) {
        CreateSanteController.openPopup(() -> { loadData(); showSuivis(null); });
    }

    @FXML void openCreateBilan(ActionEvent e) {
        CreateBilanController.openPopup(() -> { loadData(); showBilans(null); });
    }

    @FXML void openDrugSearch(ActionEvent e)     { loadView("/health/DrugSearch.fxml"); }
    @FXML void openHealthArticles(ActionEvent e) { loadView("/health/HealthArticles.fxml"); }
    @FXML void openDashboard(ActionEvent e)      { loadView("/health/DashboardSante.fxml"); }

    private void loadView(String path) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(path));
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) ctrl.loadContent(view);
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    // ── Cards ─────────────────────────────────────────────────────────────

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #6a6d71; -fx-font-style: italic; -fx-font-size: 13px; -fx-padding: 20 0;");
        l.setWrapText(true);
        return l;
    }

    private VBox createSuiviCard(SuiviSante s) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle("-fx-background-color: #1c1e24; -fx-background-radius: 12; " +
                "-fx-border-color: #2a2d32; -fx-border-width: 1; -fx-border-radius: 12; -fx-cursor: hand;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label("📅 " + (s.getDate() != null ? s.getDate().toString() : "—"));
        dateLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label humeurBadge = new Label("😊 Humeur " + s.getHumeur() + "/5");
        humeurBadge.setStyle("-fx-background-color: rgba(139,92,246,0.2); -fx-text-fill: #a78bfa; " +
                "-fx-background-radius: 20; -fx-padding: 4 12; -fx-font-size: 12px; -fx-font-weight: bold;");
        header.getChildren().addAll(dateLabel, spacer, humeurBadge);

        Label info = new Label(
            "🌙 " + s.getHeuresSommeil() + "h sommeil  •  " +
            "💧 " + s.getVerresEau() + " verres  •  " +
            "🏃 " + s.getMinutesActivite() + " min (" + (s.getActivite() != null ? s.getActivite() : "—") + ")  •  " +
            "⚖️ " + s.getPoids() + " kg");
        info.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 13px;");
        info.setWrapText(true);

        card.getChildren().addAll(header, info);
        if (s.getNotes() != null && !s.getNotes().isBlank()) {
            Label notes = new Label("📝 " + s.getNotes());
            notes.setStyle("-fx-text-fill: #6a6d71; -fx-font-size: 12px; -fx-font-style: italic;");
            notes.setWrapText(true);
            card.getChildren().add(notes);
        }

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #21232b; -fx-background-radius: 12; -fx-border-color: #8b5cf6; -fx-border-width: 1; -fx-border-radius: 12; -fx-cursor: hand;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color: #1c1e24; -fx-background-radius: 12; -fx-border-color: #2a2d32; -fx-border-width: 1; -fx-border-radius: 12; -fx-cursor: hand;"));
        card.setOnMouseClicked(e -> UpdateSanteController.openPopup(s, () -> { loadData(); showSuivis(null); }));
        return card;
    }

    private VBox createBilanCard(BilanSante b) {
        boolean burnout = b.isRisqueBurnout();
        VBox card = new VBox(10);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle("-fx-background-color: #1c1e24; -fx-background-radius: 12; " +
                "-fx-border-color: " + (burnout ? "rgba(239,68,68,0.4)" : "rgba(16,185,129,0.3)") +
                "; -fx-border-width: 1; -fx-border-radius: 12; -fx-cursor: hand;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label("📋 " + (b.getDateDebut() != null ? b.getDateDebut() : "?") +
                " → " + (b.getDateFin() != null ? b.getDateFin() : "?"));
        dateLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label(burnout ? "⚠️ RISQUE BURNOUT" : "✅ SANTÉ STABLE");
        badge.setStyle("-fx-background-color: " + (burnout ? "rgba(239,68,68,0.2); -fx-text-fill: #f87171;" : "rgba(16,185,129,0.2); -fx-text-fill: #34d399;") +
                " -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-size: 12px; -fx-font-weight: bold;");
        header.getChildren().addAll(dateLabel, spacer, badge);

        Label info = new Label("⚡ Fatigue " + b.getNiveauFatigue() + "/5  •  🧠 Stress " + b.getNiveauStress() + "/5  •  🏆 Score " + b.getScoreForme() + "/10");
        info.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 13px;");

        card.getChildren().addAll(header, info);
        if (b.getRecommandations() != null && !b.getRecommandations().isBlank()) {
            Label rec = new Label("💡 " + b.getRecommandations());
            rec.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 12px;");
            rec.setWrapText(true);
            card.getChildren().add(rec);
        }

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #21232b; -fx-background-radius: 12; -fx-border-color: " + (burnout ? "#ef4444" : "#10b981") + "; -fx-border-width: 1; -fx-border-radius: 12; -fx-cursor: hand;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color: #1c1e24; -fx-background-radius: 12; -fx-border-color: " + (burnout ? "rgba(239,68,68,0.4)" : "rgba(16,185,129,0.3)") + "; -fx-border-width: 1; -fx-border-radius: 12; -fx-cursor: hand;"));
        card.setOnMouseClicked(e -> UpdateBilanController.openPopup(b, () -> { loadData(); showBilans(null); }));
        return card;
    }
}
