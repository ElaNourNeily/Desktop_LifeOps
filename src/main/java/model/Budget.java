package model;

public class Budget {

    private int id;
    private double revenuMensuel;
    private double plafond;
    private String mois;
    private double economies;
    private int utilisateurId;

    public Budget() {
    }

    public Budget(double revenuMensuel, double plafond, String mois, double economies, int utilisateurId) {
        this.revenuMensuel = revenuMensuel;
        this.plafond = plafond;
        this.mois = mois;
        this.economies = economies;
        this.utilisateurId = utilisateurId;
    }

    public Budget(int id, double revenuMensuel, double plafond, String mois, double economies, int utilisateurId) {
        this.id = id;
        this.revenuMensuel = revenuMensuel;
        this.plafond = plafond;
        this.mois = mois;
        this.economies = economies;
        this.utilisateurId = utilisateurId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getRevenuMensuel() {
        return revenuMensuel;
    }

    public void setRevenuMensuel(double revenuMensuel) {
        this.revenuMensuel = revenuMensuel;
    }

    public double getPlafond() {
        return plafond;
    }

    public void setPlafond(double plafond) {
        this.plafond = plafond;
    }

    public String getMois() {
        return mois;
    }

    public void setMois(String mois) {
        this.mois = mois;
    }

    public double getEconomies() {
        return economies;
    }

    public void setEconomies(double economies) {
        this.economies = economies;
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    @Override
    public String toString() {
        return "Budget{" +
                "id=" + id +
                ", revenuMensuel=" + revenuMensuel +
                ", plafond=" + plafond +
                ", mois='" + mois + '\'' +
                ", economies=" + economies +
                ", utilisateurId=" + utilisateurId +
                '}';
    }
}
