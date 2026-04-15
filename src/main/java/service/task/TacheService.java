package service.task;

import enums.PrioriteTache;
import enums.StatutTache;
import model.task.Tache;
import service.Crud;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TacheService implements Crud<Tache> {

    private Connection connection;

    public TacheService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Tache t) throws SQLException {
        String sql = "INSERT INTO tache (titre, description, priorite, difficulte, statut, " +
                "deadline, created_at, updated_at, task_space_id, utilisateur_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, t.getTitre());
        ps.setString(2, t.getDescription());
        ps.setString(3, t.getPriorite().getValeur());   // enum → string DB
        ps.setInt(4, t.getDifficulte());
        ps.setString(5, t.getStatut().getValeur());     // enum → string DB

        if (t.getDeadline() != null)
            ps.setTimestamp(6, new Timestamp(t.getDeadline().getTime()));
        else
            ps.setNull(6, Types.TIMESTAMP);

        ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
        ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));

        // 0 = Solo → NULL en DB
        if (t.getTaskSpaceId() != 0)
            ps.setInt(9, t.getTaskSpaceId());
        else
            ps.setNull(9, Types.INTEGER);

        ps.setInt(10, t.getUtilisateurId());

        ps.executeUpdate();
        System.out.println("✅ Tache ajoutée : " + t.getTitre()
                + " [" + t.getPriorite() + " | " + t.getStatut() + "]");
    }

    @Override
    public List<Tache> recuperer() throws SQLException {
        String sql = "SELECT * FROM tache";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Tache> liste = new ArrayList<>();
        while (rs.next()) {
            liste.add(mapRow(rs));
        }
        return liste;
    }

    // Taches d'un utilisateur
    public List<Tache> recupererParUtilisateur(int utilisateurId) throws SQLException {
        String sql = "SELECT * FROM tache WHERE utilisateur_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();

        List<Tache> liste = new ArrayList<>();
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    // Taches d'un TaskSpace précis
    public List<Tache> recupererParTaskSpace(int taskSpaceId) throws SQLException {
        String sql = "SELECT * FROM tache WHERE task_space_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, taskSpaceId);
        ResultSet rs = ps.executeQuery();

        List<Tache> liste = new ArrayList<>();
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    // Taches Solo (sans projet) d'un utilisateur
    public List<Tache> recupererTachesSolo(int utilisateurId) throws SQLException {
        String sql = "SELECT * FROM tache WHERE task_space_id IS NULL AND utilisateur_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();

        List<Tache> liste = new ArrayList<>();
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    @Override
    public void modifier(Tache t) throws SQLException {
        String sql = "UPDATE tache SET titre=?, description=?, priorite=?, difficulte=?, " +
                "statut=?, deadline=?, updated_at=?, task_space_id=? " +
                "WHERE id=?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, t.getTitre());
        ps.setString(2, t.getDescription());
        ps.setString(3, t.getPriorite().getValeur());
        ps.setInt(4, t.getDifficulte());
        ps.setString(5, t.getStatut().getValeur());

        if (t.getDeadline() != null)
            ps.setTimestamp(6, new Timestamp(t.getDeadline().getTime()));
        else
            ps.setNull(6, Types.TIMESTAMP);

        ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));

        if (t.getTaskSpaceId() != 0)
            ps.setInt(8, t.getTaskSpaceId());
        else
            ps.setNull(8, Types.INTEGER);

        ps.setInt(9, t.getId());

        ps.executeUpdate();
        System.out.println("✅ Tache modifiée : id=" + t.getId()
                + " → " + t.getStatut().getValeur());
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM tache WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("✅ Tache supprimée : id=" + id);
    }

    // ─── Méthode privée pour éviter la répétition du mapping ─────────
    private Tache mapRow(ResultSet rs) throws SQLException {
        Tache t = new Tache();
        t.setId(rs.getInt("id"));
        t.setTitre(rs.getString("titre"));
        t.setDescription(rs.getString("description"));
        t.setPriorite(PrioriteTache.fromString(rs.getString("priorite")));  // string DB → enum
        t.setDifficulte(rs.getInt("difficulte"));
        t.setStatut(StatutTache.fromString(rs.getString("statut")));        // string DB → enum
        t.setDeadline(rs.getTimestamp("deadline"));
        t.setCreatedAt(rs.getTimestamp("created_at"));
        t.setUpdatedAt(rs.getTimestamp("updated_at"));
        t.setTaskSpaceId(rs.getInt("task_space_id")); // 0 si NULL
        t.setUtilisateurId(rs.getInt("utilisateur_id"));
        return t;
    }
    @Override
    public List<Tache> trier() {
        try {
            return recuperer().stream()
                    .sorted((t1, t2) -> {
                        // priorité d'abord
                        int cmp = t1.getPriorite().compareTo(t2.getPriorite());

                        // si même priorité → comparer difficulté
                        if (cmp == 0) {
                            return Integer.compare(t1.getDifficulte(), t2.getDifficulte());
                        }
                        return cmp;
                    })
                    .toList();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }
    @Override
    public List<Tache> rechercher(String titre) {
        try {
            return recuperer().stream()
                    .filter(t -> t.getTitre().toLowerCase().contains(titre.toLowerCase()))
                    .toList();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }
}
