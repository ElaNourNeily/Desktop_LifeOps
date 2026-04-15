package service;

import model.User;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Userservice implements CRUD<User>{
    private final Connection connection;
    public Userservice( ) {
        this.connection = MyDB.getInstance().getConnection();
    }
    @Override
    public void create(User user) throws SQLException {
        final String sql = "insert into utilisateur (nom , prenom , age , email , mot_de_passe , created_at , is_verified )"+ "values( '"+user.getNom()+"','"+ user.getPrenom()+"',"+user.getAge()+" ,'"+user.getEmail()+"','"+user.getMot_de_passe()+"','"+user.getCreated_at()+"',"+user.isIs_verified()+")";
        Statement statement = connection.createStatement();
        statement.executeUpdate(sql);
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "update personne set nom = ? , prenom = ? , age = ? " +"where id = ? ";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, user.getNom());
        preparedStatement.setString(2, user.getPrenom());
        preparedStatement.setInt(3, user.getAge());
        preparedStatement.setInt(4, user.getId());

        preparedStatement.executeUpdate();
    }

    @Override
    public void delete(User user) throws SQLException {
      final String sql =  "DELETE FROM utilisateur WHERE id = ?";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, user.getId());
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
        return  this.findAll().stream().sorted(Comparator.comparing(User::getNom)).toList();
    }
}
