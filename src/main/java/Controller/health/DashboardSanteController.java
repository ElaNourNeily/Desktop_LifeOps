package Controller.health;

import Model.health.SuiviSante;
import Controller.user.MainLayoutController;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import service.health.PdfExportService;
import service.health.SuiviSanteService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardSanteController implements Initializable {

    // ── FXML bindings ─────────────────────────────────────────────────
    @FXML private HBox              kpiContainer;
    @FXML private LineChart<String, Number> lineChartEvol;
    @FXML private BarChart<String, Number>  barChartHabitudes;
    @FXML private VBox              radarChartContainer;
    @FXML private VBox              recommandationsContainer;
    @FXML private ProgressIndicator progressExport;
    @FXML private Button            btnExportPdf;
    @FXML private Label             lblDerniereMaj;

    private final SuiviSanteService suiviService = new SuiviSanteService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM");

    // ── Lifecycle ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerDonnees();
    }

    // ── Data loading ──────────────────────────────────────────────────

    private void chargerDonnees() {
        Task<List<SuiviSante>> task = new Task<>() {
            @Override
            protected List<SuiviSante> call() throws SQLException {
                return suiviService.recuperer();
            }
        };

        task.setOnSucceeded(e -> {
            List<SuiviSante> data = task.getValue();
            afficherKpis(data);
            afficherLineChart(data);
            afficherBarChart(data);
            afficherRadar(data);
            afficherRecommandations(data);
            lblDerniereMaj.setText("Maj: " + java.time.LocalTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });

        task.setOnFailed(e -> {
            System.err.println("DashboardSanteController: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    // ── KPI cards ─────────────────────────────────────────────────────

    private void afficherKpis(List<SuiviSante> data) {
        kpiContainer.getChildren().clear();
        kpiContainer.setSpacing(16);

        if (data.isEmpty()) {
            Label empty = new Label("Aucune donnée de santé disponible.");
            empty.setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic;");
            kpiContainer.getChildren().add(empty);
            return;
        }

        double avgSommeil  = data.stream().mapToDouble(s -> s.getHeuresSommeil()).average().orElse(0);
        double avgEau      = data.stream().mapToDouble(s -> s.getVerresEau()).average().orElse(0);
        double avgHumeur   = data.stream().mapToDouble(s -> s.getHumeur()).average().orElse(0);
        double avgActivite = data.stream().mapToDouble(s -> s.getMinutesActivite()).average().orElse(0);
        double scoreForme  = calculerScore(avgSommeil, (int)(avgEau), (int)(avgActivite), (int)(avgHumeur));

        String scoreColor = scoreForme >= 70 ? "#10b981" : scoreForme >= 40 ? "#f59e0b" : "#ef4444";

        kpiContainer.getChildren().addAll(
            creerKpi("⚡", String.format("%.0f pts", scoreForme), "Score de forme",   scoreColor),
            creerKpi("🌙", String.format("%.1f h",  avgSommeil),  "Sommeil moyen",    "#8b5cf6"),
            creerKpi("💧", String.format("%.0f",    avgEau),      "Verres d'eau/j",   "#3b82f6"),
            creerKpi("🏃", String.format("%.0f min",avgActivite), "Activité/j",       "#10b981"),
            creerKpi("😊", String.format("%.1f/5",  avgHumeur),   "Humeur moyenne",   "#a78bfa")
        );

        for (javafx.scene.Node n : kpiContainer.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
    }

    private VBox creerKpi(String emoji, String valeur, String label, String couleur) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle(
            "-fx-background-color: #111318;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + couleur + ";" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1.5;"
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        Label emojiLbl  = new Label(emoji);  emojiLbl.setStyle("-fx-font-size: 22px;");
        Label valeurLbl = new Label(valeur); valeurLbl.setStyle("-fx-text-fill: " + couleur + "; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label labelLbl  = new Label(label);  labelLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        card.getChildren().addAll(emojiLbl, valeurLbl, labelLbl);
        return card;
    }

    // ── Line chart ────────────────────────────────────────────────────

    private void afficherLineChart(List<SuiviSante> data) {
        lineChartEvol.getData().clear();
        lineChartEvol.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Score de forme");

        // Show last 14 entries, oldest first
        int start = Math.max(0, data.size() - 14);
        for (int i = data.size() - 1; i >= start; i--) {
            SuiviSante s = data.get(i);
            double score = calculerScore(s.getHeuresSommeil(), s.getVerresEau(),
                                         s.getMinutesActivite(), s.getHumeur());
            String dateLabel = s.getDate() != null ? s.getDate().format(FMT) : "?";
            series.getData().add(new XYChart.Data<>(dateLabel, score));
        }

        lineChartEvol.getData().add(series);
    }

    // ── Bar chart ─────────────────────────────────────────────────────

    private void afficherBarChart(List<SuiviSante> data) {
        barChartHabitudes.getData().clear();
        barChartHabitudes.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Heures de sommeil");

        int start = Math.max(0, data.size() - 10);
        for (int i = data.size() - 1; i >= start; i--) {
            SuiviSante s = data.get(i);
            String dateLabel = s.getDate() != null ? s.getDate().format(FMT) : "?";
            series.getData().add(new XYChart.Data<>(dateLabel, s.getHeuresSommeil()));
        }

        barChartHabitudes.getData().add(series);
    }

    // ── Radar chart (Canvas-based) ────────────────────────────────────

    private void afficherRadar(List<SuiviSante> data) {
        radarChartContainer.getChildren().clear();

        if (data.isEmpty()) return;

        // Compute averages normalised to 0–1
        double sommeil  = Math.min(1.0, data.stream().mapToDouble(s -> s.getHeuresSommeil()).average().orElse(0) / 9.0);
        double eau      = Math.min(1.0, data.stream().mapToDouble(s -> s.getVerresEau()).average().orElse(0) / 8.0);
        double activite = Math.min(1.0, data.stream().mapToDouble(s -> s.getMinutesActivite()).average().orElse(0) / 60.0);
        double humeur   = Math.min(1.0, data.stream().mapToDouble(s -> s.getHumeur()).average().orElse(0) / 5.0);
        double qualite  = Math.min(1.0, data.stream().mapToDouble(s -> s.getQualiteSommeil()).average().orElse(0) / 5.0);

        double[] values = { sommeil, eau, activite, humeur, qualite };
        String[] labels = { "Sommeil", "Hydratation", "Activité", "Humeur", "Qualité" };

        Canvas canvas = new Canvas(260, 260);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        dessinerRadar(gc, values, labels, 130, 130, 100);

        radarChartContainer.getChildren().add(canvas);
    }

    private void dessinerRadar(GraphicsContext gc, double[] values, String[] labels,
                                double cx, double cy, double r) {
        int n = values.length;
        double angleStep = 2 * Math.PI / n;

        // Background rings
        gc.setStroke(Color.web("#2a2d32"));
        gc.setLineWidth(1);
        for (int ring = 1; ring <= 4; ring++) {
            double rr = r * ring / 4.0;
            double[] px = new double[n], py = new double[n];
            for (int i = 0; i < n; i++) {
                double a = -Math.PI / 2 + i * angleStep;
                px[i] = cx + rr * Math.cos(a);
                py[i] = cy + rr * Math.sin(a);
            }
            gc.strokePolygon(px, py, n);
        }

        // Axes
        gc.setStroke(Color.web("#3a3d42"));
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2 + i * angleStep;
            gc.strokeLine(cx, cy, cx + r * Math.cos(a), cy + r * Math.sin(a));
        }

        // Data polygon
        double[] dx = new double[n], dy = new double[n];
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2 + i * angleStep;
            dx[i] = cx + r * values[i] * Math.cos(a);
            dy[i] = cy + r * values[i] * Math.sin(a);
        }
        gc.setFill(Color.web("#8b5cf6", 0.25));
        gc.fillPolygon(dx, dy, n);
        gc.setStroke(Color.web("#8b5cf6"));
        gc.setLineWidth(2);
        gc.strokePolygon(dx, dy, n);

        // Labels
        gc.setFill(Color.web("#9ca3af"));
        gc.setFont(javafx.scene.text.Font.font(11));
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2 + i * angleStep;
            double lx = cx + (r + 18) * Math.cos(a);
            double ly = cy + (r + 18) * Math.sin(a);
            gc.fillText(labels[i], lx - 20, ly + 4);
        }
    }

    // ── Recommandations ───────────────────────────────────────────────

    private void afficherRecommandations(List<SuiviSante> data) {
        recommandationsContainer.getChildren().clear();

        Label titre = new Label("💡 Recommandations personnalisées");
        titre.setStyle("-fx-text-fill: #f1f2f4; -fx-font-size: 15px; -fx-font-weight: bold;");
        VBox.setMargin(titre, new Insets(0, 0, 8, 0));
        recommandationsContainer.getChildren().add(titre);

        if (data.isEmpty()) {
            recommandationsContainer.getChildren().add(emptyLabel("Ajoutez des suivis santé pour obtenir des recommandations."));
            return;
        }

        double avgSommeil  = data.stream().mapToDouble(s -> s.getHeuresSommeil()).average().orElse(0);
        double avgEau      = data.stream().mapToDouble(s -> s.getVerresEau()).average().orElse(0);
        double avgActivite = data.stream().mapToDouble(s -> s.getMinutesActivite()).average().orElse(0);
        double avgHumeur   = data.stream().mapToDouble(s -> s.getHumeur()).average().orElse(0);

        if (avgSommeil < 7)
            recommandationsContainer.getChildren().add(creerRec("🌙", "Améliorez votre sommeil",
                "Vous dormez en moyenne " + String.format("%.1f", avgSommeil) + "h. Visez 7–9h par nuit pour optimiser votre récupération."));
        if (avgEau < 6)
            recommandationsContainer.getChildren().add(creerRec("💧", "Hydratez-vous davantage",
                "Vous buvez en moyenne " + String.format("%.0f", avgEau) + " verres/j. L'objectif recommandé est 8 verres."));
        if (avgActivite < 20)
            recommandationsContainer.getChildren().add(creerRec("🏃", "Bougez plus",
                "Seulement " + String.format("%.0f", avgActivite) + " min d'activité/j en moyenne. Essayez 30 min de marche quotidienne."));
        if (avgHumeur < 3)
            recommandationsContainer.getChildren().add(creerRec("😊", "Prenez soin de votre moral",
                "Votre humeur moyenne est de " + String.format("%.1f", avgHumeur) + "/5. Pensez à des activités qui vous font du bien."));

        if (recommandationsContainer.getChildren().size() == 1)
            recommandationsContainer.getChildren().add(creerRec("✅", "Excellent équilibre !",
                "Vos indicateurs de santé sont dans les normes recommandées. Continuez ainsi !"));
    }

    private VBox creerRec(String emoji, String titre, String conseil) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle(
            "-fx-background-color: rgba(139,92,246,0.07);" +
            "-fx-border-color: rgba(139,92,246,0.2);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;"
        );

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label emojiLbl = new Label(emoji); emojiLbl.setStyle("-fx-font-size: 18px;");
        Label titreLbl = new Label(titre); titreLbl.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 13px; -fx-font-weight: bold;");
        header.getChildren().addAll(emojiLbl, titreLbl);

        Label conseilLbl = new Label(conseil);
        conseilLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");
        conseilLbl.setWrapText(true);

        card.getChildren().addAll(header, conseilLbl);
        return card;
    }

    // ── PDF export ────────────────────────────────────────────────────

    @FXML
    void onExporterPdf(ActionEvent event) {
        // Ask user where to save
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le rapport PDF");
        chooser.setInitialFileName("rapport_sante_" +
                java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

        Window window = btnExportPdf.getScene().getWindow();
        File file = chooser.showSaveDialog(window);
        if (file == null) return; // user cancelled

        progressExport.setVisible(true);
        btnExportPdf.setDisable(true);

        String outputPath = file.getAbsolutePath();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Load fresh data for the report
                List<SuiviSante> data = suiviService.recuperer();
                new PdfExportService().generer(outputPath, data);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            progressExport.setVisible(false);
            btnExportPdf.setDisable(false);

            // Open the file with the system default PDF viewer
            try {
                java.awt.Desktop.getDesktop().open(new File(outputPath));
            } catch (Exception ex) {
                System.out.println("PDF généré : " + outputPath);
            }
        });

        task.setOnFailed(e -> {
            progressExport.setVisible(false);
            btnExportPdf.setDisable(false);
            Throwable ex = task.getException();
            System.err.println("Erreur export PDF : " + ex.getMessage());
            ex.printStackTrace();

            // Show error to user
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Erreur PDF");
                alert.setHeaderText("Impossible de générer le rapport");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            });
        });

        new Thread(task).start();
    }

    // ── Navigation ────────────────────────────────────────────────────

    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/health/Sante.fxml"));
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) ctrl.loadContent(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /**
     * Simple wellness score (0–100).
     */
    private double calculerScore(double sommeil, int eau, int activite, int humeur) {
        double s = Math.max(0, 25 - Math.abs(sommeil - 8) * 5);
        double e = Math.min(20, (eau      / 8.0)  * 20);
        double a = Math.min(20, (activite / 30.0) * 20);
        double h = (humeur / 5.0) * 15;
        double q = 20; // qualite_sommeil not available in aggregate call — neutral
        return Math.min(100, s + e + a + h + q);
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic; -fx-font-size: 13px;");
        l.setWrapText(true);
        return l;
    }
}
