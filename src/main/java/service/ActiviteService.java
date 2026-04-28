package service;

import model.Activite;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiviteService implements IService<Activite> {

    private Connection connection;

    public ActiviteService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Activite activite) throws SQLException {
        System.out.println("[DEBUG] Inserting Activity. Planning ID: " + activite.getPlanningId());
        String sql = "INSERT INTO activite (titre, duree, priorite, etat, heure_debut_estimee, heure_fin_estimee, niveau_urgence, categorie, couleur, suggested_by_ai, planning_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            activite.setId(rs.getInt(1));
        }
    }

    @Override
    public void modifier(Activite activite) throws SQLException {
        String sql = "UPDATE activite SET titre = ?, duree = ?, priorite = ?, etat = ?, heure_debut_estimee = ?, heure_fin_estimee = ?, niveau_urgence = ?, categorie = ?, couleur = ?, suggested_by_ai = ?, planning_id = ? WHERE id = ?";
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
        ps.setInt(12, activite.getId());
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

    @Override
    public List<Activite> recuperer() throws SQLException {
        List<Activite> activites = new ArrayList<>();
        if (connection == null) {
            System.err.println("Database connection is null. Cannot retrieve activities.");
            return activites;
        }
        String sql = "SELECT * FROM activite";
        Statement s = connection.createStatement();
        ResultSet rs = s.executeQuery(sql);
        while (rs.next()) {
            Activite a = new Activite(
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
                    rs.getBoolean("suggested_by_ai"), // Note: The field name might vary, I'll use suggested_by_ai as per plan
                    rs.getInt("planning_id")
            );
            activites.add(a);
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
            Activite a = new Activite(
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
                    rs.getInt("planning_id")
            );
            activites.add(a);
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
                    rs.getInt("planning_id")
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
                    rs.getInt("planning_id")
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
        String sql = "SELECT COUNT(*) as total, SUM(CASE WHEN a.etat = 'terminé' THEN 1 ELSE 0 END) as done " +
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
                     "((heure_debut_estimee < ? AND heure_fin_estimee > ?))";
        
        // A more robust overlap check for [S1, E1] and [S2, E2]: S1 < E2 AND S2 < E1
        sql = "SELECT COUNT(*) FROM activite WHERE planning_id = ? AND " +
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
}

