package service;

import model.SuiviSante;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SuiviSanteService implements Crud<SuiviSante> {

    private final Connection connection;

    public SuiviSanteService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(SuiviSante suivi) throws SQLException {
        String sql = "insert into suivi_sante (date, heures_sommeil, qualite_sommeil, verres_eau, minutes_activite, poids, humeur, notes, activite, utilisateur_id) " +
                     "values(?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDate(1, Date.valueOf(suivi.getDate()));
            preparedStatement.setFloat(2, suivi.getHeuresSommeil());
            preparedStatement.setInt(3, suivi.getQualiteSommeil());
            preparedStatement.setInt(4, suivi.getVerresEau());
            preparedStatement.setInt(5, suivi.getMinutesActivite());
            preparedStatement.setFloat(6, suivi.getPoids());
            preparedStatement.setInt(7, suivi.getHumeur());
            preparedStatement.setString(8, suivi.getNotes());
            preparedStatement.setString(9, suivi.getActivite());

            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void modifier(SuiviSante suivi) throws SQLException {
        String sql = "update suivi_sante set date = ?, heures_sommeil = ?, qualite_sommeil = ?, verres_eau = ?, " +
                     "minutes_activite = ?, poids = ?, humeur = ?, notes = ?, activite = ? where id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDate(1, Date.valueOf(suivi.getDate()));
            preparedStatement.setFloat(2, suivi.getHeuresSommeil());
            preparedStatement.setInt(3, suivi.getQualiteSommeil());
            preparedStatement.setInt(4, suivi.getVerresEau());
            preparedStatement.setInt(5, suivi.getMinutesActivite());
            preparedStatement.setFloat(6, suivi.getPoids());
            preparedStatement.setInt(7, suivi.getHumeur());
            preparedStatement.setString(8, suivi.getNotes());
            preparedStatement.setString(9, suivi.getActivite());
            preparedStatement.setInt(10, suivi.getId());

            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from suivi_sante where id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<SuiviSante> recuperer() throws SQLException {
        String sql = "select * from suivi_sante";
        return getSuiviSantes(sql);
    }

    @Override
    public List<SuiviSante> trier() {
        String sql = "select * from suivi_sante order by date desc";
        try {
            return getSuiviSantes(sql);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<SuiviSante> rechercher(String critere) {
        String sql = "select * from suivi_sante where notes like '%" + critere + "%' or activite like '%" + critere + "%'";
        try {
            return getSuiviSantes(sql);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<SuiviSante> getSuiviSantes(String sql) throws SQLException {
        List<SuiviSante> suivis = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                SuiviSante s = new SuiviSante();
                s.setId(rs.getInt("id"));
                if (rs.getDate("date") != null) {
                    s.setDate(rs.getDate("date").toLocalDate());
                }
                s.setHeuresSommeil(rs.getFloat("heures_sommeil"));
                s.setQualiteSommeil(rs.getInt("qualite_sommeil"));
                s.setVerresEau(rs.getInt("verres_eau"));
                s.setMinutesActivite(rs.getInt("minutes_activite"));
                s.setPoids(rs.getFloat("poids"));
                s.setHumeur(rs.getInt("humeur"));
                s.setNotes(rs.getString("notes"));
                s.setActivite(rs.getString("activite"));

                suivis.add(s);
            }
        }
        return suivis;
    }

    public List<SuiviSante> findByPeriode(java.time.LocalDate dateDebut, java.time.LocalDate dateFin) {
        String sql = "select * from suivi_sante where date >= ? and date <= ?";
        List<SuiviSante> suivis = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDate(1, java.sql.Date.valueOf(dateDebut));
            preparedStatement.setDate(2, java.sql.Date.valueOf(dateFin));
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    SuiviSante s = new SuiviSante();
                    s.setId(rs.getInt("id"));
                    if (rs.getDate("date") != null) {
                        s.setDate(rs.getDate("date").toLocalDate());
                    }
                    s.setHeuresSommeil(rs.getFloat("heures_sommeil"));
                    s.setQualiteSommeil(rs.getInt("qualite_sommeil"));
                    s.setVerresEau(rs.getInt("verres_eau"));
                    s.setMinutesActivite(rs.getInt("minutes_activite"));
                    s.setPoids(rs.getFloat("poids"));
                    s.setHumeur(rs.getInt("humeur"));
                    s.setNotes(rs.getString("notes"));
                    s.setActivite(rs.getString("activite"));
                    suivis.add(s);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return suivis;
    }
}
