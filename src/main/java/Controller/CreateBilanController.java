package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import model.BilanSante;
import service.BilanSanteService;
import service.GeminiAnalysisService;
import service.SuiviSanteService;

import java.sql.SQLException;

public class CreateBilanController {

    @FXML
    private DatePicker dateDebutPicker;
    @FXML
    private DatePicker dateFinPicker;

    private final BilanSanteService service = new BilanSanteService();
    private final SuiviSanteService suiviService = new SuiviSanteService();
    private final GeminiAnalysisService aiService = new GeminiAnalysisService();

    @FXML
    void saveBilan(ActionEvent event) {
        if (!isInputValid()) return;

        java.util.List<model.SuiviSante> suivis = suiviService.findByPeriode(dateDebutPicker.getValue(), dateFinPicker.getValue());

        if (suivis.isEmpty()) {
            utils.AlertUtils.showError("Erreur", "Aucun suivi trouvé pour cette période.");
            return;
        }

        javafx.concurrent.Task<model.BilanAnalyse> task = new javafx.concurrent.Task<>() {
            @Override
            protected model.BilanAnalyse call() throws Exception {
                // Utilisation du prompteur dynamique basé sur les suivis
                StringBuilder sb = new StringBuilder();
                for (model.SuiviSante s : suivis) {
                    sb.append("Date: ").append(s.getDate())
                      .append(", Sommeil: ").append(s.getHeuresSommeil())
                      .append(", Humeur: ").append(s.getHumeur()).append("/10")
                      .append(", Activité: ").append(s.getActivite()).append("\n");
                }
                return aiService.analyserDonnees(sb.toString());
            }
        };

        task.setOnSucceeded(e -> {
            model.BilanAnalyse analyse = task.getValue();
            
            try {
                BilanSante b = new BilanSante();
                b.setDateDebut(dateDebutPicker.getValue());
                b.setDateFin(dateFinPicker.getValue());
                b.setNiveauFatigue(Math.min(5, Math.max(1, (int) Math.ceil(analyse.getNiveauFatigue() / 2.0))));
                b.setNiveauStress(Math.min(5, Math.max(1, (int) Math.ceil(analyse.getNiveauStress() / 2.0))));
                b.setScoreForme(analyse.getScoreFormeGlobal() / 10.0f);
                boolean burnout = "ELEVE".equals(analyse.getRisqueBurnout()) || "CRITIQUE".equals(analyse.getRisqueBurnout());
                b.setRisqueBurnout(burnout);
                
                String recs = analyse.getRecommandations() != null ? String.join("\n- ", analyse.getRecommandations()) : "Aucune recommandation.";
                b.setRecommandations(recs);

                service.ajouter(b);

                utils.AlertUtils.showSuccess("Analyse et Sauvegarde terminées ! 📊", 
                    "Votre bilan de santé a été généré via IA et sauvegardé.\n\nRisque Burnout: " + analyse.getRisqueBurnout() + "\nRecommandations:\n- " + recs);

                goBack(event);

            } catch (SQLException ex) {
                utils.AlertUtils.showError("Échec de sauvegarde ⚠️", "Erreur base de données : " + ex.getMessage());
            }
        });

        task.setOnFailed(e -> {
            utils.AlertUtils.showError("Erreur d'analyse IA", task.getException().getMessage());
        });

        utils.AlertUtils.showSuccess("Analyse en cours...", "Veuillez patienter pendant que l'IA analyse vos suivis...");
        new Thread(task).start();
    }

    private boolean isInputValid() {
        String msg = "";
        if (dateDebutPicker.getValue() == null || dateFinPicker.getValue() == null) {
            msg += "Dates manquantes.\n";
        } else if (dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
            msg += "La date de fin doit être après la date de début.\n";
        }

        if (msg.isEmpty()) return true;
        utils.AlertUtils.showError("Bilan incomplet ❌", msg);
        return false;
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ReadSante.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
            scene.setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
