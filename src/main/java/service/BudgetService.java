package service;

import model.Budget;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class BudgetService implements Crud<Budget> {

    private static final Pattern MOIS_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])-\\d{4}$");
    private final Connection connection;

    public BudgetService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Budget budget) throws SQLException {
        validateBudget(budget, false);
        String sql = "INSERT INTO budget (revenu_mensuel, plafond, mois, economies, utilisateur_id) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setDouble(1, budget.getRevenuMensuel());
        ps.setDouble(2, budget.getPlafond());
        ps.setString(3, budget.getMois());
        ps.setDouble(4, budget.getEconomies());
        ps.setInt(5, budget.getUtilisateurId());
        ps.executeUpdate();
        System.out.println("Budget ajoute : " + budget.getMois());
    }

    @Override
    public void modifier(Budget budget) throws SQLException {
        validateBudget(budget, true);
        String sql = "UPDATE budget SET revenu_mensuel=?, plafond=?, mois=?, economies=?, utilisateur_id=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setDouble(1, budget.getRevenuMensuel());
        ps.setDouble(2, budget.getPlafond());
        ps.setString(3, budget.getMois());
        ps.setDouble(4, budget.getEconomies());
        ps.setInt(5, budget.getUtilisateurId());
        ps.setInt(6, budget.getId());
        ps.executeUpdate();
        System.out.println("Budget modifie : id=" + budget.getId());
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM budget WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Budget supprime : id=" + id);
    }

    @Override
    public List<Budget> recuperer() throws SQLException {
        String sql = "SELECT * FROM budget";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Budget> budgets = new ArrayList<>();
        while (rs.next()) {
            budgets.add(mapRow(rs));
        }
        return budgets;
    }

    private void validateBudget(Budget budget, boolean isUpdate) throws SQLException {
        if (budget.getMois() == null) {
            throw new IllegalArgumentException("Le champ mois est obligatoire.");
        }

        String normalizedMois = budget.getMois().trim();
        if (!MOIS_PATTERN.matcher(normalizedMois).matches()) {
            throw new IllegalArgumentException("Le mois doit respecter le format MM-YYYY, par exemple 01-2026.");
        }

        budget.setMois(normalizedMois);

        if (budgetExistsForMonth(normalizedMois, budget.getUtilisateurId(), isUpdate ? budget.getId() : null)) {
            throw new IllegalArgumentException("Un budget existe deja pour le mois " + normalizedMois + ".");
        }
    }

    private boolean budgetExistsForMonth(String mois, int utilisateurId, Integer excludedId) throws SQLException {
        String sql = "SELECT id FROM budget WHERE mois = ? AND utilisateur_id = ?";
        if (excludedId != null) {
            sql += " AND id <> ?";
        }

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, mois);
        ps.setInt(2, utilisateurId);
        if (excludedId != null) {
            ps.setInt(3, excludedId);
        }

        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    public List<Budget> recupererParUtilisateur(int utilisateurId) throws SQLException {
        String sql = "SELECT * FROM budget WHERE utilisateur_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();

        List<Budget> budgets = new ArrayList<>();
        while (rs.next()) {
            budgets.add(mapRow(rs));
        }
        return budgets;
    }

    private Budget mapRow(ResultSet rs) throws SQLException {
        Budget budget = new Budget();
        budget.setId(rs.getInt("id"));
        budget.setRevenuMensuel(rs.getDouble("revenu_mensuel"));
        budget.setPlafond(rs.getDouble("plafond"));
        budget.setMois(rs.getString("mois"));
        budget.setEconomies(rs.getDouble("economies"));
        budget.setUtilisateurId(rs.getInt("utilisateur_id"));
        return budget;
    }

    @Override
    public List<Budget> trier() {
        try {
            return recuperer().stream()
                    .sorted(Comparator.comparing(Budget::getMois, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Budget> rechercher(String critere) {
        try {
            return recuperer().stream()
                    .filter(budget -> budget.getMois() != null
                            && budget.getMois().toLowerCase().contains(critere.toLowerCase()))
                    .toList();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }
}
