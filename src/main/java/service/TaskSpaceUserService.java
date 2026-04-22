package service;

import model.User;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskSpaceUserService {
    private final Connection connection;

    public TaskSpaceUserService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    public void addMember(int taskSpaceId, int userId, String role) {
        String sql = "INSERT INTO taskspace_user (taskspace_id, user_id, role) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, taskSpaceId);
            ps.setInt(2, userId);
            ps.setString(3, role);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<User> getMembersByBoard(int taskSpaceId) {
        List<User> members = new ArrayList<>();
        String sql = "SELECT u.* FROM utilisateur u JOIN taskspace_user tu ON u.id = tu.user_id WHERE tu.taskspace_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, taskSpaceId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setRole(rs.getString("role"));
                members.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public String getUserRoleInBoard(int userId, int taskSpaceId) {
        String sql = "SELECT role FROM taskspace_user WHERE user_id = ? AND taskspace_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, taskSpaceId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Not a member
    }
}
