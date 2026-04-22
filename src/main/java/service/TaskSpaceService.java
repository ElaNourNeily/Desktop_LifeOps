package service;

import model.task.TaskSpace;
import enums.StatutTaskSpace;
import utils.MyDatabase;
import utils.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskSpaceService {

    private final Connection connection;
    private final TaskSpaceUserService spaceUserService = new TaskSpaceUserService();

    public TaskSpaceService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    public void addTaskSpace(TaskSpace ts) {
        // Updated SQL to include both leader_id and utilisateur_id to satisfy DB constraints
        // We use a safe check for leader_id column in case it's not yet migrated, but utilisateur_id is required
        String sql = "INSERT INTO task_space (nom, type, date_creation, description, duration, status, leader_id, utilisateur_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ts.getNom());
            ps.setString(2, (ts.getMode() != null ? ts.getMode() : "Solo") + "|" + (ts.getCategory() != null ? ts.getCategory() : "Autre"));
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setString(4, ts.getDescription());
            ps.setInt(5, ts.getDuration());
            ps.setString(6, ts.getStatus().name());
            ps.setInt(7, ts.getLeaderId());
            ps.setInt(8, ts.getUtilisateurId()); // This column is NOT NULL in DB
            
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                ts.setId(id);
                // Automatically add creator as LEADER in taskspace_user table
                spaceUserService.addMember(id, ts.getLeaderId(), "LEADER");
            }
        } catch (SQLException e) {
            System.err.println("Error adding task space: " + e.getMessage());
            // Fallback for older schemas (without leader_id)
            if (e.getMessage().contains("Unknown column 'leader_id'")) {
                retryWithLegacySql(ts);
            } else {
                e.printStackTrace();
            }
        }
    }

    private void retryWithLegacySql(TaskSpace ts) {
        String sql = "INSERT INTO task_space (nom, type, date_creation, description, duration, status, utilisateur_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ts.getNom());
            ps.setString(2, (ts.getMode() != null ? ts.getMode() : "Solo") + "|" + (ts.getCategory() != null ? ts.getCategory() : "Autre"));
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setString(4, ts.getDescription());
            ps.setInt(5, ts.getDuration());
            ps.setString(6, ts.getStatus().name());
            ps.setInt(7, ts.getUtilisateurId());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                ts.setId(id);
                spaceUserService.addMember(id, ts.getUtilisateurId(), "LEADER");
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
        String sqlDeleteMembers = "DELETE FROM taskspace_user WHERE taskspace_id = ?";
        String sqlDeleteTasks = "DELETE FROM tache WHERE task_space_id = ?";
        String sqlDeleteSpace = "DELETE FROM task_space WHERE id = ?";
        
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps0 = connection.prepareStatement(sqlDeleteMembers)) {
                ps0.setInt(1, id);
                ps0.executeUpdate();
            }
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

    public List<TaskSpace> getTaskSpacesForUser(int userId) {
        List<TaskSpace> spaces = new ArrayList<>();
        // Get boards where user is either the leader OR a member
        String sql = "SELECT ts.* FROM task_space ts " +
                     "LEFT JOIN taskspace_user tu ON ts.id = tu.taskspace_id " +
                     "WHERE ts.utilisateur_id = ? OR tu.user_id = ? GROUP BY ts.id";
        
        // Dynamic search for leader_id if it exists
        try {
            DatabaseMetaData dbm = connection.getMetaData();
            ResultSet rsCol = dbm.getColumns(null, null, "task_space", "leader_id");
            if (rsCol.next()) {
                sql = "SELECT ts.* FROM task_space ts " +
                      "LEFT JOIN taskspace_user tu ON ts.id = tu.taskspace_id " +
                      "WHERE ts.leader_id = ? OR ts.utilisateur_id = ? OR tu.user_id = ? GROUP BY ts.id";
            }
        } catch (Exception e) {}

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            if (sql.contains("leader_id")) ps.setInt(3, userId);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                spaces.add(mapResultSetToTaskSpace(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return spaces;
    }

    public List<TaskSpace> getAllTaskSpaces() {
        if (Session.isLoggedIn()) {
            return getTaskSpacesForUser(Session.getCurrentUser().getId());
        }
        return new ArrayList<>();
    }

    private TaskSpace mapResultSetToTaskSpace(ResultSet rs) throws SQLException {
        TaskSpace ts = new TaskSpace();
        ts.setId(rs.getInt("id"));
        ts.setNom(rs.getString("nom"));
        
        String typeData = rs.getString("type");
        if (typeData != null && typeData.contains("|")) {
            String[] parts = typeData.split("\\|", 2);
            ts.setMode(parts[0]);
            ts.setCategory(parts[1]);
        } else {
            ts.setMode("Solo");
            ts.setCategory(typeData != null ? typeData : "Autre");
        }
        
        ts.setDateCreation(rs.getTimestamp("date_creation"));
        ts.setDescription(rs.getString("description"));
        ts.setDuration(rs.getInt("duration"));
        ts.setStatus(StatutTaskSpace.fromString(rs.getString("status")));
        
        ts.setUtilisateurId(rs.getInt("utilisateur_id"));
        try {
            ts.setLeaderId(rs.getInt("leader_id"));
        } catch (SQLException e) {
            ts.setLeaderId(ts.getUtilisateurId());
        }
        return ts;
    }
}
