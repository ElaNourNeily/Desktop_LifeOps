package Service;

import Model.Objectif;
import Utilis.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ObjectifService {
    private final Connection connection;

    public ObjectifService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // CREATE — insère l'objectif dans la base de données
    public void create(Objectif objectif) {
        String sql = "INSERT INTO objectif (titre, description, categorie, statut, date_debut, date_fin, utilisateur_id, progression) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, objectif.getTitre());
            ps.setString(2, objectif.getDescription());
            ps.setString(3, objectif.getCategorie());
            ps.setString(4, objectif.getStatut());
            ps.setDate(5, objectif.getDate_debut() != null ? Date.valueOf(objectif.getDate_debut()) : null);
            ps.setDate(6, objectif.getDate_fin() != null ? Date.valueOf(objectif.getDate_fin()) : null);
            ps.setInt(7, 1); // utilisateur_id par défaut, à remplacer par l'utilisateur connecté
            ps.setInt(8, objectif.getProgression());
            ps.executeUpdate();

            // Récupérer l'ID généré automatiquement
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                objectif.setId(keys.getInt(1));
            }
            System.out.println("Objectif enregistré avec succès, ID = " + objectif.getId());
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion de l'objectif : " + e.getMessage());
        }
    }

    // READ : Tous les objectifs depuis la base de données
    public List<Objectif> readAll() {
        List<Objectif> objectifs = new ArrayList<>();
        String sql = "SELECT * FROM objectif";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
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
                objectifs.add(o);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture des objectifs : " + e.getMessage());
        }
        return objectifs;
    }

    // READ : Chercher un objectif par son ID
    public Optional<Objectif> readById(int id) {
        String sql = "SELECT * FROM objectif WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
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
                return Optional.of(o);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche par ID : " + e.getMessage());
        }
        return Optional.empty();
    }

    // READ : Chercher des objectifs par titre (recherche partielle)
    public List<Objectif> searchByTitre(String titre) {
        List<Objectif> objectifs = new ArrayList<>();
        String sql = "SELECT * FROM objectif WHERE titre LIKE ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "%" + titre + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
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
                objectifs.add(o);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche par titre : " + e.getMessage());
        }
        return objectifs;
    }

    // UPDATE : Mettre à jour un objectif dans la base de données
    public void update(Objectif objectif) {
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
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de l'objectif : " + e.getMessage());
        }
    }

    // DELETE : Supprimer un objectif de la base de données
    public void delete(int id) {
        String sql = "DELETE FROM objectif WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Objectif supprimé avec succès.");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de l'objectif : " + e.getMessage());
        }
    }
}
