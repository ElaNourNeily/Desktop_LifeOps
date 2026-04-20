package service.task;

import enums.StatutTaskSpace;
import enums.TypeTaskSpace;
import model.task.TaskSpace;
import service.Crud;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskSpaceService implements Crud<TaskSpace> {

    private Connection connection;

    public TaskSpaceService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(TaskSpace ts) throws SQLException {
        String sql = "INSERT INTO task_space (nom, type, date_creation, description, duration, status, utilisateur_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, ts.getNom());
        ps.setString(2, ts.getType().getValeur());
        ps.setDate(3, new java.sql.Date(ts.getDateCreation().getTime()));
        ps.setString(4, ts.getDescription());
        ps.setInt(5, ts.getDuration());
        ps.setString(6, ts.getStatus().getValeur());
        ps.setInt(7, ts.getUtilisateurId());

        ps.executeUpdate();
        System.out.println("✅ TaskSpace ajouté : " + ts.getNom());
    }

    @Override
    public List<TaskSpace> recuperer() throws SQLException {
        String sql = "SELECT * FROM task_space";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<TaskSpace> liste = new ArrayList<>();
        while (rs.next()) {
            TaskSpace ts = new TaskSpace();
            ts.setId(rs.getInt("id"));
            ts.setNom(rs.getString("nom"));
            ts.setType(TypeTaskSpace.fromString(rs.getString("type")));
            ts.setDateCreation(rs.getDate("date_creation"));
            ts.setDescription(rs.getString("description"));
            ts.setDuration(rs.getInt("duration"));
            ts.setStatus(StatutTaskSpace.fromString(rs.getString("status")));
            ts.setUtilisateurId(rs.getInt("utilisateur_id"));
            liste.add(ts);
        }
        return liste;
    }

    public List<TaskSpace> recupererParUtilisateur(int utilisateurId) throws SQLException {
        String sql = "SELECT * FROM task_space WHERE utilisateur_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();

        List<TaskSpace> liste = new ArrayList<>();
        while (rs.next()) {
            TaskSpace ts = new TaskSpace();
            ts.setId(rs.getInt("id"));
            ts.setNom(rs.getString("nom"));
            ts.setType(TypeTaskSpace.fromString(rs.getString("type")));
            ts.setDateCreation(rs.getDate("date_creation"));
            ts.setDescription(rs.getString("description"));
            ts.setDuration(rs.getInt("duration"));
            ts.setStatus(StatutTaskSpace.fromString(rs.getString("status")));
            ts.setUtilisateurId(rs.getInt("utilisateur_id"));
            liste.add(ts);
        }
        return liste;
    }

    public List<TaskSpace> recupererProjetsMembre(int utilisateurId) throws SQLException {
        String sql = "SELECT ts.* FROM task_space ts JOIN task_space_membre tsm ON ts.id = tsm.task_space_id WHERE tsm.utilisateur_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();

        List<TaskSpace> liste = new ArrayList<>();
        while (rs.next()) {
            TaskSpace ts = new TaskSpace();
            ts.setId(rs.getInt("id"));
            ts.setNom(rs.getString("nom"));
            ts.setType(TypeTaskSpace.fromString(rs.getString("type")));
            ts.setDateCreation(rs.getDate("date_creation"));
            ts.setDescription(rs.getString("description"));
            ts.setDuration(rs.getInt("duration"));
            ts.setStatus(StatutTaskSpace.fromString(rs.getString("status")));
            ts.setUtilisateurId(rs.getInt("utilisateur_id"));
            liste.add(ts);
        }
        return liste;
    }

    @Override
    public void modifier(TaskSpace ts) throws SQLException {
        String sql = "UPDATE task_space SET nom=?, type=?, date_creation=?, description=?, duration=?, status=? " +
                "WHERE id=?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, ts.getNom());
        ps.setString(2, ts.getType().getValeur());
        ps.setDate(3, new java.sql.Date(ts.getDateCreation().getTime()));
        ps.setString(4, ts.getDescription());
        ps.setInt(5, ts.getDuration());
        ps.setString(6, ts.getStatus().getValeur());
        ps.setInt(7, ts.getId());

        ps.executeUpdate();
        System.out.println("✅ TaskSpace modifié : id=" + ts.getId());
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM task_space WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("✅ TaskSpace supprimé : id=" + id);
    }

    @Override
    public List<TaskSpace> rechercher(String type) {
        try {
            return recuperer().stream()
                    .filter(ts -> ts.getType().getValeur().equalsIgnoreCase(type))
                    .toList();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public TaskSpace findbyID(int i) throws SQLException {
        return null;
    }

    @Override
    public TaskSpace findbyMail(String mail) throws SQLException {
        return null;
    }

    @Override
    public List<TaskSpace> findAll() throws SQLException {
        return List.of();
    }

    @Override
    public List<TaskSpace> sortbyName() throws SQLException {
        return List.of();
    }

    @Override
    public List<TaskSpace> trier() {
        try {
            return recuperer().stream()
                    .sorted((ts1, ts2) -> ts1.getNom().compareToIgnoreCase(ts2.getNom()))
                    .toList();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }
}