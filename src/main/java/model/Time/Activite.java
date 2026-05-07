package model.Time;

import java.sql.Time;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

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
    private int minutesRappel = 0;
    
    // Feature 1: Stats
    private Time heureDebutReelle;
    private Time heureFinReelle;

    // Feature 3: Recurrence
    private boolean recurrent = false;
    private String recurrenceType; // DAILY, WEEKLY, MONTHLY
    private String recurrenceDays; // TUE,THU
    private int recurrenceInterval = 1;
    private int recurrenceGroupId = 0;

    public Activite() {}

    public Activite(int id, String titre, int duree, int priorite, String etat, Time heureDebutEstimee, Time heureFinEstimee, String niveauUrgence, String categorie, String couleur, boolean suggestedByAi, int planningId, int minutesRappel) {
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
        this.minutesRappel = minutesRappel;
    }

    public Activite(int id, String titre, int duree, int priorite, String etat, Time heureDebutEstimee, Time heureFinEstimee, String niveauUrgence, String categorie, String couleur, boolean suggestedByAi, int planningId, int minutesRappel, Time heureDebutReelle, Time heureFinReelle, boolean recurrent, String recurrenceType, String recurrenceDays, int recurrenceInterval) {
        this(id, titre, duree, priorite, etat, heureDebutEstimee, heureFinEstimee, niveauUrgence, categorie, couleur, suggestedByAi, planningId, minutesRappel);
        this.heureDebutReelle = heureDebutReelle;
        this.heureFinReelle = heureFinReelle;
        this.recurrent = recurrent;
        this.recurrenceType = recurrenceType;
        this.recurrenceDays = recurrenceDays;
        this.recurrenceInterval = recurrenceInterval;
    }

    public Activite(int id, String titre, int duree, int priorite, String etat, Time heureDebutEstimee, Time heureFinEstimee, String niveauUrgence, String categorie, String couleur, boolean suggestedByAi, int planningId, int minutesRappel, Time heureDebutReelle, Time heureFinReelle, boolean recurrent, String recurrenceType, String recurrenceDays, int recurrenceInterval, int recurrenceGroupId) {
        this(id, titre, duree, priorite, etat, heureDebutEstimee, heureFinEstimee, niveauUrgence, categorie, couleur, suggestedByAi, planningId, minutesRappel, heureDebutReelle, heureFinReelle, recurrent, recurrenceType, recurrenceDays, recurrenceInterval);
        this.recurrenceGroupId = recurrenceGroupId;
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

    public int getMinutesRappel() { return minutesRappel; }
    public void setMinutesRappel(int minutesRappel) { this.minutesRappel = minutesRappel; }

    public Time getHeureDebutReelle() { return heureDebutReelle; }
    public void setHeureDebutReelle(Time heureDebutReelle) { this.heureDebutReelle = heureDebutReelle; }

    public Time getHeureFinReelle() { return heureFinReelle; }
    public void setHeureFinReelle(Time heureFinReelle) { this.heureFinReelle = heureFinReelle; }

    public boolean isRecurrent() { return recurrent; }
    public void setRecurrent(boolean recurrent) { this.recurrent = recurrent; }

    public String getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(String recurrenceType) { this.recurrenceType = recurrenceType; }

    public String getRecurrenceDays() { return recurrenceDays; }
    public void setRecurrenceDays(String recurrenceDays) { this.recurrenceDays = recurrenceDays; }

    public int getRecurrenceInterval() { return recurrenceInterval; }
    public void setRecurrenceInterval(int recurrenceInterval) { this.recurrenceInterval = recurrenceInterval; }

    public String getStatutDynamique(java.sql.Date planningDate) {
        if (planningDate == null || heureDebutEstimee == null || heureFinEstimee == null) {
            return "En attente";
        }

        LocalDateTime now = LocalDateTime.now();
        
        // Convert planningDate to LocalDate and combine with LocalTime
        java.time.LocalDate localDate = planningDate.toLocalDate();
        LocalDateTime start = LocalDateTime.of(localDate, heureDebutEstimee.toLocalTime());
        LocalDateTime end = LocalDateTime.of(localDate, heureFinEstimee.toLocalTime());

        if (now.isBefore(start)) {
            return "En attente";
        } else if (now.isAfter(end)) {
            return "Terminé";
        } else {
            return "En cours";
        }
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
    public int getRecurrenceGroupId() { return recurrenceGroupId; }
    public void setRecurrenceGroupId(int recurrenceGroupId) { this.recurrenceGroupId = recurrenceGroupId; }
}
