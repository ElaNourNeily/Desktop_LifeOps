package Service;

import Model.PlanAction;
import Utilis.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlanActionService {

    private final Connection connection;

    public PlanActionService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // CREATE
    public void create(PlanAction plan) {
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
        } catch (SQLException e) {
            System.err.println("Erreur création plan d'action : " + e.getMessage());
        }
    }

    // READ ALL par objectif
    public List<PlanAction> readByObjectif(int objectifId) {
        List<PlanAction> plans = new ArrayList<>();
        String sql = "SELECT * FROM plan_action WHERE objectif_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, objectifId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PlanAction p = new PlanAction(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getString("priorite"),
                    rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null,
                    rs.getDate("date_fin") != null ? rs.getDate("date_fin").toLocalDate() : null,
                    rs.getInt("objectif_id"),
                    rs.getString("statut")
                );
                plans.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture plans d'action : " + e.getMessage());
        }
        return plans;
    }

    // UPDATE
    public void update(PlanAction plan) {
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
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour plan d'action : " + e.getMessage());
        }
    }

    // DELETE
    public void delete(int id) {
        String sql = "DELETE FROM plan_action WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Plan d'action supprimé.");
        } catch (SQLException e) {
            System.err.println("Erreur suppression plan d'action : " + e.getMessage());
        }
    }
}
