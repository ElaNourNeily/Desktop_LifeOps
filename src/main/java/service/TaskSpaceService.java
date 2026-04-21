package service;

import model.task.TaskSpace;
import enums.StatutTaskSpace;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskSpaceService {

    private final Connection connection;

    public TaskSpaceService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    public void addTaskSpace(TaskSpace ts) {
        // Hybrid Solution: Using the 'type' column to store 'mode|category'
        String sql = "INSERT INTO task_space (nom, type, date_creation, description, duration, status, utilisateur_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ts.getNom());
            // Store as mode|category
            ps.setString(2, (ts.getMode() != null ? ts.getMode() : "Solo") + "|" + (ts.getCategory() != null ? ts.getCategory() : "Autre"));
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setString(4, ts.getDescription());
            ps.setInt(5, ts.getDuration());
            ps.setString(6, ts.getStatus().name());
            ps.setInt(7, ts.getUtilisateurId());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                ts.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateTaskSpace(TaskSpace ts) {
        String sql = "UPDATE task_space SET nom=?, type=?, description=?, duration=?, status=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ts.getNom());
            ps.setString(2, (ts.getMode() != null ? ts.getMode() : "Solo") + "|" + (ts.getCategory() != null ? ts.getCategory() : "Autre"));
            ps.setString(3, ts.getDescription());
            ps.setInt(4, ts.getDuration());
            ps.setString(5, ts.getStatus().name());
            ps.setInt(6, ts.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteTaskSpace(int id) {
        String sqlDeleteTasks = "DELETE FROM tache WHERE task_space_id = ?";
        String sqlDeleteSpace = "DELETE FROM task_space WHERE id = ?";
        
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps1 = connection.prepareStatement(sqlDeleteTasks)) {
                ps1.setInt(1, id);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = connection.prepareStatement(sqlDeleteSpace)) {
                ps2.setInt(1, id);
                ps2.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public List<TaskSpace> getAllTaskSpaces() {
        List<TaskSpace> spaces = new ArrayList<>();
        String sql = "SELECT * FROM task_space";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                spaces.add(mapResultSetToTaskSpace(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return spaces;
    }

    private TaskSpace mapResultSetToTaskSpace(ResultSet rs) throws SQLException {
        TaskSpace ts = new TaskSpace();
        ts.setId(rs.getInt("id"));
        ts.setNom(rs.getString("nom"));
        
        // Split 'type' column value to extract mode and category
        String typeData = rs.getString("type");
        if (typeData != null && typeData.contains("|")) {
            String[] parts = typeData.split("\\|", 2);
            ts.setMode(parts[0]);
            ts.setCategory(parts[1]);
        } else {
            // Support for legacy data or simple strings
            ts.setMode("Solo");
            ts.setCategory(typeData != null ? typeData : "Autre");
        }
        
        ts.setDateCreation(rs.getTimestamp("date_creation"));
        ts.setDescription(rs.getString("description"));
        ts.setDuration(rs.getInt("duration"));
        ts.setStatus(StatutTaskSpace.fromString(rs.getString("status")));
        ts.setUtilisateurId(rs.getInt("utilisateur_id"));
        return ts;
    }
}
