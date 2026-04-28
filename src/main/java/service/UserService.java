package service;

import model.User;
import utils.MyDatabase;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

public class UserService {
    private final Connection connection;

    public UserService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    /**
     * Authenticates a user with email and password.
     * Compatible with Symfony's $2y$ BCrypt hashes.
     */
    public User login(String email, String password) {
        System.out.println("--- Login Debug ---");
        System.out.println("Email provided: " + email);
        
        // Case-insensitive email search
        String sql = "SELECT * FROM utilisateur WHERE LOWER(email) = LOWER(?)";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = mapResultSetToUser(rs);

                System.out.println("User found: " + user.getEmail());
                System.out.println("DB Hash: " + user.getMotDePasse());

                // Special case: Login blocked if password not set (Social Login)
                if (!user.hasSetPassword()) {
                    System.out.println("Login blocked: has_set_password is false.");
                    return null; 
                }

                // BCrypt Compatibility Fix: Symfony uses $2y$, Java needs $2a$
                String hashedPassword = user.getMotDePasse();
                if (hashedPassword != null && hashedPassword.startsWith("$2y$")) {
                    hashedPassword = hashedPassword.replaceFirst("\\$2y\\$", "\\$2a\\$");
                }
                System.out.println("Hash after compat fix: " + hashedPassword);

                // Verify password
                try {
                    boolean matches = BCrypt.checkpw(password, hashedPassword);
                    System.out.println("Match result: " + matches);
                    
                    if (matches) {
                        return user;
                    }
                } catch (Exception e) {
                    System.err.println("Error checking BCrypt password: " + e.getMessage());
                }
            } else {
                System.out.println("No user found with email: " + email);
            }
        } catch (SQLException e) {
            System.err.println("Database error during login: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    public User getUserByEmail(String email) {
        String sql = "SELECT * FROM utilisateur WHERE LOWER(email) = LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public java.util.List<User> getAllUsers() {
        java.util.List<User> users = new java.util.ArrayList<>();
        String sql = "SELECT * FROM utilisateur";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public java.util.List<User> searchUsers(String keyword) {
        java.util.List<User> users = new java.util.ArrayList<>();
        String sql = "SELECT * FROM utilisateur WHERE LOWER(nom) LIKE LOWER(?) OR LOWER(prenom) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?) LIMIT 10";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        user.setMotDePasse(rs.getString("mot_de_passe"));
        user.setHasSetPassword(rs.getBoolean("has_set_password"));
        user.setTelephone(rs.getString("telephone"));
        return user;
    }

    @Deprecated
    public User authenticate(String email, String password) {
        return login(email, password);
    }
}
