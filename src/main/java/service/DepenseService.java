package service;

import model.Depense;
import utils.MyDatabase;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DepenseService implements Crud<Depense> {

    private final Connection connection;

    public DepenseService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Depense depense) throws SQLException {
        String sql = "INSERT INTO depense (titre, montant, categorie, date, type_paiement, utilisateur_id, budget_id, is_important, phone_number, sms_sent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, depense.getTitre());
        ps.setDouble(2, depense.getMontant());
        ps.setString(3, depense.getCategorie());
        ps.setDate(4, new Date(depense.getDate().getTime()));
        ps.setString(5, depense.getTypePaiement());
        ps.setInt(6, depense.getUtilisateurId());
        ps.setInt(7, depense.getBudgetId());
        ps.setBoolean(8, depense.isImportant());
        ps.setString(9, depense.getPhoneNumber());
        ps.setBoolean(10, depense.isSmsSent());
        ps.executeUpdate();
        System.out.println("Depense ajoutee : " + depense.getTitre());
    }

    @Override
    public void modifier(Depense depense) throws SQLException {
        String sql = "UPDATE depense SET titre=?, montant=?, categorie=?, date=?, type_paiement=?, utilisateur_id=?, budget_id=?, is_important=?, phone_number=?, sms_sent=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, depense.getTitre());
        ps.setDouble(2, depense.getMontant());
        ps.setString(3, depense.getCategorie());
        ps.setDate(4, new Date(depense.getDate().getTime()));
        ps.setString(5, depense.getTypePaiement());
        ps.setInt(6, depense.getUtilisateurId());
        ps.setInt(7, depense.getBudgetId());
        ps.setBoolean(8, depense.isImportant());
        ps.setString(9, depense.getPhoneNumber());
        ps.setBoolean(10, depense.isSmsSent());
        ps.setInt(11, depense.getId());
        ps.executeUpdate();
        System.out.println("Depense modifiee : id=" + depense.getId());
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM depense WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Depense supprimee : id=" + id);
    }

    @Override
    public List<Depense> recuperer() throws SQLException {
        String sql = "SELECT * FROM depense";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Depense> depenses = new ArrayList<>();
        while (rs.next()) {
            depenses.add(mapRow(rs));
        }
        return depenses;
    }

    public List<Depense> recupererParUtilisateur(int utilisateurId) throws SQLException {
        String sql = "SELECT * FROM depense WHERE utilisateur_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();

        List<Depense> depenses = new ArrayList<>();
        while (rs.next()) {
            depenses.add(mapRow(rs));
        }
        return depenses;
    }

    public List<Depense> recupererParBudget(int budgetId) throws SQLException {
        String sql = "SELECT * FROM depense WHERE budget_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, budgetId);
        ResultSet rs = ps.executeQuery();

        List<Depense> depenses = new ArrayList<>();
        while (rs.next()) {
            depenses.add(mapRow(rs));
        }
        return depenses;
    }

    private Depense mapRow(ResultSet rs) throws SQLException {
        Depense depense = new Depense();
        depense.setId(rs.getInt("id"));
        depense.setTitre(rs.getString("titre"));
        depense.setMontant(rs.getDouble("montant"));
        depense.setCategorie(rs.getString("categorie"));
        depense.setDate(rs.getDate("date"));
        depense.setTypePaiement(rs.getString("type_paiement"));
        depense.setUtilisateurId(rs.getInt("utilisateur_id"));
        depense.setBudgetId(rs.getInt("budget_id"));
        depense.setImportant(rs.getBoolean("is_important"));
        depense.setPhoneNumber(rs.getString("phone_number"));
        depense.setSmsSent(rs.getBoolean("sms_sent"));
        return depense;
    }

    @Override
    public List<Depense> trier() {
        try {
            return recuperer().stream()
                    .sorted(Comparator.comparingDouble(Depense::getMontant))
                    .toList();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Depense> rechercher(String critere) {
        try {
            return recuperer().stream()
                    .filter(depense -> depense.getTitre() != null
                            && depense.getTitre().toLowerCase().contains(critere.toLowerCase()))
                    .toList();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }
}
