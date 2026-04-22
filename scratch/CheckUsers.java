import utils.MyDatabase;
import java.sql.*;

public class CheckUsers {
    public static void main(String[] args) {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM utilisateur");
            boolean found = false;
            while (rs.next()) {
                System.out.println("User ID: " + rs.getInt("id"));
                found = true;
            }
            if (!found) {
                System.out.println("No users found in 'utilisateur' table.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
