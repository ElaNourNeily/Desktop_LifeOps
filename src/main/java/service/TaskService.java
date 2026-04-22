package service;

import model.task.Tache;
import enums.PrioriteTache;
import enums.StatutTache;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskService {

    private final Connection connection;

    public TaskService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    public void addTask(Tache t) {
        if (t.getDescription() == null) {
            t.setDescription("");
        }
        String sql = "INSERT INTO tache (titre, description, priorite, difficulte, statut, deadline, task_space_id, utilisateur_id, assigned_user_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTitre());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getPriorite().name().toLowerCase());
            ps.setInt(4, t.getDifficulte());
            ps.setString(5, t.getStatut().getValeur()); 
            ps.setTimestamp(6, t.getDeadline() != null ? new java.sql.Timestamp(t.getDeadline().getTime()) : null);
            ps.setInt(7, t.getTaskSpaceId());
            ps.setInt(8, t.getUtilisateurId());
            if (t.getAssignedUserId() != null) ps.setInt(9, t.getAssignedUserId()); else ps.setNull(9, Types.INTEGER);
            ps.setTimestamp(10, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(11, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                t.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateTask(Tache t) {
        if (t.getDescription() == null) {
            t.setDescription("");
        }
        String sql = "UPDATE tache SET titre=?, description=?, priorite=?, difficulte=?, statut=?, deadline=?, assigned_user_id=?, updated_at=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, t.getTitre());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getPriorite().name().toLowerCase()); 
            ps.setInt(4, t.getDifficulte());
            ps.setString(5, t.getStatut().getValeur()); 
            ps.setTimestamp(6, t.getDeadline() != null ? new java.sql.Timestamp(t.getDeadline().getTime()) : null);
            if (t.getAssignedUserId() != null) ps.setInt(7, t.getAssignedUserId()); else ps.setNull(7, Types.INTEGER);
            ps.setTimestamp(8, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setInt(9, t.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteTask(int id) {
        String sql = "DELETE FROM tache WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Tache> getTasksByBoard(int boardId) {
        List<Tache> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tache WHERE task_space_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, boardId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tasks.add(mapResultSetToTache(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    private Tache mapResultSetToTache(ResultSet rs) throws SQLException {
        Tache t = new Tache();
        t.setId(rs.getInt("id"));
        t.setTitre(rs.getString("titre"));
        t.setDescription(rs.getString("description"));
        t.setPriorite(PrioriteTache.fromString(rs.getString("priorite")));
        t.setDifficulte(rs.getInt("difficulte"));
        t.setStatut(StatutTache.fromString(rs.getString("statut")));
        t.setDeadline(rs.getTimestamp("deadline"));
        t.setCreatedAt(rs.getTimestamp("created_at"));
        t.setUpdatedAt(rs.getTimestamp("updated_at"));
        t.setTaskSpaceId(rs.getInt("task_space_id"));
        t.setUtilisateurId(rs.getInt("utilisateur_id"));
        int assignedId = rs.getInt("assigned_user_id");
        if (!rs.wasNull()) t.setAssignedUserId(assignedId);
        return t;
    }
}
