package Service;

import Model.Objectif;
import Utilis.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ObjectifService implements CRUD<Objectif> {
    private final Connection connection;

    public ObjectifService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // CREATE — insère l'objectif dans la base de données
    @Override
    public void create(Objectif objectif) throws SQLException {
        String sql = "INSERT INTO objectif (titre, description, categorie, statut, date_debut, date_fin, utilisateur_id, progression) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, objectif.getTitre());
            ps.setString(2, objectif.getDescription());
            ps.setString(3, objectif.getCategorie());
            ps.setString(4, objectif.getStatut());
            ps.setDate(5, objectif.getDate_debut() != null ? Date.valueOf(objectif.getDate_debut()) : null);
            ps.setDate(6, objectif.getDate_fin() != null ? Date.valueOf(objectif.getDate_fin()) : null);
            ps.setInt(7, 1); // utilisateur_id par défaut
            ps.setInt(8, objectif.getProgression());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                objectif.setId(keys.getInt(1));
            }
            System.out.println("Objectif enregistré avec succès, ID = " + objectif.getId());
        }
    }

    // UPDATE : Mettre à jour un objectif dans la base de données
    @Override
    public void update(Objectif objectif) throws SQLException {
        String sql = "UPDATE objectif SET titre=?, description=?, categorie=?, statut=?, date_debut=?, date_fin=?, progression=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, objectif.getTitre());
            ps.setString(2, objectif.getDescription());
            ps.setString(3, objectif.getCategorie());
            ps.setString(4, objectif.getStatut());
            ps.setDate(5, objectif.getDate_debut() != null ? Date.valueOf(objectif.getDate_debut()) : null);
            ps.setDate(6, objectif.getDate_fin() != null ? Date.valueOf(objectif.getDate_fin()) : null);
            ps.setInt(7, objectif.getProgression());
            ps.setInt(8, objectif.getId());
            ps.executeUpdate();
            System.out.println("Objectif mis à jour avec succès.");
        }
    }

    // DELETE : Supprimer un objectif de la base de données
    @Override
    public void delete(Objectif objectif) throws SQLException {
        String sql = "DELETE FROM objectif WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, objectif.getId());
            ps.executeUpdate();
            System.out.println("Objectif supprimé avec succès.");
        }
    }

    // FIND BY ID
    @Override
    public Objectif findbyID(int id) throws SQLException {
        String sql = "SELECT * FROM objectif WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }

    // FIND BY MAIL — non applicable pour Objectif, retourne null
    @Override
    public Objectif findbyMail(String mail) throws SQLException {
        return null;
    }

    // FIND ALL : Tous les objectifs depuis la base de données
    @Override
    public List<Objectif> findAll() throws SQLException {
        List<Objectif> objectifs = new ArrayList<>();
        String sql = "SELECT * FROM objectif";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                objectifs.add(mapResultSet(rs));
            }
        }
        return objectifs;
    }

    // SORT BY NAME : Trier les objectifs par titre
    @Override
    public List<Objectif> sortbyName() throws SQLException {
        List<Objectif> objectifs = new ArrayList<>();
        String sql = "SELECT * FROM objectif ORDER BY titre ASC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                objectifs.add(mapResultSet(rs));
            }
        }
        return objectifs;
    }

    // Méthode utilitaire : recherche par titre (partielle)
    public List<Objectif> searchByTitre(String titre) throws SQLException {
        List<Objectif> objectifs = new ArrayList<>();
        String sql = "SELECT * FROM objectif WHERE titre LIKE ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "%" + titre + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                objectifs.add(mapResultSet(rs));
            }
        }
        return objectifs;
    }

    /**
     * Recalcule automatiquement la progression d'un objectif
     * en fonction du ratio de plans d'action terminés.
     *
     * Règles :
     *  - Aucun plan          → progression inchangée
     *  - Tous terminés       → 100% + statut "Complété"
     *  - Partiellement       → (nb terminés / total) * 100, arrondi
     *  - Aucun terminé       → 0%
     */
    public void recalculerProgression(int objectifId) throws SQLException {
        // Compter le total et les terminés en une seule requête
        String sql = """
            SELECT
                COUNT(*) AS total,
                SUM(CASE WHEN LOWER(statut) IN ('terminé','termine','terminé','completed') THEN 1 ELSE 0 END) AS termines
            FROM plan_action
            WHERE objectif_id = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, objectifId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int total    = rs.getInt("total");
                int termines = rs.getInt("termines");

                if (total == 0) return; // pas de plans → on ne touche pas à la progression manuelle

                int nouvelleProgression = (int) Math.round((double) termines / total * 100);

                // Mettre à jour progression (et statut si 100%)
                String nouveauStatut = nouvelleProgression == 100 ? "Complété" : null;

                String updateSql = nouveauStatut != null
                    ? "UPDATE objectif SET progression = ?, statut = ? WHERE id = ?"
                    : "UPDATE objectif SET progression = ? WHERE id = ?";

                try (PreparedStatement upd = connection.prepareStatement(updateSql)) {
                    upd.setInt(1, nouvelleProgression);
                    if (nouveauStatut != null) {
                        upd.setString(2, nouveauStatut);
                        upd.setInt(3, objectifId);
                    } else {
                        upd.setInt(2, objectifId);
                    }
                    upd.executeUpdate();
                    System.out.println("Progression recalculée : " + nouvelleProgression + "% (objectif #" + objectifId + ")");
                }
            }
        }
    }

    // Méthode utilitaire : mapper un ResultSet vers un Objectif
    private Objectif mapResultSet(ResultSet rs) throws SQLException {
        Objectif o = new Objectif(
            rs.getString("titre"),
            rs.getString("description"),
            rs.getString("categorie"),
            rs.getString("statut"),
            rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null,
            rs.getDate("date_fin") != null ? rs.getDate("date_fin").toLocalDate() : null,
            rs.getInt("progression")
        );
        o.setId(rs.getInt("id"));
        return o;
    }
}
