package service.health;

import Model.health.SuiviSante;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SuiviSanteService {

    private final Connection connection;

    public SuiviSanteService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(SuiviSante s) throws SQLException {
        String sql = "INSERT INTO suivi_sante (date, heures_sommeil, qualite_sommeil, verres_eau, " +
                     "minutes_activite, poids, humeur, notes, activite, utilisateur_id) VALUES (?,?,?,?,?,?,?,?,?,1)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(s.getDate()));
            ps.setFloat(2, s.getHeuresSommeil());
            ps.setInt(3, s.getQualiteSommeil());
            ps.setInt(4, s.getVerresEau());
            ps.setInt(5, s.getMinutesActivite());
            ps.setFloat(6, s.getPoids());
            ps.setInt(7, s.getHumeur());
            ps.setString(8, s.getNotes());
            ps.setString(9, s.getActivite());
            ps.executeUpdate();
        }
    }

    public void modifier(SuiviSante s) throws SQLException {
        String sql = "UPDATE suivi_sante SET date=?, heures_sommeil=?, qualite_sommeil=?, verres_eau=?, " +
                     "minutes_activite=?, poids=?, humeur=?, notes=?, activite=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(s.getDate()));
            ps.setFloat(2, s.getHeuresSommeil());
            ps.setInt(3, s.getQualiteSommeil());
            ps.setInt(4, s.getVerresEau());
            ps.setInt(5, s.getMinutesActivite());
            ps.setFloat(6, s.getPoids());
            ps.setInt(7, s.getHumeur());
            ps.setString(8, s.getNotes());
            ps.setString(9, s.getActivite());
            ps.setInt(10, s.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM suivi_sante WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<SuiviSante> recuperer() throws SQLException {
        return query("SELECT * FROM suivi_sante ORDER BY date DESC");
    }

    public List<SuiviSante> findByPeriode(java.time.LocalDate debut, java.time.LocalDate fin) {
        List<SuiviSante> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM suivi_sante WHERE date >= ? AND date <= ?")) {
            ps.setDate(1, Date.valueOf(debut));
            ps.setDate(2, Date.valueOf(fin));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    private List<SuiviSante> query(String sql) throws SQLException {
        List<SuiviSante> list = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private SuiviSante map(ResultSet rs) throws SQLException {
        SuiviSante s = new SuiviSante();
        s.setId(rs.getInt("id"));
        if (rs.getDate("date") != null) s.setDate(rs.getDate("date").toLocalDate());
        s.setHeuresSommeil(rs.getFloat("heures_sommeil"));
        s.setQualiteSommeil(rs.getInt("qualite_sommeil"));
        s.setVerresEau(rs.getInt("verres_eau"));
        s.setMinutesActivite(rs.getInt("minutes_activite"));
        s.setPoids(rs.getFloat("poids"));
        s.setHumeur(rs.getInt("humeur"));
        s.setNotes(rs.getString("notes"));
        s.setActivite(rs.getString("activite"));
        return s;
    }
}
