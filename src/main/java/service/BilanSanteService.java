package service;

import model.BilanSante;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BilanSanteService implements Crud<BilanSante> {

    private final Connection connection;

    public BilanSanteService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(BilanSante bilan) throws SQLException {
        String sql = "insert into bilan_sante (date_debut, date_fin, niveau_fatigue, niveau_stress, score_forme, risque_burnout, recommandations, utilisateur_id) " +
                     "values(?, ?, ?, ?, ?, ?, ?, 1)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDate(1, bilan.getDateDebut() != null ? Date.valueOf(bilan.getDateDebut()) : null);
            preparedStatement.setDate(2, bilan.getDateFin() != null ? Date.valueOf(bilan.getDateFin()) : null);
            preparedStatement.setInt(3, bilan.getNiveauFatigue());
            preparedStatement.setInt(4, bilan.getNiveauStress());
            preparedStatement.setFloat(5, bilan.getScoreForme());
            preparedStatement.setBoolean(6, bilan.isRisqueBurnout());
            preparedStatement.setString(7, bilan.getRecommandations());

            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void modifier(BilanSante bilan) throws SQLException {
        String sql = "update bilan_sante set date_debut = ?, date_fin = ?, niveau_fatigue = ?, niveau_stress = ?, " +
                     "score_forme = ?, risque_burnout = ?, recommandations = ? where id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDate(1, bilan.getDateDebut() != null ? Date.valueOf(bilan.getDateDebut()) : null);
            preparedStatement.setDate(2, bilan.getDateFin() != null ? Date.valueOf(bilan.getDateFin()) : null);
            preparedStatement.setInt(3, bilan.getNiveauFatigue());
            preparedStatement.setInt(4, bilan.getNiveauStress());
            preparedStatement.setFloat(5, bilan.getScoreForme());
            preparedStatement.setBoolean(6, bilan.isRisqueBurnout());
            preparedStatement.setString(7, bilan.getRecommandations());
            preparedStatement.setInt(8, bilan.getId());

            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from bilan_sante where id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<BilanSante> recuperer() throws SQLException {
        String sql = "select * from bilan_sante";
        return getBilanSantes(sql);
    }

    @Override
    public List<BilanSante> trier() {
        String sql = "select * from bilan_sante order by date_debut desc";
        try {
            return getBilanSantes(sql);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<BilanSante> rechercher(String critere) {
        String sql = "select * from bilan_sante where recommandations like '%" + critere + "%'";
        try {
            return getBilanSantes(sql);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<BilanSante> getBilanSantes(String sql) throws SQLException {
        List<BilanSante> bilans = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                BilanSante b = new BilanSante();
                b.setId(rs.getInt("id"));
                if (rs.getDate("date_debut") != null) b.setDateDebut(rs.getDate("date_debut").toLocalDate());
                if (rs.getDate("date_fin") != null) b.setDateFin(rs.getDate("date_fin").toLocalDate());
                b.setNiveauFatigue(rs.getInt("niveau_fatigue"));
                b.setNiveauStress(rs.getInt("niveau_stress"));
                b.setScoreForme(rs.getFloat("score_forme"));
                b.setRisqueBurnout(rs.getBoolean("risque_burnout"));
                b.setRecommandations(rs.getString("recommandations"));

                bilans.add(b);
            }
        }
        return bilans;
    }
}
