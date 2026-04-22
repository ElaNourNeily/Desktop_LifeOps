import java.sql.*;
import utils.MyDatabase;

public class CheckPlannings {
    public static void main(String[] args) {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            String sql = "SELECT * FROM planning";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            System.out.println("ID | Date | UserID");
            System.out.println("-------------------");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + " | " + rs.getDate("date") + " | " + rs.getInt("utilisateur_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
