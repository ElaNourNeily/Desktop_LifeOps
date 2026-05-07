package service.Time;

import model.Time.Activite;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiviteService implements IService<Activite> {

    private Connection connection;

    public ActiviteService() {
        connection = MyDatabase.getInstance().getConnection();
        checkAndRepairDatabase();
    }

    private void checkAndRepairDatabase() {
        try {
            DatabaseMetaData md = connection.getMetaData();
            ResultSet rs = md.getColumns(null, null, "activite", "recurrence_group_id");
            if (!rs.next()) {
                System.out.println("[DB] Adding recurrence_group_id column to activite table...");
                connection.createStatement().execute("ALTER TABLE activite ADD COLUMN recurrence_group_id INT NULL");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Could not check/repair table: " + e.getMessage());
        }
    }

    @Override
    public void ajouter(Activite activite) throws SQLException {
        System.out.println("[DEBUG] Inserting Activity. Planning ID: " + activite.getPlanningId());
        String sql = "INSERT INTO activite (titre, duree, priorite, etat, heure_debut_estimee, heure_fin_estimee, niveau_urgence, categorie, couleur, suggested_by_ai, planning_id, minutes_rappel, heure_debut_reelle, heure_fin_reelle, is_recurrent, recurrence_type, recurrence_days, recurrence_interval, recurrence_group_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, activite.getTitre());
        ps.setInt(2, activite.getDuree());
        ps.setInt(3, activite.getPriorite());
        ps.setString(4, activite.getEtat());
        ps.setTime(5, activite.getHeureDebutEstimee());
        ps.setTime(6, activite.getHeureFinEstimee());
        ps.setString(7, activite.getNiveauUrgence());
        ps.setString(8, activite.getCategorie());
        ps.setString(9, activite.getCouleur());
        ps.setBoolean(10, activite.isSuggestedByAi());
        ps.setInt(11, activite.getPlanningId());
        ps.setInt(12, activite.getMinutesRappel());
        ps.setTime(13, activite.getHeureDebutReelle());
        ps.setTime(14, activite.getHeureFinReelle());
        ps.setBoolean(15, activite.isRecurrent());
        ps.setString(16, activite.getRecurrenceType());
        ps.setString(17, activite.getRecurrenceDays());
        ps.setInt(18, activite.getRecurrenceInterval());
        if (activite.getRecurrenceGroupId() > 0) ps.setInt(19, activite.getRecurrenceGroupId());
        else ps.setNull(19, Types.INTEGER);
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            activite.setId(rs.getInt(1));
        }
    }

    @Override
    public void modifier(Activite activite) throws SQLException {
        String sql = "UPDATE activite SET titre = ?, duree = ?, priorite = ?, etat = ?, heure_debut_estimee = ?, heure_fin_estimee = ?, niveau_urgence = ?, categorie = ?, couleur = ?, suggested_by_ai = ?, planning_id = ?, minutes_rappel = ?, heure_debut_reelle = ?, heure_fin_reelle = ?, is_recurrent = ?, recurrence_type = ?, recurrence_days = ?, recurrence_interval = ?, recurrence_group_id = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, activite.getTitre());
        ps.setInt(2, activite.getDuree());
        ps.setInt(3, activite.getPriorite());
        ps.setString(4, activite.getEtat());
        ps.setTime(5, activite.getHeureDebutEstimee());
        ps.setTime(6, activite.getHeureFinEstimee());
        ps.setString(7, activite.getNiveauUrgence());
        ps.setString(8, activite.getCategorie());
        ps.setString(9, activite.getCouleur());
        ps.setBoolean(10, activite.isSuggestedByAi());
        ps.setInt(11, activite.getPlanningId());
        ps.setInt(12, activite.getMinutesRappel());
        ps.setTime(13, activite.getHeureDebutReelle());
        ps.setTime(14, activite.getHeureFinReelle());
        ps.setBoolean(15, activite.isRecurrent());
        ps.setString(16, activite.getRecurrenceType());
        ps.setString(17, activite.getRecurrenceDays());
        ps.setInt(18, activite.getRecurrenceInterval());
        if (activite.getRecurrenceGroupId() > 0) ps.setInt(19, activite.getRecurrenceGroupId());
        else ps.setNull(19, Types.INTEGER);
        ps.setInt(20, activite.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM activite WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public void supprimerSuggestionsIA(int planningId) throws SQLException {
        String sql = "DELETE FROM activite WHERE planning_id = ? AND suggested_by_ai = 1";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, planningId);
        ps.executeUpdate();
    }

    public void supprimerSerie(int recurrenceGroupId) throws SQLException {
        String sql = "DELETE FROM activite WHERE recurrence_group_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, recurrenceGroupId);
        ps.executeUpdate();
    }

    @Override
    public List<Activite> recuperer() throws SQLException {
        List<Activite> activites = new ArrayList<>();
        if (connection == null) return activites;
        String sql = "SELECT * FROM activite";
        ResultSet rs = connection.createStatement().executeQuery(sql);
        while (rs.next()) {
            activites.add(new Activite(
                    rs.getInt("id"), rs.getString("titre"), rs.getInt("duree"), rs.getInt("priorite"),
                    rs.getString("etat"), rs.getTime("heure_debut_estimee"), rs.getTime("heure_fin_estimee"),
                    rs.getString("niveau_urgence"), rs.getString("categorie"), rs.getString("couleur"),
                    rs.getBoolean("suggested_by_ai"), rs.getInt("planning_id"), rs.getInt("minutes_rappel"),
                    rs.getTime("heure_debut_reelle"), rs.getTime("heure_fin_reelle"), rs.getBoolean("is_recurrent"),
                    rs.getString("recurrence_type"), rs.getString("recurrence_days"), rs.getInt("recurrence_interval"),
                    rs.getInt("recurrence_group_id")
            ));
        }
        return activites;
    }
    
    public List<Activite> recupererParPlanning(int planningId) throws SQLException {
        List<Activite> activites = new ArrayList<>();
        String sql = "SELECT * FROM activite WHERE planning_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, planningId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            activites.add(new Activite(
                    rs.getInt("id"), rs.getString("titre"), rs.getInt("duree"), rs.getInt("priorite"),
                    rs.getString("etat"), rs.getTime("heure_debut_estimee"), rs.getTime("heure_fin_estimee"),
                    rs.getString("niveau_urgence"), rs.getString("categorie"), rs.getString("couleur"),
                    rs.getBoolean("suggested_by_ai"), rs.getInt("planning_id"), rs.getInt("minutes_rappel"),
                    rs.getTime("heure_debut_reelle"), rs.getTime("heure_fin_reelle"), rs.getBoolean("is_recurrent"),
                    rs.getString("recurrence_type"), rs.getString("recurrence_days"), rs.getInt("recurrence_interval"),
                    rs.getInt("recurrence_group_id")
            ));
        }
        return activites;
    }

    // ─── TRI : priorité décroissante, puis durée croissante ──────────────
    @Override
    public List<Activite> trier() throws SQLException {
        List<Activite> activites = new ArrayList<>();
        String sql = "SELECT * FROM activite ORDER BY priorite DESC, duree ASC";
        ResultSet rs = connection.createStatement().executeQuery(sql);
        while (rs.next()) {
            activites.add(new Activite(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getInt("duree"),
                    rs.getInt("priorite"),
                    rs.getString("etat"),
                    rs.getTime("heure_debut_estimee"),
                    rs.getTime("heure_fin_estimee"),
                    rs.getString("niveau_urgence"),
                    rs.getString("categorie"),
                    rs.getString("couleur"),
                    rs.getBoolean("suggested_by_ai"),
                    rs.getInt("planning_id"),
                    rs.getInt("minutes_rappel"),
                    rs.getTime("heure_debut_reelle"),
                    rs.getTime("heure_fin_reelle"),
                    rs.getBoolean("is_recurrent"),
                    rs.getString("recurrence_type"),
                    rs.getString("recurrence_days"),
                    rs.getInt("recurrence_interval")
            ));
        }
        return activites;
    }

    // ─── RECHERCHE : par titre ou catégorie (insensible à la casse) ──────
    @Override
    public List<Activite> rechercher(String motCle) throws SQLException {
        List<Activite> activites = new ArrayList<>();
        String sql = "SELECT * FROM activite WHERE titre LIKE ? OR categorie LIKE ? ORDER BY priorite DESC";
        PreparedStatement ps = connection.prepareStatement(sql);
        String pattern = "%" + motCle + "%";
        ps.setString(1, pattern);
        ps.setString(2, pattern);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            activites.add(new Activite(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getInt("duree"),
                    rs.getInt("priorite"),
                    rs.getString("etat"),
                    rs.getTime("heure_debut_estimee"),
                    rs.getTime("heure_fin_estimee"),
                    rs.getString("niveau_urgence"),
                    rs.getString("categorie"),
                    rs.getString("couleur"),
                    rs.getBoolean("suggested_by_ai"),
                    rs.getInt("planning_id"),
                    rs.getInt("minutes_rappel"),
                    rs.getTime("heure_debut_reelle"),
                    rs.getTime("heure_fin_reelle"),
                    rs.getBoolean("is_recurrent"),
                    rs.getString("recurrence_type"),
                    rs.getString("recurrence_days"),
                    rs.getInt("recurrence_interval")
            ));
        }
        return activites;
    }

    // ─── DASHBOARD : Distribution des priorités ──────────────
    public java.util.Map<String, Integer> getPriorityDistribution(int utilisateurId) throws SQLException {
        java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();
        // 1=Basse (Emerald), 2=Moyenne (Amber), 3=Haute (Pink) - according to common patterns
        String sql = "SELECT a.priorite, COUNT(*) as count " +
                     "FROM activite a " +
                     "JOIN planning p ON a.planning_id = p.id " +
                     "WHERE p.utilisateur_id = ? " +
                     "GROUP BY a.priorite";
        
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String pStr = switch (rs.getInt("priorite")) {
                case 1 -> "Basse";
                case 2 -> "Moyenne";
                case 3 -> "Haute";
                default -> "Inconnue";
            };
            stats.put(pStr, rs.getInt("count"));
        }
        return stats;
    }

    // ─── DASHBOARD : Taux de complétion ──────────────
    public double getCompletionRate(int utilisateurId) throws SQLException {
        String sql = "SELECT COUNT(*) as total, SUM(CASE WHEN UPPER(a.etat) = 'TERMINÉ' OR a.etat = 'Terminé' THEN 1 ELSE 0 END) as done " +
                     "FROM activite a " +
                     "JOIN planning p ON a.planning_id = p.id " +
                     "WHERE p.utilisateur_id = ?";
                     
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int total = rs.getInt("total");
            int done = rs.getInt("done");
            return total == 0 ? 0.0 : (double) done / total;
        }
        return 0.0;
    }

    public boolean hasOverlap(int planningId, Time start, Time end, Integer ignoreId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM activite WHERE planning_id = ? AND " +
              "heure_debut_estimee < ? AND ? < heure_fin_estimee";
        
        if (ignoreId != null && ignoreId != -1) {
            sql += " AND id != " + ignoreId;
        }

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, planningId);
        ps.setTime(2, end);
        ps.setTime(3, start);
        
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    // ─── FEATURE 1 : STATS AVANCÉES ──────────────────────────────
    
    public java.util.Map<String, Integer> getTimeByCategory(int utilisateurId) throws SQLException {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        String sql = "SELECT a.categorie, SUM((TIME_TO_SEC(TIMEDIFF(a.heure_fin_estimee, a.heure_debut_estimee)) + IF(a.heure_fin_estimee < a.heure_debut_estimee, 86400, 0)) / 60) as minutes " +
                     "FROM activite a JOIN planning p ON a.planning_id = p.id " +
                     "WHERE p.utilisateur_id = ? GROUP BY a.categorie";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            stats.put(rs.getString("categorie"), rs.getInt("minutes"));
        }
        return stats;
    }

    public double getSchedulingAccuracy(int utilisateurId) throws SQLException {
        String sql = "SELECT COUNT(*) as total, SUM(CASE WHEN ABS(TIME_TO_SEC(TIMEDIFF(a.heure_fin_reelle, a.heure_fin_estimee))) < 900 THEN 1 ELSE 0 END) as accurate " +
                     "FROM activite a JOIN planning p ON a.planning_id = p.id " +
                     "WHERE p.utilisateur_id = ? AND (UPPER(a.etat) = 'TERMINÉ' OR a.etat = 'Terminé') AND a.heure_fin_reelle IS NOT NULL";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int total = rs.getInt("total");
            int accurate = rs.getInt("accurate");
            // If No accurate tasks yet, return 0 instead of 100 to show that data is missing or not yet tracked
            return total == 0 ? 0.0 : ((double) accurate / total) * 100.0;
        }
        return 0.0;
    }

    public java.util.Map<String, Integer> getWeeklyProductivityTrend(int utilisateurId) throws SQLException {
        java.util.Map<String, Integer> trend = new java.util.LinkedHashMap<>();
        String sql = "SELECT YEARWEEK(p.date, 3) as week, COUNT(*) as count " +
                     "FROM activite a JOIN planning p ON a.planning_id = p.id " +
                     "WHERE p.utilisateur_id = ? AND (UPPER(a.etat) = 'TERMINÉ' OR a.etat = 'Terminé') " +
                     "GROUP BY week ORDER BY week DESC LIMIT 5";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            trend.put(rs.getString("week"), rs.getInt("count"));
        }
        return trend;
    }
}

