package service.health;

import Model.health.SuiviSante;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service providing aggregated health data for the Dashboard.
 */
public class HealthDataService {

    private final Connection connection;

    public HealthDataService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // ── DTO ───────────────────────────────────────────────────────────

    /**
     * Lightweight projection used by DashboardController.
     *
     * @param scoreForme     overall wellness score (0–100)
     * @param risqueBurnout  one of: FAIBLE, MODERE, ELEVE, CRITIQUE
     */
    public record SuiviSanteDTO(double scoreForme, String risqueBurnout) {}

    // ── Queries ───────────────────────────────────────────────────────

    /**
     * Returns the most recent suivi_sante entries (up to 7), newest first.
     */
    public List<SuiviSanteDTO> getRecentData() {
        List<SuiviSanteDTO> result = new ArrayList<>();
        String sql = "SELECT heures_sommeil, qualite_sommeil, verres_eau, " +
                     "minutes_activite, humeur " +
                     "FROM suivi_sante ORDER BY date DESC LIMIT 7";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                double sommeil  = rs.getDouble("heures_sommeil");
                int    qualite  = rs.getInt("qualite_sommeil");
                int    eau      = rs.getInt("verres_eau");
                int    activite = rs.getInt("minutes_activite");
                int    humeur   = rs.getInt("humeur");

                double score   = calculerScore(sommeil, qualite, eau, activite, humeur);
                String burnout = evaluerBurnout(sommeil, qualite, humeur, activite);
                result.add(new SuiviSanteDTO(score, burnout));
            }
        } catch (SQLException e) {
            System.err.println("HealthDataService.getRecentData : " + e.getMessage());
        }
        return result;
    }

    /** Average sleep hours over the last 30 days. */
    public double getMoyenneSommeil() {
        return moyenneColonne("heures_sommeil");
    }

    /** Average water glasses over the last 30 days. */
    public double getMoyenneEau() {
        return moyenneColonne("verres_eau");
    }

    /** Average mood (1–5 scale) over the last 30 days. */
    public double getMoyenneHumeur() {
        return moyenneColonne("humeur");
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private double moyenneColonne(String colonne) {
        String sql = "SELECT AVG(" + colonne + ") FROM suivi_sante " +
                     "WHERE date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("HealthDataService.moyenneColonne(" + colonne + ") : " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Simple wellness score (0–100) derived from the five tracked metrics.
     */
    private double calculerScore(double sommeil, int qualite, int eau,
                                  int activite, int humeur) {
        // Sommeil idéal : 7–9 h  → 0–25 pts
        double scoreSommeil = Math.max(0, 25 - Math.abs(sommeil - 8) * 5);
        // Qualité sommeil 1–5    → 0–20 pts
        double scoreQualite = (qualite / 5.0) * 20;
        // Eau : 8 verres idéal   → 0–20 pts
        double scoreEau     = Math.min(20, (eau / 8.0) * 20);
        // Activité : 30 min idéal → 0–20 pts
        double scoreActivite = Math.min(20, (activite / 30.0) * 20);
        // Humeur 1–5             → 0–15 pts
        double scoreHumeur  = (humeur / 5.0) * 15;

        return Math.min(100, scoreSommeil + scoreQualite + scoreEau + scoreActivite + scoreHumeur);
    }

    /**
     * Burnout risk level based on sleep, sleep quality, mood, and activity.
     */
    private String evaluerBurnout(double sommeil, int qualite, int humeur, int activite) {
        int risque = 0;
        if (sommeil < 6)   risque += 2;
        else if (sommeil < 7) risque += 1;
        if (qualite <= 2)  risque += 2;
        else if (qualite == 3) risque += 1;
        if (humeur <= 2)   risque += 2;
        else if (humeur == 3) risque += 1;
        if (activite < 15) risque += 1;

        if (risque >= 6) return "CRITIQUE";
        if (risque >= 4) return "ELEVE";
        if (risque >= 2) return "MODERE";
        return "FAIBLE";
    }
}
