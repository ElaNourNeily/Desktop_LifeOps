package service;

import model.task.Tache;
import enums.PrioriteTache;
import enums.StatutTache;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class TaskService implements CRUD<Tache> {

    private final Connection connection;
    private final PusherService pusherService = new PusherService();

    public TaskService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void add(Tache t) {
        if (t.getDescription() == null) {
            t.setDescription("");
        }
        String sql = "INSERT INTO tache (titre, description, priorite, difficulte, statut, deadline, task_space_id, utilisateur_id, assigned_user_id, toggl_entry_id, start_time, end_time, real_time_spent_seconds, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            if (t.getTogglEntryId() != null) ps.setLong(10, t.getTogglEntryId()); else ps.setNull(10, Types.BIGINT);
            ps.setTimestamp(11, t.getStartTime() != null ? new java.sql.Timestamp(t.getStartTime().getTime()) : null);
            ps.setTimestamp(12, t.getEndTime() != null ? new java.sql.Timestamp(t.getEndTime().getTime()) : null);
            if (t.getRealTimeSpentSeconds() != null) ps.setInt(13, t.getRealTimeSpentSeconds()); else ps.setNull(13, Types.INTEGER);
            ps.setTimestamp(14, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(15, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                t.setId(rs.getInt(1));
            }

            // realtime: task-created
            triggerTaskEvent("task-created", t, null);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Tache t) {
        if (t.getDescription() == null) {
            t.setDescription("");
        }
        String sql = "UPDATE tache SET titre=?, description=?, priorite=?, difficulte=?, statut=?, deadline=?, assigned_user_id=?, toggl_entry_id=?, start_time=?, end_time=?, real_time_spent_seconds=?, updated_at=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, t.getTitre());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getPriorite().name().toLowerCase()); 
            ps.setInt(4, t.getDifficulte());
            ps.setString(5, t.getStatut().getValeur()); 
            ps.setTimestamp(6, t.getDeadline() != null ? new java.sql.Timestamp(t.getDeadline().getTime()) : null);
            if (t.getAssignedUserId() != null) ps.setInt(7, t.getAssignedUserId()); else ps.setNull(7, Types.INTEGER);
            if (t.getTogglEntryId() != null) ps.setLong(8, t.getTogglEntryId()); else ps.setNull(8, Types.BIGINT);
            ps.setTimestamp(9, t.getStartTime() != null ? new java.sql.Timestamp(t.getStartTime().getTime()) : null);
            ps.setTimestamp(10, t.getEndTime() != null ? new java.sql.Timestamp(t.getEndTime().getTime()) : null);
            if (t.getRealTimeSpentSeconds() != null) ps.setInt(11, t.getRealTimeSpentSeconds()); else ps.setNull(11, Types.INTEGER);
            ps.setTimestamp(12, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setInt(13, t.getId());
            ps.executeUpdate();

            // realtime: task-updated (and clients can interpret status changes as moves)
            triggerTaskEvent("task-updated", t, null);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        Tache existing = getById(id);
        String sql = "DELETE FROM tache WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();

            if (existing != null) {
                // realtime: task-deleted
                triggerTaskEvent("task-deleted", existing, Map.of("taskId", id));
            }
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

    public Tache getById(int id) {
        String sql = "SELECT * FROM tache WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSetToTache(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Tache> readAll() {
        List<Tache> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tache";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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

        long togglId = rs.getLong("toggl_entry_id");
        if (!rs.wasNull()) t.setTogglEntryId(togglId);
        t.setStartTime(rs.getTimestamp("start_time"));
        t.setEndTime(rs.getTimestamp("end_time"));
        int spent = rs.getInt("real_time_spent_seconds");
        if (!rs.wasNull()) t.setRealTimeSpentSeconds(spent);
        return t;
    }

    private void triggerTaskEvent(String event, Tache t, Map<String, Object> extra) {
        if (t == null || !pusherService.isEnabled()) return;
        String channel = pusherService.channelForBoard(t.getTaskSpaceId());
        Map<String, Object> data = new HashMap<>();
        data.put("event", event);
        data.put("boardId", t.getTaskSpaceId());
        data.put("taskId", t.getId());
        data.put("userId", utils.Session.isLoggedIn() ? utils.Session.getCurrentUser().getId() : null);

        Map<String, Object> fields = new HashMap<>();
        fields.put("titre", t.getTitre());
        fields.put("description", t.getDescription());
        fields.put("priorite", t.getPriorite() != null ? t.getPriorite().name() : null);
        fields.put("difficulte", t.getDifficulte());
        fields.put("statut", t.getStatut() != null ? t.getStatut().getValeur() : null);
        fields.put("deadline", t.getDeadline() != null ? t.getDeadline().getTime() : null);
        fields.put("assignedUserId", t.getAssignedUserId());
        fields.put("togglEntryId", t.getTogglEntryId());
        fields.put("startTime", t.getStartTime() != null ? t.getStartTime().getTime() : null);
        fields.put("endTime", t.getEndTime() != null ? t.getEndTime().getTime() : null);
        fields.put("realTimeSpentSeconds", t.getRealTimeSpentSeconds());
        data.put("fields", fields);

        if (extra != null) data.putAll(extra);
        pusherService.triggerEvent(channel, event, data);
    }

    public void assignTaskToUser(Tache task, model.User user) {
        task.setAssignedUserId(user.getId());
        update(task);
    }
}
