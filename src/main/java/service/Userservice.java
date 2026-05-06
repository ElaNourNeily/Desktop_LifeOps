package Service;

import model.User;
import utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Userservice implements CRUD<User> {
    private final Connection connection;

    public Userservice() {
        this.connection = MyDB.getInstance().getConnection();
    }

    @Override
    public void create(User user) throws SQLException {
        final String sql = "INSERT INTO utilisateur (nom, prenom, age, email, mot_de_passe, created_at, is_verified, role, telephone) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setInt(3, user.getAge());
        ps.setString(4, user.getEmail());
        ps.setString(5, user.getMot_de_passe());
        ps.setTimestamp(6, Timestamp.valueOf(user.getCreated_at()));
        ps.setInt(7, user.isIs_verified());
        ps.setString(8, user.getRole());
        ps.setString(9, user.getTelephone());
        ps.executeUpdate();
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE utilisateur SET nom = ?, prenom = ?, age = ?, email = ?, telephone = ? WHERE id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, user.getNom());
        preparedStatement.setString(2, user.getPrenom());
        preparedStatement.setInt(3, user.getAge());
        preparedStatement.setString(4, user.getEmail());
        preparedStatement.setString(5, user.getTelephone());
        preparedStatement.setInt(6, user.getId());

        preparedStatement.executeUpdate();
    }

    @Override
    public void delete(User user) throws SQLException {
        final String sql = "DELETE FROM utilisateur WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, user.getId());
        preparedStatement.executeUpdate();
    }

    @Override
    public User findbyID(int userid) throws SQLException {
        return this.findAll().stream().filter(u -> u.getId() == userid).findFirst().orElse(null);
    }

    @Override
    public User findbyMail(String mail) throws SQLException {
        return this.findAll().stream().filter(u -> u.getEmail().equals(mail)).findFirst().orElse(null);
    }

    @Override
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();

        String sql = "SELECT * FROM utilisateur";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setNom(rs.getString("nom"));
            user.setPrenom(rs.getString("prenom"));
            user.setEmail(rs.getString("email"));
            user.setAge(rs.getInt("age"));
            user.setMot_de_passe(rs.getString("mot_de_passe"));
            user.setTelephone(rs.getString("telephone"));
            user.setRole(rs.getString("role"));
            user.setIs_verified(rs.getInt("is_verified"));

            Timestamp banTimestamp = rs.getTimestamp("ban_until");
            if (banTimestamp != null) {
                user.setBanUntil(banTimestamp.toLocalDateTime());
            }

            user.setTotalConnectionTime(rs.getLong("total_connection_time"));
            users.add(user);
        }

        return users;
    }

    @Override
    public List<User> sortbyName() throws SQLException {
        return this.findAll().stream().sorted(Comparator.comparing(User::getNom)).toList();
    }

    @Override
    public boolean recherche(User user) throws SQLException {
        User result = this.findbyMail(user.getEmail());
        if (result != null && result.getMot_de_passe().equals(user.getMot_de_passe())) {
            return true;
        }
        return false;
    }

    public void updatePassword(User user) throws SQLException {
        String sql = "UPDATE utilisateur SET mot_de_passe = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, user.getMot_de_passe());
        ps.setInt(2, user.getId());
        ps.executeUpdate();
    }

    @Override
    public void ban(User user) throws SQLException {
        LocalDateTime banTime = LocalDateTime.now().plusHours(3);
        user.setBanUntil(banTime);

        String sql = "UPDATE utilisateur SET ban_until = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setTimestamp(1, Timestamp.valueOf(banTime));
        ps.setInt(2, user.getId());
        int rows = ps.executeUpdate();
        System.out.println("DEBUG SQL: Ban appliqué en DB (Lignes: " + rows + ") pour l'utilisateur ID: " + user.getId());
    }

    public void unban(User user) throws SQLException {
        user.setBanUntil(null);
        String sql = "UPDATE utilisateur SET ban_until = NULL WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, user.getId());
        ps.executeUpdate();
        System.out.println("DEBUG SQL: Unban appliqué en DB pour l'utilisateur ID: " + user.getId());
    }

    public void updateConnectionTime(User user) throws SQLException {
        String sql = "UPDATE utilisateur SET total_connection_time = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, user.getTotalConnectionTime());
        ps.setInt(2, user.getId());
        ps.executeUpdate();
    }
}
