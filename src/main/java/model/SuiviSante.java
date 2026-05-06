package model;

import java.time.LocalDate;

public class SuiviSante {
    private int id;
    private LocalDate date;
    private float heuresSommeil;
    private int qualiteSommeil;
    private int verresEau;
    private int minutesActivite;
    private float poids;
    private int humeur;
    private String notes;
    private String activite;

    public SuiviSante() {}

    public SuiviSante(int id, LocalDate date, float heuresSommeil, int qualiteSommeil, int verresEau, int minutesActivite, float poids, int humeur, String notes, String activite) {
        this.id = id;
        this.date = date;
        this.heuresSommeil = heuresSommeil;
        this.qualiteSommeil = qualiteSommeil;
        this.verresEau = verresEau;
        this.minutesActivite = minutesActivite;
        this.poids = poids;
        this.humeur = humeur;
        this.notes = notes;
        this.activite = activite;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public float getHeuresSommeil() { return heuresSommeil; }
    public void setHeuresSommeil(float heuresSommeil) { this.heuresSommeil = heuresSommeil; }
    public int getQualiteSommeil() { return qualiteSommeil; }
    public void setQualiteSommeil(int qualiteSommeil) { this.qualiteSommeil = qualiteSommeil; }
    public int getVerresEau() { return verresEau; }
    public void setVerresEau(int verresEau) { this.verresEau = verresEau; }
    public int getMinutesActivite() { return minutesActivite; }
    public void setMinutesActivite(int minutesActivite) { this.minutesActivite = minutesActivite; }
    public float getPoids() { return poids; }
    public void setPoids(float poids) { this.poids = poids; }
    public int getHumeur() { return humeur; }
    public void setHumeur(int humeur) { this.humeur = humeur; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getActivite() { return activite; }
    public void setActivite(String activite) { this.activite = activite; }

    @Override
    public String toString() {
        return "SuiviSante{id=" + id + ", date=" + date + ", heuresSommeil=" + heuresSommeil + ", qualiteSommeil=" + qualiteSommeil + ", verresEau=" + verresEau + ", minutesActivite=" + minutesActivite + ", poids=" + poids + ", humeur=" + humeur + ", notes='" + notes + "', activite='" + activite + "'}";
    }
}
