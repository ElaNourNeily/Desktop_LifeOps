package model;

import java.util.List;

public class BilanAnalyse {
    private int niveauFatigue;
    private int niveauStress;
    private int scoreFormeGlobal;
    private String risqueBurnout;
    private List<String> recommandations;
    private String explication;

    public BilanAnalyse() {}

    public int getNiveauFatigue() { return niveauFatigue; }
    public void setNiveauFatigue(int niveauFatigue) { this.niveauFatigue = niveauFatigue; }

    public int getNiveauStress() { return niveauStress; }
    public void setNiveauStress(int niveauStress) { this.niveauStress = niveauStress; }

    public int getScoreFormeGlobal() { return scoreFormeGlobal; }
    public void setScoreFormeGlobal(int scoreFormeGlobal) { this.scoreFormeGlobal = scoreFormeGlobal; }

    public String getRisqueBurnout() { return risqueBurnout; }
    public void setRisqueBurnout(String risqueBurnout) { this.risqueBurnout = risqueBurnout; }

    public List<String> getRecommandations() { return recommandations; }
    public void setRecommandations(List<String> recommandations) { this.recommandations = recommandations; }

    public String getExplication() { return explication; }
    public void setExplication(String explication) { this.explication = explication; }
}
