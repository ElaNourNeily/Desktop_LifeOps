package test;

import model.Activite;
import model.Planning;
import service.ActiviteService;
import service.PlanningService;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        PlanningService ps = new PlanningService();
        ActiviteService as = new ActiviteService();

        // Robust user detection to satisfy FK constraint
        int validUserId = 1;
        try {
            java.sql.Connection conn = utils.MyDatabase.getInstance().getConnection();
            java.sql.ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM utilisateur LIMIT 1");
            if (rs.next()) {
                validUserId = rs.getInt("id");
                System.out.println("Environment OK: Using existing user (ID: " + validUserId + ").");
            } else {
                String setupUser = "INSERT INTO utilisateur (email, roles, password) VALUES ('test@lifeops.tn', '[]', 'password')";
                java.sql.PreparedStatement psUser = conn.prepareStatement(setupUser, java.sql.Statement.RETURN_GENERATED_KEYS);
                psUser.executeUpdate();
                java.sql.ResultSet rsKey = psUser.getGeneratedKeys();
                if (rsKey.next()) {
                    validUserId = rsKey.getInt(1);
                    System.out.println("Environment OK: New test user created (ID: " + validUserId + ").");
                }
            }
        } catch (SQLException e) {
            System.out.println("Warning: Could not auto-detect/create user. Using ID 1 as fallback. Error: " + e.getMessage());
        }

        try {
            // 1. Test Planning CRUD
            System.out.println("--- Testing Planning CRUD ---");
            Planning planning = new Planning(
                    Date.valueOf("2026-04-10"),
                    true,
                    Time.valueOf("08:00:00"),
                    Time.valueOf("18:00:00"),
                    validUserId
            );
            ps.ajouter(planning);
            System.out.println("Added Planning: " + planning);

            // 2. Test Activite CRUD
            System.out.println("\n--- Testing Activite CRUD ---");
            Activite activite = new Activite(
                    "Reunion d'equipe",
                    60,
                    1,
                    "en_attente",
                    Time.valueOf("09:00:00"),
                    Time.valueOf("10:00:00"),
                    "moyen",
                    "Travail",
                    "#FF5733",
                    true,
                    planning.getId()
            );
            as.ajouter(activite);
            System.out.println("Added Activite: " + activite);

            // 3. Dynamic Status check
            System.out.println("\n--- Dynamic Status Check ---");
            System.out.println("Activite Status: " + activite.getStatutDynamique(planning.getDate()));

            // 4. Retrieve and Update
            List<Planning> allPlannings = ps.recuperer();
            System.out.println("\nAll Plannings: " + allPlannings);

            activite.setTitre("Reunion d'equipe (Modifie)");
            as.modifier(activite);
            System.out.println("Updated Activite: " + activite);

            // List activities for the planning
            List<Activite> activites = as.recupererParPlanning(planning.getId());
            System.out.println("Activities for Planning #" + planning.getId() + ": " + activites);

        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
