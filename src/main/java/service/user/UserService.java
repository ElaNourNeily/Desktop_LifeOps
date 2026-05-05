package service.user;

import model.user.User;
import utils.MyDatabase;
import service.CRUD;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class UserService implements CRUD<User> {
    private final Connection connection;

    public UserService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void create(User user) throws SQLException {
        final String sql = "INSERT INTO utilisateur (nom, prenom, age, email, mot_de_passe, created_at, is_verified, role, telephone, has_set_password) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        statement.setString(1, user.getNom());
        statement.setString(2, user.getPrenom());
        statement.setInt(3, user.getAge());
        statement.setString(4, user.getEmail());
        statement.setString(5, user.getMot_de_passe());
        statement.setObject(6, user.getCreated_at());
        statement.setInt(7, user.isIs_verified());
        statement.setString(8, user.getRole() != null ? user.getRole() : "ROLE_USER");
        statement.setString(9, user.getTelephone());
        statement.setBoolean(10, user.hasSetPassword());
        
        statement.executeUpdate();
        
        // Get the generated ID
        ResultSet rs = statement.getGeneratedKeys();
        if (rs.next()) {
            user.setId(rs.getInt(1));
        }
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE utilisateur SET nom = ?, prenom = ?, age = ?, email = ?, telephone = ?, role = ? WHERE id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, user.getNom());
        preparedStatement.setString(2, user.getPrenom());
        preparedStatement.setInt(3, user.getAge());
        preparedStatement.setString(4, user.getEmail());
        preparedStatement.setString(5, user.getTelephone());
        preparedStatement.setString(6, user.getRole());
        preparedStatement.setInt(7, user.getId());

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
            user.setRole(rs.getString("role"));
            user.setTelephone(rs.getString("telephone"));
            user.setIs_verified(rs.getInt("is_verified"));
            
            // Handle created_at
            Timestamp timestamp = rs.getTimestamp("created_at");
            if (timestamp != null) {
                user.setCreated_at(timestamp.toLocalDateTime());
            }
            
            // Handle has_set_password (may not exist in older databases)
            try {
                user.setHasSetPassword(rs.getBoolean("has_set_password"));
            } catch (SQLException e) {
                user.setHasSetPassword(true); // default to true if column doesn't exist
            }

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
        if (result != null) {
            String dbPass = result.getMot_de_passe();
            String inputPass = user.getMot_de_passe();
            if (dbPass != null && (dbPass.startsWith("$2a$") || dbPass.startsWith("$2y$"))) {
                String checkPass = dbPass.replaceFirst("^\\$2y\\$", "\\$2a\\$");
                try {
                    return BCrypt.checkpw(inputPass, checkPass);
                } catch (Exception e) {
                    return false;
                }
            } else if (dbPass != null) {
                return dbPass.equals(inputPass);
            }
        }
        return false;
    }

    /**
     * Login method - validates email and password
     */
    public User login(String email, String password) {
        try {
            User user = findbyMail(email);
            if (user != null) {
                String dbPass = user.getMot_de_passe();
                if (dbPass != null && (dbPass.startsWith("$2a$") || dbPass.startsWith("$2y$"))) {
                    String checkPass = dbPass.replaceFirst("^\\$2y\\$", "\\$2a\\$");
                    if (BCrypt.checkpw(password, checkPass)) {
                        return user;
                    }
                } else if (dbPass != null && dbPass.equals(password)) {
                    return user;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get user by email (alias for findbyMail for compatibility)
     */
    public User getUserByEmail(String email) {
        try {
            return findbyMail(email);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Updates only the password for the given user (by id).
     */
    public void updatePassword(User user) throws SQLException {
        String sql = "UPDATE utilisateur SET mot_de_passe = ?, has_set_password = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, user.getMot_de_passe());
        ps.setBoolean(2, true);
        ps.setInt(3, user.getId());
        ps.executeUpdate();
    }

    /**
     * Search users by name or email
     */
    public List<User> searchUsers(String query) {
        try {
            String lowerQuery = query.toLowerCase();
            return findAll().stream()
                    .filter(u -> u.getNom().toLowerCase().contains(lowerQuery) ||
                            u.getPrenom().toLowerCase().contains(lowerQuery) ||
                            u.getEmail().toLowerCase().contains(lowerQuery))
                    .toList();
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        try {
            return findAll();
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get user by ID
     */
    public User getById(int userId) {
        try {
            return findbyID(userId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
