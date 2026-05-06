package model;

import java.time.LocalDate;

public class BilanSante {
    private int id;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int niveauFatigue;
    private int niveauStress;
    private float scoreForme;
    private boolean risqueBurnout;
    private String recommandations;

    public BilanSante() {}

    public BilanSante(int id, LocalDate dateDebut, LocalDate dateFin, int niveauFatigue, int niveauStress, float scoreForme, boolean risqueBurnout, String recommandations) {
        this.id = id;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.niveauFatigue = niveauFatigue;
        this.niveauStress = niveauStress;
        this.scoreForme = scoreForme;
        this.risqueBurnout = risqueBurnout;
        this.recommandations = recommandations;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public int getNiveauFatigue() { return niveauFatigue; }
    public void setNiveauFatigue(int niveauFatigue) { this.niveauFatigue = niveauFatigue; }
    public int getNiveauStress() { return niveauStress; }
    public void setNiveauStress(int niveauStress) { this.niveauStress = niveauStress; }
    public float getScoreForme() { return scoreForme; }
    public void setScoreForme(float scoreForme) { this.scoreForme = scoreForme; }
    public boolean isRisqueBurnout() { return risqueBurnout; }
    public void setRisqueBurnout(boolean risqueBurnout) { this.risqueBurnout = risqueBurnout; }
    public String getRecommandations() { return recommandations; }
    public void setRecommandations(String recommandations) { this.recommandations = recommandations; }

    @Override
    public String toString() {
        return "BilanSante{id=" + id + ", dateDebut=" + dateDebut + ", dateFin=" + dateFin + ", niveauFatigue=" + niveauFatigue + ", niveauStress=" + niveauStress + ", scoreForme=" + scoreForme + ", risqueBurnout=" + risqueBurnout + ", recommandations='" + recommandations + "'}";
    }
}
