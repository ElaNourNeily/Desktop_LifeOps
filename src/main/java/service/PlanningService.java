package service;

import model.Planning;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlanningService implements IService<Planning> {

    private Connection connection;

    public PlanningService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Planning planning) throws SQLException {
        String sql = "INSERT INTO planning (date, disponibilite, heure_debut_journee, heure_fin_journee, utilisateur_id) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setDate(1, planning.getDate());
        ps.setBoolean(2, planning.isDisponibilite());
        ps.setTime(3, planning.getHeureDebutJournee());
        ps.setTime(4, planning.getHeureFinJournee());
        ps.setInt(5, planning.getUtilisateurId());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            int generatedId = rs.getInt(1);
            planning.setId(generatedId);
            System.out.println("[DEBUG] Planning inserted. Generated ID: " + generatedId);
        } else {
            System.err.println("[DEBUG] Planning inserted but NO ID WAS RETURNED!");
        }
    }

    @Override
    public void modifier(Planning planning) throws SQLException {
        String sql = "UPDATE planning SET date = ?, disponibilite = ?, heure_debut_journee = ?, heure_fin_journee = ?, utilisateur_id = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setDate(1, planning.getDate());
        ps.setBoolean(2, planning.isDisponibilite());
        ps.setTime(3, planning.getHeureDebutJournee());
        ps.setTime(4, planning.getHeureFinJournee());
        ps.setInt(5, planning.getUtilisateurId());
        ps.setInt(6, planning.getId());
        
        int rows = ps.executeUpdate();
        System.out.println("[SQL DEBUG] Planning updated in DB. ID: " + planning.getId() + " | Rows affected: " + rows);
        ps.close();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // Cascade delete activities manually to ensure integrity
        String sqlActivites = "DELETE FROM activite WHERE planning_id = ?";
        PreparedStatement ps1 = connection.prepareStatement(sqlActivites);
        ps1.setInt(1, id);
        ps1.executeUpdate();

        // Now delete the planning
        String sqlPlanning = "DELETE FROM planning WHERE id = ?";
        PreparedStatement ps2 = connection.prepareStatement(sqlPlanning);
        ps2.setInt(1, id);
        ps2.executeUpdate();
    }

    @Override
    public List<Planning> recuperer() throws SQLException {
        List<Planning> plannings = new ArrayList<>();
        if (connection == null) {
            System.err.println("Database connection is null. Cannot retrieve plannings.");
            return plannings;
        }
        String sql = "SELECT * FROM planning";
        Statement s = connection.createStatement();
        ResultSet rs = s.executeQuery(sql);
        while (rs.next()) {
            Planning p = new Planning(
                    rs.getInt("id"),
                    rs.getDate("date"),
                    rs.getBoolean("disponibilite"),
                    rs.getTime("heure_debut_journee"),
                    rs.getTime("heure_fin_journee"),
                    rs.getInt("utilisateur_id")
            );
            plannings.add(p);
        }
        return plannings;
    }

    // ─── TRI : par date la plus récente ──────────────
    @Override
    public List<Planning> trier() throws SQLException {
        List<Planning> plannings = new ArrayList<>();
        String sql = "SELECT * FROM planning ORDER BY date DESC, heure_debut_journee ASC";
        ResultSet rs = connection.createStatement().executeQuery(sql);
        while (rs.next()) {
            plannings.add(new Planning(
                    rs.getInt("id"),
                    rs.getDate("date"),
                    rs.getBoolean("disponibilite"),
                    rs.getTime("heure_debut_journee"),
                    rs.getTime("heure_fin_journee"),
                    rs.getInt("utilisateur_id")
            ));
        }
        return plannings;
    }

    // ─── RECHERCHE : par date (YYYY-MM-DD) ou ID utilisateur ──────────────
    @Override
    public List<Planning> rechercher(String motCle) throws SQLException {
        List<Planning> plannings = new ArrayList<>();
        String sql = "SELECT * FROM planning WHERE date LIKE ? OR utilisateur_id LIKE ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        String pattern = "%" + motCle + "%";
        ps.setString(1, pattern);
        ps.setString(2, pattern);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            plannings.add(new Planning(
                    rs.getInt("id"),
                    rs.getDate("date"),
                    rs.getBoolean("disponibilite"),
                    rs.getTime("heure_debut_journee"),
                    rs.getTime("heure_fin_journee"),
                    rs.getInt("utilisateur_id")
            ));
        }
        return plannings;
    }

    // ─── DASHBOARD : Récupérer les plannings d'une semaine ──────────────
    public List<Planning> recupererParSemaine(int utilisateurId, java.sql.Date debutSemaine) throws SQLException {
        List<Planning> plannings = new ArrayList<>();
        String sql = "SELECT * FROM planning WHERE utilisateur_id = ? AND date >= ? AND date <= DATE_ADD(?, INTERVAL 6 DAY) ORDER BY date ASC";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ps.setDate(2, debutSemaine);
        ps.setDate(3, debutSemaine);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            plannings.add(new Planning(
                    rs.getInt("id"),
                    rs.getDate("date"),
                    rs.getBoolean("disponibilite"),
                    rs.getTime("heure_debut_journee"),
                    rs.getTime("heure_fin_journee"),
                    rs.getInt("utilisateur_id")
            ));
        }
        return plannings;
    }

    // ─── DASHBOARD : Statistiques minutes travaillées ──────────────
    public java.util.Map<String, Integer> getWeeklyWorkedMinutes(int utilisateurId) throws SQLException {
        java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();
        String sql = "SELECT p.date, SUM(TIME_TO_SEC(TIMEDIFF(a.heure_fin_estimee, a.heure_debut_estimee))/60) as minutes " +
                     "FROM planning p " +
                     "JOIN activite a ON p.id = a.planning_id " +
                     "WHERE p.utilisateur_id = ? " +
                     "AND p.date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                     "GROUP BY p.date ORDER BY p.date ASC";
        
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            stats.put(rs.getDate("date").toString(), rs.getInt("minutes"));
        }
        return stats;
    }

    public Planning recupererParDate(java.time.LocalDate date, int utilisateurId) throws SQLException {
        String sql = "SELECT * FROM planning WHERE date = ? AND utilisateur_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setDate(1, java.sql.Date.valueOf(date));
        ps.setInt(2, utilisateurId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new Planning(
                rs.getInt("id"),
                rs.getDate("date"),
                rs.getBoolean("disponibilite"),
                rs.getTime("heure_debut_journee"),
                rs.getTime("heure_fin_journee"),
                rs.getInt("utilisateur_id")
            );
        }
        return null;
    }

    public List<Planning> recupererParUtilisateur(int id) throws SQLException {
        List<Planning> plannings = new ArrayList<>();
        String sql = "SELECT * FROM planning WHERE utilisateur_id = ? ORDER BY date DESC";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            plannings.add(new Planning(
                rs.getInt("id"),
                rs.getDate("date"),
                rs.getBoolean("disponibilite"),
                rs.getTime("heure_debut_journee"),
                rs.getTime("heure_fin_journee"),
                rs.getInt("utilisateur_id")
            ));
        }
        return plannings;
    }
}
