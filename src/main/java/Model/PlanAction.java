package Model;

import java.time.LocalDate;

public class PlanAction {
    private int id;
    private String titre;
    private String description;
    private String priorite;
    private LocalDate date_debut;
    private LocalDate date_fin;
    private int objectif_id;
    private String statut;

    public PlanAction() {}

    public PlanAction(int id, String titre, String description, String priorite, LocalDate date_debut, LocalDate date_fin, int objectif_id, String statut) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.priorite = priorite;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.objectif_id = objectif_id;
        this.statut = statut;
    }

    public PlanAction(String titre, String description, String priorite, LocalDate date_debut, LocalDate date_fin, int objectif_id, String statut) {
        this.titre = titre;
        this.description = description;
        this.priorite = priorite;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.objectif_id = objectif_id;
        this.statut = statut;
    }

    // Getters
    public int getId() { return id; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public String getPriorite() { return priorite; }
    public LocalDate getDate_debut() { return date_debut; }
    public LocalDate getDate_fin() { return date_fin; }
    public int getObjectif_id() { return objectif_id; }
    public String getStatut() { return statut; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setDescription(String description) { this.description = description; }
    public void setPriorite(String priorite) { this.priorite = priorite; }
    public void setDate_debut(LocalDate date_debut) { this.date_debut = date_debut; }
    public void setDate_fin(LocalDate date_fin) { this.date_fin = date_fin; }
    public void setObjectif_id(int objectif_id) { this.objectif_id = objectif_id; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "PlanAction{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", priorite='" + priorite + '\'' +
                ", date_debut=" + date_debut +
                ", date_fin=" + date_fin +
                ", objectif_id=" + objectif_id +
                ", statut='" + statut + '\'' +
                '}';
    }
}
