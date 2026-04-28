package model;

import java.util.Date;

public class Depense {

    private int id;
    private String titre;
    private double montant;
    private String categorie;
    private Date date;
    private String typePaiement;
    private int utilisateurId;
    private int budgetId;

    public Depense() {
    }

    public Depense(String titre, double montant, String categorie, Date date,
                   String typePaiement, int utilisateurId, int budgetId) {
        this.titre = titre;
        this.montant = montant;
        this.categorie = categorie;
        this.date = date;
        this.typePaiement = typePaiement;
        this.utilisateurId = utilisateurId;
        this.budgetId = budgetId;
    }

    public Depense(int id, String titre, double montant, String categorie, Date date,
                   String typePaiement, int utilisateurId, int budgetId) {
        this.id = id;
        this.titre = titre;
        this.montant = montant;
        this.categorie = categorie;
        this.date = date;
        this.typePaiement = typePaiement;
        this.utilisateurId = utilisateurId;
        this.budgetId = budgetId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTypePaiement() {
        return typePaiement;
    }

    public void setTypePaiement(String typePaiement) {
        this.typePaiement = typePaiement;
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public int getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(int budgetId) {
        this.budgetId = budgetId;
    }

    @Override
    public String toString() {
        return "Depense{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", montant=" + montant +
                ", categorie='" + categorie + '\'' +
                ", date=" + date +
                ", typePaiement='" + typePaiement + '\'' +
                ", utilisateurId=" + utilisateurId +
                ", budgetId=" + budgetId +
                '}';
    }
}
