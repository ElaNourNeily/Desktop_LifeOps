package Model.health;

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

    public BilanSante(int id, LocalDate dateDebut, LocalDate dateFin, int niveauFatigue,
                      int niveauStress, float scoreForme, boolean risqueBurnout, String recommandations) {
        this.id = id; this.dateDebut = dateDebut; this.dateFin = dateFin;
        this.niveauFatigue = niveauFatigue; this.niveauStress = niveauStress;
        this.scoreForme = scoreForme; this.risqueBurnout = risqueBurnout;
        this.recommandations = recommandations;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate d) { this.dateDebut = d; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate d) { this.dateFin = d; }
    public int getNiveauFatigue() { return niveauFatigue; }
    public void setNiveauFatigue(int n) { this.niveauFatigue = n; }
    public int getNiveauStress() { return niveauStress; }
    public void setNiveauStress(int n) { this.niveauStress = n; }
    public float getScoreForme() { return scoreForme; }
    public void setScoreForme(float s) { this.scoreForme = s; }
    public boolean isRisqueBurnout() { return risqueBurnout; }
    public void setRisqueBurnout(boolean r) { this.risqueBurnout = r; }
    public String getRecommandations() { return recommandations; }
    public void setRecommandations(String r) { this.recommandations = r; }
}
