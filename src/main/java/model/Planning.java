package model;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class Planning {
    private int id;
    private Date date;
    private boolean disponibilite = true;
    private Time heureDebutJournee;
    private Time heureFinJournee;
    private int utilisateurId;
    private List<Activite> activites = new ArrayList<>();

    public Planning() {}

    public Planning(int id, Date date, boolean disponibilite, Time heureDebutJournee, Time heureFinJournee, int utilisateurId) {
        this.id = id;
        this.date = date;
        this.disponibilite = disponibilite;
        this.heureDebutJournee = heureDebutJournee;
        this.heureFinJournee = heureFinJournee;
        this.utilisateurId = utilisateurId;
    }

    public Planning(Date date, boolean disponibilite, Time heureDebutJournee, Time heureFinJournee, int utilisateurId) {
        this.date = date;
        this.disponibilite = disponibilite;
        this.heureDebutJournee = heureDebutJournee;
        this.heureFinJournee = heureFinJournee;
        this.utilisateurId = utilisateurId;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public boolean isDisponibilite() { return disponibilite; }
    public void setDisponibilite(boolean disponibilite) { this.disponibilite = disponibilite; }

    public Time getHeureDebutJournee() { return heureDebutJournee; }
    public void setHeureDebutJournee(Time heureDebutJournee) { this.heureDebutJournee = heureDebutJournee; }

    public Time getHeureFinJournee() { return heureFinJournee; }
    public void setHeureFinJournee(Time heureFinJournee) { this.heureFinJournee = heureFinJournee; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public List<Activite> getActivites() { return activites; }
    public void setActivites(List<Activite> activites) { this.activites = activites; }

    public boolean isValid() {
        if (heureDebutJournee != null && heureFinJournee != null) {
            return heureFinJournee.after(heureDebutJournee);
        }
        return true;
    }

    @Override
    public String toString() {
        return "Planning{" +
                "id=" + id +
                ", date=" + date +
                ", disponibilite=" + disponibilite +
                ", heureDebutJournee=" + heureDebutJournee +
                ", heureFinJournee=" + heureFinJournee +
                ", utilisateurId=" + utilisateurId +
                ", activitesCount=" + activites.size() +
                '}';
    }
}
