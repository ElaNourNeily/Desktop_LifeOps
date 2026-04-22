package model;

import java.sql.Time;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Calendar;

public class Activite {
    private int id;
    private String titre;
    private int duree;
    private int priorite;
    private String etat = "en_attente";
    private Time heureDebutEstimee;
    private Time heureFinEstimee;
    private String niveauUrgence = "moyen";
    private String categorie;
    private String couleur;
    private boolean suggestedByAi = false;
    private int planningId;

    public Activite() {}

    public Activite(int id, String titre, int duree, int priorite, String etat, Time heureDebutEstimee, Time heureFinEstimee, String niveauUrgence, String categorie, String couleur, boolean suggestedByAi, int planningId) {
        this.id = id;
        this.titre = titre;
        this.duree = duree;
        this.priorite = priorite;
        this.etat = etat;
        this.heureDebutEstimee = heureDebutEstimee;
        this.heureFinEstimee = heureFinEstimee;
        this.niveauUrgence = niveauUrgence;
        this.categorie = categorie;
        this.couleur = couleur;
        this.suggestedByAi = suggestedByAi;
        this.planningId = planningId;
    }

    public Activite(String titre, int duree, int priorite, String etat, Time heureDebutEstimee, Time heureFinEstimee, String niveauUrgence, String categorie, String couleur, boolean suggestedByAi, int planningId) {
        this.titre = titre;
        this.duree = duree;
        this.priorite = priorite;
        this.etat = etat;
        this.heureDebutEstimee = heureDebutEstimee;
        this.heureFinEstimee = heureFinEstimee;
        this.niveauUrgence = niveauUrgence;
        this.categorie = categorie;
        this.couleur = couleur;
        this.suggestedByAi = suggestedByAi;
        this.planningId = planningId;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public int getPriorite() { return priorite; }
    public void setPriorite(int priorite) { this.priorite = priorite; }

    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    public Time getHeureDebutEstimee() { return heureDebutEstimee; }
    public void setHeureDebutEstimee(Time heureDebutEstimee) { this.heureDebutEstimee = heureDebutEstimee; }

    public Time getHeureFinEstimee() { return heureFinEstimee; }
    public void setHeureFinEstimee(Time heureFinEstimee) { this.heureFinEstimee = heureFinEstimee; }

    public String getNiveauUrgence() { return niveauUrgence; }
    public void setNiveauUrgence(String niveauUrgence) { this.niveauUrgence = niveauUrgence; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getCouleur() { return couleur; }
    public void setCouleur(String couleur) { this.couleur = couleur; }

    public boolean isSuggestedByAi() { return suggestedByAi; }
    public void setSuggestedByAi(boolean suggestedByAi) { this.suggestedByAi = suggestedByAi; }

    public int getPlanningId() { return planningId; }
    public void setPlanningId(int planningId) { this.planningId = planningId; }

    public String getStatutDynamique(Date planningDate) {
        if (planningDate == null || heureDebutEstimee == null || heureFinEstimee == null) {
            return "En attente";
        }

        LocalDateTime now = LocalDateTime.now();
        
        // Combine planning Date with activity Time
        LocalDateTime start = combine(planningDate, heureDebutEstimee);
        LocalDateTime end = combine(planningDate, heureFinEstimee);

        if (now.isBefore(start)) {
            return "En attente";
        }

        if (now.isAfter(end)) {
            return "Terminé";
        }

        return "En cours";
    }

    private LocalDateTime combine(Date date, Time time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        
        LocalTime lt = time.toLocalTime();
        return LocalDateTime.of(year, month, day, lt.getHour(), lt.getMinute(), lt.getSecond());
    }

    @Override
    public String toString() {
        return "Activite{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", duree=" + duree +
                ", priorite=" + priorite +
                ", etat='" + etat + '\'' +
                ", heureDebutEstimee=" + heureDebutEstimee +
                ", heureFinEstimee=" + heureFinEstimee +
                ", niveauUrgence='" + niveauUrgence + '\'' +
                ", categorie='" + categorie + '\'' +
                ", couleur='" + couleur + '\'' +
                ", suggestedByAi=" + suggestedByAi +
                ", planningId=" + planningId +
                '}';
    }
}
