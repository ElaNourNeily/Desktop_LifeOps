package service.health;

import Model.health.BilanSante;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BilanSanteService {

    private final Connection connection;

    public BilanSanteService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(BilanSante b) throws SQLException {
        String sql = "INSERT INTO bilan_sante (date_debut, date_fin, niveau_fatigue, niveau_stress, " +
                     "score_forme, risque_burnout, recommandations, utilisateur_id) VALUES (?,?,?,?,?,?,?,1)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, b.getDateDebut() != null ? Date.valueOf(b.getDateDebut()) : null);
            ps.setDate(2, b.getDateFin() != null ? Date.valueOf(b.getDateFin()) : null);
            ps.setInt(3, b.getNiveauFatigue());
            ps.setInt(4, b.getNiveauStress());
            ps.setFloat(5, b.getScoreForme());
            ps.setBoolean(6, b.isRisqueBurnout());
            ps.setString(7, b.getRecommandations());
            ps.executeUpdate();
        }
    }

    public void modifier(BilanSante b) throws SQLException {
        String sql = "UPDATE bilan_sante SET date_debut=?, date_fin=?, niveau_fatigue=?, niveau_stress=?, " +
                     "score_forme=?, risque_burnout=?, recommandations=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, b.getDateDebut() != null ? Date.valueOf(b.getDateDebut()) : null);
            ps.setDate(2, b.getDateFin() != null ? Date.valueOf(b.getDateFin()) : null);
            ps.setInt(3, b.getNiveauFatigue());
            ps.setInt(4, b.getNiveauStress());
            ps.setFloat(5, b.getScoreForme());
            ps.setBoolean(6, b.isRisqueBurnout());
            ps.setString(7, b.getRecommandations());
            ps.setInt(8, b.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM bilan_sante WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<BilanSante> recuperer() throws SQLException {
        List<BilanSante> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM bilan_sante ORDER BY date_debut DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private BilanSante map(ResultSet rs) throws SQLException {
        BilanSante b = new BilanSante();
        b.setId(rs.getInt("id"));
        if (rs.getDate("date_debut") != null) b.setDateDebut(rs.getDate("date_debut").toLocalDate());
        if (rs.getDate("date_fin") != null) b.setDateFin(rs.getDate("date_fin").toLocalDate());
        b.setNiveauFatigue(rs.getInt("niveau_fatigue"));
        b.setNiveauStress(rs.getInt("niveau_stress"));
        b.setScoreForme(rs.getFloat("score_forme"));
        b.setRisqueBurnout(rs.getBoolean("risque_burnout"));
        b.setRecommandations(rs.getString("recommandations"));
        return b;
    }
}
