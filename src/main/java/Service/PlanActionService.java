package Service;

import Model.PlanAction;
import Utilis.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlanActionService implements CRUD<PlanAction> {

    private final Connection connection;

    public PlanActionService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // CREATE
    @Override
    public void create(PlanAction plan) throws SQLException {
        String sql = "INSERT INTO plan_action (titre, description, priorite, date_debut, date_fin, objectif_id, statut) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plan.getTitre());
            ps.setString(2, plan.getDescription());
            ps.setString(3, plan.getPriorite());
            ps.setDate(4, plan.getDate_debut() != null ? Date.valueOf(plan.getDate_debut()) : null);
            ps.setDate(5, plan.getDate_fin() != null ? Date.valueOf(plan.getDate_fin()) : null);
            ps.setInt(6, plan.getObjectif_id());
            ps.setString(7, plan.getStatut());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                plan.setId(keys.getInt(1));
            }
            System.out.println("Plan d'action créé, ID = " + plan.getId());
        }
    }

    // UPDATE
    @Override
    public void update(PlanAction plan) throws SQLException {
        String sql = "UPDATE plan_action SET titre=?, description=?, priorite=?, date_debut=?, date_fin=?, statut=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, plan.getTitre());
            ps.setString(2, plan.getDescription());
            ps.setString(3, plan.getPriorite());
            ps.setDate(4, plan.getDate_debut() != null ? Date.valueOf(plan.getDate_debut()) : null);
            ps.setDate(5, plan.getDate_fin() != null ? Date.valueOf(plan.getDate_fin()) : null);
            ps.setString(6, plan.getStatut());
            ps.setInt(7, plan.getId());
            ps.executeUpdate();
            System.out.println("Plan d'action mis à jour.");
        }
    }

    // DELETE
    @Override
    public void delete(PlanAction plan) throws SQLException {
        String sql = "DELETE FROM plan_action WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, plan.getId());
            ps.executeUpdate();
            System.out.println("Plan d'action supprimé.");
        }
    }

    // FIND BY ID
    @Override
    public PlanAction findbyID(int id) throws SQLException {
        String sql = "SELECT * FROM plan_action WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }

    // FIND BY MAIL — non applicable pour PlanAction, retourne null
    @Override
    public PlanAction findbyMail(String mail) throws SQLException {
        return null;
    }

    // FIND ALL
    @Override
    public List<PlanAction> findAll() throws SQLException {
        List<PlanAction> plans = new ArrayList<>();
        String sql = "SELECT * FROM plan_action";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                plans.add(mapResultSet(rs));
            }
        }
        return plans;
    }

    // SORT BY NAME : Trier les plans d'action par titre
    @Override
    public List<PlanAction> sortbyName() throws SQLException {
        List<PlanAction> plans = new ArrayList<>();
        String sql = "SELECT * FROM plan_action ORDER BY titre ASC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                plans.add(mapResultSet(rs));
            }
        }
        return plans;
    }

    // Méthode utilitaire : lire les plans d'action par objectif
    public List<PlanAction> readByObjectif(int objectifId) throws SQLException {
        List<PlanAction> plans = new ArrayList<>();
        String sql = "SELECT * FROM plan_action WHERE objectif_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, objectifId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                plans.add(mapResultSet(rs));
            }
        }
        return plans;
    }

    // Méthode utilitaire : mapper un ResultSet vers un PlanAction
    private PlanAction mapResultSet(ResultSet rs) throws SQLException {
        return new PlanAction(
            rs.getInt("id"),
            rs.getString("titre"),
            rs.getString("description"),
            rs.getString("priorite"),
            rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null,
            rs.getDate("date_fin") != null ? rs.getDate("date_fin").toLocalDate() : null,
            rs.getInt("objectif_id"),
            rs.getString("statut")
        );
    }
}
