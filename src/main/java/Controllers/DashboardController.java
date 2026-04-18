package Controllers;

import Service.DashboardService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // ── KPI cards ────────────────────────────────────────────────────
    @FXML private VBox cardTotal;
    @FXML private VBox cardEnCours;
    @FXML private VBox cardTermines;
    @FXML private VBox cardEnRetard;
    @FXML private VBox cardPlans;
    @FXML private VBox cardProgression;

    // ── Charts ───────────────────────────────────────────────────────
    @FXML private PieChart pieCategorie;
    @FXML private PieChart pieStatut;
    @FXML private BarChart<String, Number> barProgression;
    @FXML private BarChart<String, Number> barPlans;

    private final DashboardService service = new DashboardService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerKPI();
        chargerPieCategorie();
        chargerPieStatut();
        chargerBarProgression();
        chargerBarPlans();
    }

    // ── KPI ──────────────────────────────────────────────────────────

    private void chargerKPI() {
        remplirCarte(cardTotal,       "📋 Total objectifs",       String.valueOf(service.totalObjectifs()),       "#8b5cf6");
        remplirCarte(cardEnCours,     "🔄 En cours",              String.valueOf(service.objectifsEnCours()),     "#6ea8fe");
        remplirCarte(cardTermines,    "✅ Complétés",             String.valueOf(service.objectifsTermines()),    "#00d285");
        remplirCarte(cardEnRetard,    "⚠ En retard",             String.valueOf(service.objectifsEnRetard()),    "#ff477e");
        remplirCarte(cardPlans,       "📌 Plans d'action",        String.valueOf(service.totalPlansAction()),     "#ffb703");
        remplirCarte(cardProgression, "📈 Progression moyenne",
                String.format("%.0f%%", service.progressionMoyenne()), "#a78bfa");
    }

    private void remplirCarte(VBox card, String titre, String valeur, String couleur) {
        card.getChildren().clear();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(8);
        card.setStyle(
            "-fx-background-color: #1c1e22;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + couleur + ";" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1.5;" +
            "-fx-padding: 20 25;"
        );
        card.setPrefWidth(260);
        card.setPrefHeight(110);

        Label lblValeur = new Label(valeur);
        lblValeur.setStyle("-fx-text-fill: " + couleur + "; -fx-font-size: 32px; -fx-font-weight: bold;");

        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 13px;");

        card.getChildren().addAll(lblValeur, lblTitre);
    }

    // ── Pie : catégories ─────────────────────────────────────────────

    private void chargerPieCategorie() {
        pieCategorie.getData().clear();
        pieCategorie.setLegendVisible(true);
        pieCategorie.setLabelsVisible(true);
        styleChart(pieCategorie);

        Map<String, Integer> data = service.objectifsParCategorie();
        if (data.isEmpty()) {
            pieCategorie.setData(FXCollections.observableArrayList(
                new PieChart.Data("Aucune donnée", 1)
            ));
            return;
        }
        data.forEach((cat, nb) ->
            pieCategorie.getData().add(new PieChart.Data(cat + " (" + nb + ")", nb))
        );
        appliquerCouleursPie(pieCategorie, new String[]{
            "#8b5cf6","#a78bfa","#6ea8fe","#00d285","#ffb703","#ff477e","#f472b6"
        });
    }

    // ── Pie : statuts ─────────────────────────────────────────────────

    private void chargerPieStatut() {
        pieStatut.getData().clear();
        pieStatut.setLegendVisible(true);
        pieStatut.setLabelsVisible(true);
        styleChart(pieStatut);

        Map<String, Integer> data = service.objectifsParStatut();
        if (data.isEmpty()) {
            pieStatut.setData(FXCollections.observableArrayList(
                new PieChart.Data("Aucune donnée", 1)
            ));
            return;
        }
        data.forEach((statut, nb) ->
            pieStatut.getData().add(new PieChart.Data(statut + " (" + nb + ")", nb))
        );
        appliquerCouleursPie(pieStatut, new String[]{
            "#6ea8fe","#00d285","#ff477e","#ffb703","#a78bfa","#f472b6","#8b5cf6"
        });
    }

    // ── Bar : progression par catégorie ──────────────────────────────

    private void chargerBarProgression() {
        barProgression.getData().clear();
        styleBarChart(barProgression);
        barProgression.setLegendVisible(false);

        Map<String, Double> data = service.progressionParCategorie();
        if (data.isEmpty()) return;

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Progression (%)");
        data.forEach((cat, moy) ->
            serie.getData().add(new XYChart.Data<>(cat, Math.round(moy)))
        );
        barProgression.getData().add(serie);

        // Colorer les barres en purple
        barProgression.setStyle(
            "CHART_COLOR_1: #8b5cf6;"
        );
    }

    // ── Bar : plans d'action par statut ──────────────────────────────

    private void chargerBarPlans() {
        barPlans.getData().clear();
        styleBarChart(barPlans);
        barPlans.setLegendVisible(false);

        Map<String, Integer> data = service.plansParStatut();
        if (data.isEmpty()) return;

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Plans");
        data.forEach((statut, nb) ->
            serie.getData().add(new XYChart.Data<>(statut, nb))
        );
        barPlans.getData().add(serie);

        barPlans.setStyle(
            "CHART_COLOR_1: #a78bfa;"
        );
    }

    // ── Helpers de style ─────────────────────────────────────────────

    private void styleChart(PieChart chart) {
        chart.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-pie-label-visible: true;"
        );
    }

    private void styleBarChart(BarChart<?, ?> chart) {
        chart.setStyle("-fx-background-color: transparent;");
        chart.lookup(".chart-plot-background")  ;
        chart.setHorizontalGridLinesVisible(false);
        chart.setVerticalGridLinesVisible(false);
    }

    private void appliquerCouleursPie(PieChart chart, String[] couleurs) {
        // Les couleurs sont appliquées via CSS inline sur chaque slice après rendu
        javafx.application.Platform.runLater(() -> {
            int i = 0;
            for (PieChart.Data d : chart.getData()) {
                if (d.getNode() != null && i < couleurs.length) {
                    d.getNode().setStyle("-fx-pie-color: " + couleurs[i % couleurs.length] + ";");
                }
                i++;
            }
        });
    }
}
