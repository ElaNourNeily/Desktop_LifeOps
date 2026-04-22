package Service;

import Utilis.MyDatabase;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardService {

    private final Connection connection;

    public DashboardService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // ── Compteurs généraux ────────────────────────────────────────────

    public int totalObjectifs() {
        return compterAvecSQL("SELECT COUNT(*) FROM objectif");
    }

    public int totalPlansAction() {
        return compterAvecSQL("SELECT COUNT(*) FROM plan_action");
    }

    public int objectifsTermines() {
        return compterAvecSQL("SELECT COUNT(*) FROM objectif WHERE statut = 'Complété'");
    }

    public int objectifsEnCours() {
        return compterAvecSQL("SELECT COUNT(*) FROM objectif WHERE statut = 'En cours'");
    }

    public int objectifsEnRetard() {
        return compterAvecSQL(
            "SELECT COUNT(*) FROM objectif WHERE date_fin < CURDATE() AND statut != 'Complété'"
        );
    }

    public double progressionMoyenne() {
        String sql = "SELECT AVG(progression) FROM objectif";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("Erreur progressionMoyenne : " + e.getMessage());
        }
        return 0;
    }

    // ── Répartition par catégorie ─────────────────────────────────────

    public Map<String, Integer> objectifsParCategorie() {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql = "SELECT categorie, COUNT(*) as nb FROM objectif GROUP BY categorie ORDER BY nb DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String cat = rs.getString("categorie");
                data.put(cat != null ? cat : "Autre", rs.getInt("nb"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur objectifsParCategorie : " + e.getMessage());
        }
        return data;
    }

    // ── Répartition par statut ────────────────────────────────────────

    public Map<String, Integer> objectifsParStatut() {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql = "SELECT statut, COUNT(*) as nb FROM objectif GROUP BY statut ORDER BY nb DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String statut = rs.getString("statut");
                data.put(statut != null ? statut : "Inconnu", rs.getInt("nb"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur objectifsParStatut : " + e.getMessage());
        }
        return data;
    }

    // ── Plans d'action par statut ─────────────────────────────────────

    public Map<String, Integer> plansParStatut() {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql = "SELECT statut, COUNT(*) as nb FROM plan_action GROUP BY statut ORDER BY nb DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String statut = rs.getString("statut");
                data.put(statut != null ? statut : "Inconnu", rs.getInt("nb"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur plansParStatut : " + e.getMessage());
        }
        return data;
    }

    // ── Progression par catégorie (moyenne) ──────────────────────────

    public Map<String, Double> progressionParCategorie() {
        Map<String, Double> data = new LinkedHashMap<>();
        String sql = "SELECT categorie, AVG(progression) as moy FROM objectif GROUP BY categorie";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String cat = rs.getString("categorie");
                data.put(cat != null ? cat : "Autre", rs.getDouble("moy"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur progressionParCategorie : " + e.getMessage());
        }
        return data;
    }

    // ── Helper ───────────────────────────────────────────────────────

    private int compterAvecSQL(String sql) {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
        return 0;
    }
}
