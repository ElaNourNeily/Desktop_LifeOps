package test;
import utils.MyDatabase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, email, mot_de_passe FROM utilisateur");
            boolean found = false;
            while(rs.next()) {
                String email = rs.getString("email");
                System.out.println("User in DB: " + email + " / pass: " + rs.getString("mot_de_passe"));
                if ("nawnaw@gmail.com".equals(email)) {
                    found = true;
                }
            }
            if (!found) {
                System.out.println("NO USER FOUND WITH nawnaw@gmail.com!");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
