package service.user;

import model.user.User;
import service.Crud;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Userservice implements Crud<User> {
    private Connection connection;

    public Userservice() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(User user) throws SQLException {
        final String sql = "insert into utilisateur (nom , prenom , age , email , mot_de_passe , created_at , is_verified )"+ "values( '"+user.getNom()+"','"+ user.getPrenom()+"',"+user.getAge()+" ,'"+user.getEmail()+"','"+user.getMot_de_passe()+"','"+user.getCreated_at()+"',"+user.isIs_verified()+")";
        Statement statement = connection.createStatement();
        statement.executeUpdate(sql);
    }

    @Override
    public void modifier(User user) throws SQLException {
        String sql = "update utilisateur set nom = ? , prenom = ? , age = ? where id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, user.getNom());
        preparedStatement.setString(2, user.getPrenom());
        preparedStatement.setInt(3, user.getAge());
        preparedStatement.setInt(4, user.getId());
        preparedStatement.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        final String sql =  "DELETE FROM utilisateur WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
    }

    @Override
    public List<User> recuperer() throws SQLException {
        return this.findAll();
    }

    @Override
    public List<User> trier() {
        try {
            return this.findAll().stream()
                    .sorted(Comparator.comparing(User::getNom, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<User> rechercher(String critere) {
        try {
            return rechercherUser(critere);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public User findbyID(int userid) throws SQLException {
        return this.findAll().stream().filter(u -> u.getId()==userid).findFirst().orElse(null);
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
            users.add(user);
        }
        return users;
    }

    @Override
    public List<User> sortbyName() throws SQLException {
        return this.findAll().stream()
                .sorted(Comparator.comparing(User::getNom, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    // ⭐ Custom method for InviteTaskSpace Search bar
    public List<User> rechercherUser(String query) throws SQLException {
        List<User> users = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return users;
        }

        String sql = "SELECT * FROM utilisateur WHERE LOWER(nom) LIKE ? OR LOWER(prenom) LIKE ? OR LOWER(email) LIKE ?";
        PreparedStatement ps = connection.prepareStatement(sql);

        String searchPattern = "%" + query.toLowerCase() + "%";
        ps.setString(1, searchPattern);
        ps.setString(2, searchPattern);
        ps.setString(3, searchPattern);

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
}