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
    private int utilisateurId;   // Creator
    private Integer assignedUserId; // NEW: The user responsible for the task

    public Tache() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public PrioriteTache getPriorite() { return priorite; }
    public void setPriorite(PrioriteTache priorite) { this.priorite = priorite; }

    public int getDifficulte() { return difficulte; }
    public void setDifficulte(int difficulte) { this.difficulte = difficulte; }

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

    public Integer getAssignedUserId() { return assignedUserId; }
    public void setAssignedUserId(Integer assignedUserId) { this.assignedUserId = assignedUserId; }
}