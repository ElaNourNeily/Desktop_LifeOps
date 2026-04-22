import java.sql.*;

public class DBCheck {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/life_ops_symfony";
        String user = "root";
        String password = "";
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connection successful!");
            
            // Check Users
            ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM utilisateur LIMIT 5");
            while (rs.next()) {
                System.out.println("Found User ID: " + rs.getInt("id"));
            }
            
            // Check Plannings
            rs = conn.createStatement().executeQuery("SELECT id, date, utilisateur_id FROM planning ORDER BY id DESC LIMIT 5");
            while (rs.next()) {
                System.out.println("Planning ID: " + rs.getInt("id") + " | Date: " + rs.getDate("date") + " | User: " + rs.getInt("utilisateur_id"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
