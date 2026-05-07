package Model.health;

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
    public void setNiveauFatigue(int n) { this.niveauFatigue = n; }
    public int getNiveauStress() { return niveauStress; }
    public void setNiveauStress(int n) { this.niveauStress = n; }
    public int getScoreFormeGlobal() { return scoreFormeGlobal; }
    public void setScoreFormeGlobal(int s) { this.scoreFormeGlobal = s; }
    public String getRisqueBurnout() { return risqueBurnout; }
    public void setRisqueBurnout(String r) { this.risqueBurnout = r; }
    public List<String> getRecommandations() { return recommandations; }
    public void setRecommandations(List<String> r) { this.recommandations = r; }
    public String getExplication() { return explication; }
    public void setExplication(String e) { this.explication = e; }
}
