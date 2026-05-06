package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import java.io.IOException;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.SuiviSante;
import model.BilanSante;
import service.SuiviSanteService;
import service.BilanSanteService;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ReadSanteController {

    @FXML
    private VBox santeListContainer;
    @FXML
    private VBox bilanListContainer;
    @FXML
    private javafx.scene.control.Button btnTabSuivi;
    @FXML
    private javafx.scene.control.Button btnTabBilan;
    @FXML
    private HBox suiviFilterBar;
    @FXML
    private HBox bilanFilterBar;
    @FXML
    private javafx.scene.control.Button btnFilterJour;
    @FXML
    private javafx.scene.control.Button btnFilterMois;
    @FXML
    private javafx.scene.control.Button btnFilterAnnee;
    @FXML
    private javafx.scene.control.Button btnToggleBurnout;

    private final SuiviSanteService suiviService = new SuiviSanteService();
    private final BilanSanteService bilanService = new BilanSanteService();
    
    private List<SuiviSante> allSuivis;
    private List<BilanSante> allBilans;
    private String currentSuiviFilter = "Tout";
    private boolean burnoutOnly = false;

    @FXML
    public void initialize() {
        loadData();
    }

    @FXML
    void filterByJour(ActionEvent event) {
        currentSuiviFilter = "Jour";
        updateSegmentedButtons(btnFilterJour);
        applyFilters(null);
    }

    @FXML
    void filterByMois(ActionEvent event) {
        currentSuiviFilter = "Mois";
        updateSegmentedButtons(btnFilterMois);
        applyFilters(null);
    }

    @FXML
    void filterByAnnee(ActionEvent event) {
        currentSuiviFilter = "Année";
        updateSegmentedButtons(btnFilterAnnee);
        applyFilters(null);
    }

    private void updateSegmentedButtons(javafx.scene.control.Button active) {
        btnFilterJour.setStyle("-fx-background-color: #27272a; -fx-text-fill: #a1a1aa; -fx-background-radius: 10 0 0 10; -fx-padding: 8 15; -fx-cursor: hand;");
        btnFilterMois.setStyle("-fx-background-color: #27272a; -fx-text-fill: #a1a1aa; -fx-background-radius: 0; -fx-padding: 8 15; -fx-cursor: hand;");
        btnFilterAnnee.setStyle("-fx-background-color: #27272a; -fx-text-fill: #a1a1aa; -fx-background-radius: 0 10 10 0; -fx-padding: 8 15; -fx-cursor: hand;");
        
        active.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; " + 
                       (active == btnFilterJour ? "-fx-background-radius: 10 0 0 10;" : 
                        active == btnFilterAnnee ? "-fx-background-radius: 0 10 10 0;" : "-fx-background-radius: 0;") + 
                       "-fx-padding: 8 15; -fx-cursor: hand;");
    }

    @FXML
    void toggleBurnoutFilter(ActionEvent event) {
        burnoutOnly = !burnoutOnly;
        if (burnoutOnly) {
            btnToggleBurnout.setStyle("-fx-background-color: #ef4444; -fx-border-color: #ef4444; -fx-border-radius: 20; -fx-background-radius: 20; -fx-text-fill: white; -fx-cursor: hand;");
        } else {
            btnToggleBurnout.setStyle("-fx-background-color: #27272a; -fx-border-color: #ef4444; -fx-border-radius: 20; -fx-background-radius: 20; -fx-text-fill: #ef4444; -fx-cursor: hand;");
        }
        applyFilters(null);
    }

    private void loadData() {
        try {
            allSuivis = suiviService.recuperer();
            allBilans = bilanService.recuperer();
            applyFilters(null);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void applyFilters(ActionEvent event) {
        santeListContainer.getChildren().clear();

        java.time.LocalDate now = java.time.LocalDate.now();

        // 1. Filtrer les Suivis (Toujours trié par le plus récent)
        List<SuiviSante> filteredSuivis = allSuivis.stream()
            .filter(s -> {
                if (currentSuiviFilter.equals("Jour")) return s.getDate().equals(now);
                if (currentSuiviFilter.equals("Mois")) return s.getDate().getMonth() == now.getMonth() && s.getDate().getYear() == now.getYear();
                if (currentSuiviFilter.equals("Année")) return s.getDate().getYear() == now.getYear();
                return true;
            })
            .sorted((s1, s2) -> s2.getDate().compareTo(s1.getDate()))
            .collect(Collectors.toList());

        for (SuiviSante s : filteredSuivis) santeListContainer.getChildren().add(createSuiviCard(s));

        // 2. Filtrer les Bilans
        List<BilanSante> filteredBilans = allBilans.stream()
            .filter(b -> {
                if (burnoutOnly) return b.isRisqueBurnout();
                return true;
            })
            .sorted((b1, b2) -> b2.getDateDebut().compareTo(b1.getDateDebut()))
            .collect(Collectors.toList());

        for (BilanSante b : filteredBilans) bilanListContainer.getChildren().add(createBilanCard(b));
    }

    @FXML
    void resetFilters(ActionEvent event) {
        currentSuiviFilter = "Tout";
        burnoutOnly = false;
        
        btnFilterJour.setStyle("-fx-background-color: #27272a; -fx-text-fill: #a1a1aa; -fx-background-radius: 10 0 0 10; -fx-padding: 8 15; -fx-cursor: hand;");
        btnFilterMois.setStyle("-fx-background-color: #27272a; -fx-text-fill: #a1a1aa; -fx-background-radius: 0; -fx-padding: 8 15; -fx-cursor: hand;");
        btnFilterAnnee.setStyle("-fx-background-color: #27272a; -fx-text-fill: #a1a1aa; -fx-background-radius: 0 10 10 0; -fx-padding: 8 15; -fx-cursor: hand;");
        
        btnToggleBurnout.setStyle("-fx-background-color: #27272a; -fx-border-color: #ef4444; -fx-border-radius: 20; -fx-background-radius: 20; -fx-text-fill: #ef4444; -fx-cursor: hand;");
        
        applyFilters(null);
    }

    private VBox createSuiviCard(SuiviSante s) {
        VBox card = new VBox();
        card.setSpacing(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12; -fx-border-color: #8b5cf6; -fx-border-width: 0.5;");

        HBox header = new HBox();
        header.setSpacing(12);

        Label dateLabel = new Label(s.getDate() != null ? "📅 " + s.getDate().toString() : "Date inconnue");
        dateLabel.setStyle("-fx-text-fill: -fx-text-main; -fx-font-weight: bold; -fx-font-size: 15px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label humeurLabel = new Label("📈 HUMEUR: " + s.getHumeur() + "/10");
        humeurLabel.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-weight: bold;");

        header.getChildren().addAll(dateLabel, spacer, humeurLabel);

        Label infoLabel = new Label(
            "🌙 SOMMEIL : " + s.getHeuresSommeil() + "h (Qualité " + s.getQualiteSommeil() + "/5)\n" +
            "💧 HYDRATATION : " + s.getVerresEau() + " verres\n" +
            "🏃 ACTIVITÉ : " + s.getMinutesActivite() + " min (" + s.getActivite() + ")\n" +
            "⚖️ POIDS : " + s.getPoids() + " kg"
        );
        infoLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-line-spacing: 5;");

        Label notesLabel = new Label("Notes : " + s.getNotes());
        notesLabel.setStyle("-fx-text-fill: #a1a1aa; -fx-font-size: 12px; -fx-font-style: italic;");
        notesLabel.setWrapText(true);

        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> {
            try {
                UpdateSanteController.currentSuivi = s;
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/UpdateSante.fxml"));
                santeListContainer.getScene().setRoot(root);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        card.getChildren().addAll(header, infoLabel, notesLabel);
        return card;
    }

    private VBox createBilanCard(BilanSante b) {
        VBox card = new VBox();
        card.setSpacing(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12; -fx-border-color: #a78bfa; -fx-border-width: 1.5;");

        HBox header = new HBox();
        header.setSpacing(12);

        String datesStr = "📋 Bilan du " + (b.getDateDebut() != null ? b.getDateDebut().toString() : "?") + 
                          " au " + (b.getDateFin() != null ? b.getDateFin().toString() : "?");
        Label dateLabel = new Label(datesStr);
        dateLabel.setStyle("-fx-text-fill: #c4b5fd; -fx-font-weight: bold; -fx-font-size: 15px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label burnoutLabel = new Label(b.isRisqueBurnout() ? "⚠️ RISQUE BURNOUT" : "✅ SANTÉ STABLE");
        String bgColor = b.isRisqueBurnout() ? "#ef4444" : "#10b981";
        burnoutLabel.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-weight: bold;");

        header.getChildren().addAll(dateLabel, spacer, burnoutLabel);

        Label infoLabel = new Label(
            "⚡ NIVEAU FATIGUE : " + b.getNiveauFatigue() + "/5\n" +
            "🧠 NIVEAU STRESS : " + b.getNiveauStress() + "/5\n" +
            "🏆 SCORE GLOBAL : " + b.getScoreForme() + "/10"
        );
        infoLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-line-spacing: 5;");

        Label recLabel = new Label("Recommandations : " + b.getRecommandations());
        recLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 12px; -fx-font-weight: bold;");
        recLabel.setWrapText(true);

        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> {
            try {
                UpdateBilanController.currentBilan = b;
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/UpdateBilan.fxml"));
                santeListContainer.getScene().setRoot(root);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        card.getChildren().addAll(header, infoLabel, recLabel);
        return card;
    }

    @FXML
    void openCreate(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/CreateSante.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
            scene.setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openCreateBilan(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/CreateBilan.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
            scene.setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    /** Resolves an FXML resource URL, trying both the class and the classloader. */
    private java.net.URL resolveFxml(String name) {
        java.net.URL url = getClass().getResource(name);
        if (url == null) url = getClass().getClassLoader().getResource(name.replaceFirst("^/", ""));
        if (url == null) throw new RuntimeException("FXML introuvable sur le classpath : " + name
            + "\nVérifiez que le projet a été compilé (Build → Rebuild Project).");
        return url;
    }

    @FXML
    void openDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(resolveFxml("/DashboardSante.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur chargement DashboardSante.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void openDrugSearch(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(resolveFxml("/DrugSearch.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur chargement DrugSearch.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void openHealthArticles(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(resolveFxml("/HealthArticles.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de HealthArticles.fxml : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void showSuivis(ActionEvent event) {
        santeListContainer.setVisible(true);
        santeListContainer.setManaged(true);
        bilanListContainer.setVisible(false);
        bilanListContainer.setManaged(false);
        
        suiviFilterBar.setVisible(true);
        suiviFilterBar.setManaged(true);
        bilanFilterBar.setVisible(false);
        bilanFilterBar.setManaged(false);

        btnTabSuivi.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-background-radius: 5px;");
        btnTabBilan.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-background-radius: 5px;");
        
        applyFilters(null);
    }

    @FXML
    void showBilans(ActionEvent event) {
        santeListContainer.setVisible(false);
        santeListContainer.setManaged(false);
        bilanListContainer.setVisible(true);
        bilanListContainer.setManaged(true);
        
        suiviFilterBar.setVisible(false);
        suiviFilterBar.setManaged(false);
        bilanFilterBar.setVisible(true);
        bilanFilterBar.setManaged(true);

        btnTabBilan.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-background-radius: 5px;");
        btnTabSuivi.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-background-radius: 5px;");
        
        applyFilters(null);
    }
}
