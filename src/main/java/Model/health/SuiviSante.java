package Model.health;

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

    public SuiviSante(int id, LocalDate date, float heuresSommeil, int qualiteSommeil,
                      int verresEau, int minutesActivite, float poids, int humeur,
                      String notes, String activite) {
        this.id = id; this.date = date; this.heuresSommeil = heuresSommeil;
        this.qualiteSommeil = qualiteSommeil; this.verresEau = verresEau;
        this.minutesActivite = minutesActivite; this.poids = poids;
        this.humeur = humeur; this.notes = notes; this.activite = activite;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public float getHeuresSommeil() { return heuresSommeil; }
    public void setHeuresSommeil(float h) { this.heuresSommeil = h; }
    public int getQualiteSommeil() { return qualiteSommeil; }
    public void setQualiteSommeil(int q) { this.qualiteSommeil = q; }
    public int getVerresEau() { return verresEau; }
    public void setVerresEau(int v) { this.verresEau = v; }
    public int getMinutesActivite() { return minutesActivite; }
    public void setMinutesActivite(int m) { this.minutesActivite = m; }
    public float getPoids() { return poids; }
    public void setPoids(float p) { this.poids = p; }
    public int getHumeur() { return humeur; }
    public void setHumeur(int h) { this.humeur = h; }
    public String getNotes() { return notes; }
    public void setNotes(String n) { this.notes = n; }
    public String getActivite() { return activite; }
    public void setActivite(String a) { this.activite = a; }
}
