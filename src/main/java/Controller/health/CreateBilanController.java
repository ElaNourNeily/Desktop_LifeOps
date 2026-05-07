package Controller.health;

import Model.health.BilanAnalyse;
import Model.health.BilanSante;
import Model.health.SuiviSante;
import Controller.user.MainLayoutController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import service.health.BilanSanteService;
import service.health.GeminiHealthService;
import service.health.SuiviSanteService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class CreateBilanController {

    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Label statusLabel;

    private final BilanSanteService service = new BilanSanteService();
    private final SuiviSanteService suiviService = new SuiviSanteService();
    private final GeminiHealthService aiService = new GeminiHealthService();
    private Runnable onSaved;

    /** Open this form as a modal popup. */
    public static void openPopup(Runnable onSaved) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    CreateBilanController.class.getResource("/health/CreateBilanPopup.fxml"));
            Parent root = loader.load();
            CreateBilanController ctrl = loader.getController();
            ctrl.onSaved = onSaved;

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Bilan IA");
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(
                    CreateBilanController.class.getResource("/Task/board_style.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void saveBilan(ActionEvent event) {
        if (dateDebutPicker.getValue() == null || dateFinPicker.getValue() == null) {
            showStatus("⚠ Veuillez sélectionner les deux dates.", false); return;
        }
        if (dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
            showStatus("⚠ La date de fin doit être après la date de début.", false); return;
        }

        List<SuiviSante> suivis = suiviService.findByPeriode(
                dateDebutPicker.getValue(), dateFinPicker.getValue());
        if (suivis.isEmpty()) {
            showStatus("⚠ Aucun suivi trouvé pour cette période.", false); return;
        }

        showStatus("⏳ Analyse IA en cours...", true);

        javafx.concurrent.Task<BilanAnalyse> task = new javafx.concurrent.Task<>() {
            @Override protected BilanAnalyse call() throws Exception {
                StringBuilder sb = new StringBuilder();
                for (SuiviSante s : suivis) {
                    sb.append("Date: ").append(s.getDate())
                      .append(", Sommeil: ").append(s.getHeuresSommeil())
                      .append(", Humeur: ").append(s.getHumeur()).append("/5")
                      .append(", Activité: ").append(s.getActivite()).append("\n");
                }
                return aiService.analyserDonnees(sb.toString());
            }
        };

        task.setOnSucceeded(e -> {
            BilanAnalyse analyse = task.getValue();
            try {
                BilanSante b = new BilanSante();
                b.setDateDebut(dateDebutPicker.getValue());
                b.setDateFin(dateFinPicker.getValue());
                b.setNiveauFatigue(Math.min(5, Math.max(1, (int) Math.ceil(analyse.getNiveauFatigue() / 2.0))));
                b.setNiveauStress(Math.min(5, Math.max(1, (int) Math.ceil(analyse.getNiveauStress() / 2.0))));
                b.setScoreForme(analyse.getScoreFormeGlobal() / 10.0f);
                b.setRisqueBurnout("ELEVE".equals(analyse.getRisqueBurnout()) || "CRITIQUE".equals(analyse.getRisqueBurnout()));
                String recs = analyse.getRecommandations() != null
                        ? String.join(", ", analyse.getRecommandations()) : "Aucune recommandation.";
                b.setRecommandations(recs);
                service.ajouter(b);
                showStatus("✓ Bilan généré et sauvegardé !", true);
                if (onSaved != null) onSaved.run();
                // Close after short delay
                new Thread(() -> {
                    try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(this::closeStage);
                }).start();
            } catch (SQLException ex) {
                showStatus("⚠ Erreur sauvegarde : " + ex.getMessage(), false);
            }
        });

        task.setOnFailed(e -> showStatus("⚠ Erreur IA : " + task.getException().getMessage(), false));
        new Thread(task).start();
    }

    @FXML
    void handleClose(ActionEvent event) {
        closeStage();
    }

    // Used by non-popup version
    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/health/Sante.fxml"));
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) ctrl.loadContent(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void closeStage() {
        if (dateDebutPicker != null && dateDebutPicker.getScene() != null) {
            ((Stage) dateDebutPicker.getScene().getWindow()).close();
        }
    }

    private void showStatus(String msg, boolean ok) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
            statusLabel.setStyle(ok
                ? "-fx-text-fill: #34d399; -fx-font-size: 12px; -fx-background-color: rgba(52,211,153,0.1); -fx-background-radius: 6; -fx-padding: 6 10;"
                : "-fx-text-fill: #f87171; -fx-font-size: 12px; -fx-background-color: rgba(239,68,68,0.1); -fx-background-radius: 6; -fx-padding: 6 10;");
            statusLabel.setVisible(true);
        }
    }
}
