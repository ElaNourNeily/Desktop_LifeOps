package Controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.BilanSante;
import model.SuiviSante;
import service.BilanSanteService;
import service.HealthDataService;
import service.PdfExportService;
import service.SuiviSanteService;
import utils.ChartAnimator;
import utils.PdfChartRenderer;
import utils.RadarChart;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

public class DashboardController implements Initializable {

    @FXML private Label lblScoreGlobal;
    @FXML private Label lblBurnoutRisque;
    @FXML private Label lblDerniereMaj;
    @FXML private Button btnExportPdf;
    @FXML private ProgressIndicator progressExport;
    
    @FXML private LineChart<String, Number> lineChartEvol;
    @FXML private BarChart<String, Number> barChartHabitudes;
    @FXML private CategoryAxis xBarAxis;
    @FXML private NumberAxis yBarAxis;
    @FXML private VBox radarChartContainer;

    private RadarChart radarChart;
    private HealthDataService dataService;
    private Timeline refreshTimeline;
    private final PdfExportService exportService = new PdfExportService();
    private final PdfChartRenderer chartRenderer = new PdfChartRenderer();
    private final SuiviSanteService suiviSanteService = new SuiviSanteService();
    private final BilanSanteService bilanService = new BilanSanteService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataService = new HealthDataService();
        
        // Init Radar Chart
        radarChart = new RadarChart(300, 300);
        if (radarChartContainer != null) {
            radarChartContainer.getChildren().add(radarChart);
        }

        // Configuration des axes (Désactiver les animations pour éviter les chevauchements)
        if (lineChartEvol.getXAxis() instanceof CategoryAxis) {
            lineChartEvol.getXAxis().setAnimated(false);
        }
        if (barChartHabitudes.getXAxis() instanceof CategoryAxis) {
            barChartHabitudes.getXAxis().setAnimated(false);
        }

        loadData();

        // Auto-refresh 30s
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), ev -> {
            loadData();
            ChartAnimator.flashUpdate(lblDerniereMaj);
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void loadData() {
        Platform.runLater(() -> {
            List<HealthDataService.SuiviSanteDTO> suivis = dataService.getRecentData();
            
            if (suivis.isEmpty()) {
                lblScoreGlobal.setText("0");
                lblBurnoutRisque.setText("N/A");
                return;
            }

            // Mettre à jour KPIs
            HealthDataService.SuiviSanteDTO dernier = suivis.get(0); // Assuming 0 is the latest ? We should sort if needed, but let's take the first.
            ChartAnimator.animateCounter(lblScoreGlobal, dernier.scoreForme(), "pts");
            
            lblBurnoutRisque.setText(dernier.risqueBurnout());
            if ("ELEVE".equals(dernier.risqueBurnout()) || "CRITIQUE".equals(dernier.risqueBurnout())) {
                lblBurnoutRisque.setStyle("-fx-text-fill: red;");
                ChartAnimator.alertPulse(lblBurnoutRisque);
            } else {
                lblBurnoutRisque.setStyle("-fx-text-fill: green;");
            }
            
            lblDerniereMaj.setText("Maj: " + java.time.LocalTime.now().toString().substring(0, 8));

            updateLineChart(suivis);
            updateBarChart(suivis);
            updateRadarChart();
        });
    }

    private void updateLineChart(List<HealthDataService.SuiviSanteDTO> suivis) {
        lineChartEvol.getData().clear();
        
        XYChart.Series<String, Number> seriesScore = new XYChart.Series<>();
        seriesScore.setName("Score Forme");
        
        for (int i = suivis.size() - 1; i >= 0; i--) {
            HealthDataService.SuiviSanteDTO dto = suivis.get(i);
            seriesScore.getData().add(new XYChart.Data<>(dto.date(), dto.scoreForme()));
        }
        lineChartEvol.getData().add(seriesScore);
        
        Platform.runLater(() -> {
            lineChartEvol.layout();
            ChartAnimator.animateLineChartPoints(lineChartEvol);
        });
    }

    private void updateBarChart(List<HealthDataService.SuiviSanteDTO> suivis) {
        barChartHabitudes.getData().clear();
        
        XYChart.Series<String, Number> seriesSommeil = new XYChart.Series<>();
        seriesSommeil.setName("Heures Sommeil");
        
        for (int i = suivis.size() - 1; i >= 0; i--) {
            HealthDataService.SuiviSanteDTO dto = suivis.get(i);
            seriesSommeil.getData().add(new XYChart.Data<>(dto.date(), dto.heuresSommeil()));
        }
        barChartHabitudes.getData().add(seriesSommeil);
        
        Platform.runLater(() -> {
            barChartHabitudes.layout();
            ChartAnimator.animateBarChart(barChartHabitudes, 7.0);
        });
    }

    private void updateRadarChart() {
        double[] vals = new double[]{
            dataService.getMoyenneSommeil(),
            dataService.getMoyenneActivite(),
            dataService.getMoyenneEau(),
            dataService.getMoyenneHumeur()
        };
        radarChart.updateData(vals);
    }

    @FXML
    void goBack(ActionEvent event) {
        if(refreshTimeline != null) refreshTimeline.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReadSante.fxml"));
            Parent root = loader.load();
            Scene scene = ((Node) event.getSource()).getScene();
            scene.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onExporterPdf(ActionEvent event) {
        List<HealthDataService.SuiviSanteDTO> dtos = dataService.getRecentData();
        if (dtos.isEmpty()) {
            utils.AlertUtils.showError("Erreur", "Aucune donnée à exporter.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le rapport PDF");
        chooser.setInitialFileName("bilan_sante_ia.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        Stage stage = (Stage) lblScoreGlobal.getScene().getWindow();
        File fichier = chooser.showSaveDialog(stage);
        if (fichier == null) return;

        btnExportPdf.setDisable(true);
        progressExport.setVisible(true);

        // Récupérer le dernier bilan réel en BDD pour avoir les vraies recommandations IA
        BilanSante bilan = new BilanSante();
        try {
            List<BilanSante> tousLesBilans = bilanService.recuperer(); // On suppose que la liste est triée par date
            if (!tousLesBilans.isEmpty()) {
                bilan = tousLesBilans.get(tousLesBilans.size() - 1);
            } else {
                bilan.setRecommandations("Aucun bilan récent trouvé. Veuillez générer un bilan dans l'onglet Santé.");
            }
        } catch (SQLException e) {
            bilan.setRecommandations("Erreur lors de la récupération des recommandations.");
        }

        final byte[][] captures = new byte[3][];
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                captures[0] = chartRenderer.nodeToBytes(lineChartEvol, 520, 280);
                captures[1] = chartRenderer.nodeToBytes(barChartHabitudes, 520, 280);
                captures[2] = chartRenderer.canvasToBytes(radarChart);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        final BilanSante bilanPourExport = bilan;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                latch.await();
                List<SuiviSante> fullSuivis = suiviSanteService.recuperer();
                PdfExportService.ChartImages images = new PdfExportService.ChartImages(captures[0], captures[1], captures[2]);
                exportService.genererRapport(bilanPourExport, fullSuivis, images, fichier);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            btnExportPdf.setDisable(false);
            progressExport.setVisible(false);
            utils.AlertUtils.showSuccess("Export réussi", "Rapport PDF enregistré : " + fichier.getName());
        });

        task.setOnFailed(e -> {
            btnExportPdf.setDisable(false);
            progressExport.setVisible(false);
            utils.AlertUtils.showError("Erreur d'export", task.getException().getMessage());
        });

        new Thread(task).start();
    }
}
