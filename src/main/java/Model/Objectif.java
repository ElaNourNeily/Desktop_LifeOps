package Model;

import java.time.LocalDate;

public class Objectif {
    private int id;
    private String titre;
    private String description;
    private String categorie;
    private String statut;
    private LocalDate date_debut;
    private LocalDate date_fin;
    private int progression;
    
    // Relation : Chaque objectif a un plan d'action (gardé depuis votre version précédente)
    private PlanAction planAction;

    public Objectif() {}

    public Objectif(int id, String titre, String description, String categorie, String statut, LocalDate date_debut, LocalDate date_fin, int progression, PlanAction planAction) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.categorie = categorie;
        this.statut = statut;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.progression = progression;
        this.planAction = planAction;
    }

    public Objectif(String titre, String description, String categorie, String statut, LocalDate date_debut, LocalDate date_fin, int progression) {
        this.titre = titre;
        this.description = description;
        this.categorie = categorie;
        this.statut = statut;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.progression = progression;
    }

    // Getters
    public int getId() { return id; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public String getCategorie() { return categorie; }
    public String getStatut() { return statut; }
    public LocalDate getDate_debut() { return date_debut; }
    public LocalDate getDate_fin() { return date_fin; }
    public int getProgression() { return progression; }
    public PlanAction getPlanAction() { return planAction; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setDescription(String description) { this.description = description; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setDate_debut(LocalDate date_debut) { this.date_debut = date_debut; }
    public void setDate_fin(LocalDate date_fin) { this.date_fin = date_fin; }
    public void setProgression(int progression) { this.progression = progression; }
    public void setPlanAction(PlanAction planAction) { this.planAction = planAction; }

    @Override
    public String toString() {
        return "Objectif{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", categorie='" + categorie + '\'' +
                ", statut='" + statut + '\'' +
                ", date_debut=" + date_debut +
                ", date_fin=" + date_fin +
                ", progression=" + progression +
                ", planAction=" + planAction +
                '}';
    }
}
