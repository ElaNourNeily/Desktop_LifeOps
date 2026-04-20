package model.task;

import enums.PrioriteTache;
import enums.StatutTache;
import java.util.Date;

public class Tache {

    private int id;
    private String titre;
    private String description;
    private PrioriteTache priorite;
    private int difficulte;
    private StatutTache statut;
    private Date deadline;
    private Date createdAt;
    private Date updatedAt;
    private int taskSpaceId;
    private int utilisateurId;

    public Tache(String titre, String description, PrioriteTache priorite,
                 int difficulte, StatutTache statut, Date deadline,
                 int taskSpaceId, int utilisateurId) {
        this.titre = titre;
        this.description = description;
        this.priorite = priorite;
        this.difficulte = difficulte;
        this.statut = statut;
        this.deadline = deadline;
        this.taskSpaceId = taskSpaceId;
        this.utilisateurId = utilisateurId;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public Tache(int id, String titre, String description, PrioriteTache priorite,
                 int difficulte, StatutTache statut, Date deadline,
                 int taskSpaceId, int utilisateurId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.priorite = priorite;
        this.difficulte = difficulte;
        this.statut = statut;
        this.deadline = deadline;
        this.taskSpaceId = taskSpaceId;
        this.utilisateurId = utilisateurId;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public Tache() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public PrioriteTache getPriorite() { return priorite; }
    public void setPriorite(PrioriteTache priorite) { this.priorite = priorite; }

    public int getDifficulte() { return difficulte; }
    public void setDifficulte(int difficulte) {
        if (difficulte < 1 || difficulte > 5)
            throw new IllegalArgumentException("La difficulté doit être entre 1 et 5.");
        this.difficulte = difficulte;
    }

    public StatutTache getStatut() { return statut; }
    public void setStatut(StatutTache statut) { this.statut = statut; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public int getTaskSpaceId() { return taskSpaceId; }
    public void setTaskSpaceId(int taskSpaceId) { this.taskSpaceId = taskSpaceId; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    @Override
    public String toString() {
        return "Tache{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", priorite=" + (priorite != null ? priorite.getValeur() : "null") +
                ", difficulte=" + difficulte + "/5" +
                ", statut=" + (statut != null ? statut.getValeur() : "null") +
                ", deadline=" + deadline +
                ", taskSpaceId=" + taskSpaceId +
                ", utilisateurId=" + utilisateurId +
                '}';
    }
}