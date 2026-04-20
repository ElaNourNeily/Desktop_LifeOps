package service.task;

import model.user.User;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TaskSpaceMembreService {
    private Connection connection;

    public TaskSpaceMembreService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouterMembre(int taskSpaceId, int utilisateurId) throws SQLException {
        String sql = "INSERT INTO task_space_membre (task_space_id, utilisateur_id) VALUES (?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, taskSpaceId);
        ps.setInt(2, utilisateurId);
        ps.executeUpdate();
        System.out.println("✅ Membre ajouté : user_id=" + utilisateurId + " au projet=" + taskSpaceId);
    }

    public void retirerMembre(int taskSpaceId, int utilisateurId) throws SQLException {
        String sql = "DELETE FROM task_space_membre WHERE task_space_id = ? AND utilisateur_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, taskSpaceId);
        ps.setInt(2, utilisateurId);
        ps.executeUpdate();
        System.out.println("✅ Membre retiré : user_id=" + utilisateurId + " du projet=" + taskSpaceId);
    }

    public List<User> recupererMembres(int taskSpaceId) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.* FROM utilisateur u " +
                "JOIN task_space_membre tsm ON u.id = tsm.utilisateur_id " +
                "WHERE tsm.task_space_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, taskSpaceId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setNom(rs.getString("nom"));
            user.setPrenom(rs.getString("prenom"));
            user.setEmail(rs.getString("email"));
            user.setAge(rs.getInt("age"));
            users.add(user);
        }
        return users;
    }

    public boolean isMembre(int taskSpaceId, int utilisateurId) throws SQLException {
        String sql = "SELECT 1 FROM task_space_membre WHERE task_space_id = ? AND utilisateur_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, taskSpaceId);
        ps.setInt(2, utilisateurId);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }
}