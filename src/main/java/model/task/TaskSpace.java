package model.task;

import enums.StatutTaskSpace;
import java.util.Date;

public class TaskSpace {

    private int id;
    private String nom;
    private String mode;      // Solo / Equipe
    private String category;  // Recherche, Marketing, Design, Développement, Autre
    private Date dateCreation;
    private String description;
    private int duration;
    private StatutTaskSpace status;
    private int leaderId;     // NEW: The user who created the board
    private int utilisateurId; // Keeping for compatibility, but leaderId is the new secondary key

    public TaskSpace() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public StatutTaskSpace getStatus() { return status; }
    public void setStatus(StatutTaskSpace status) { this.status = status; }

    public int getLeaderId() { return leaderId; }
    public void setLeaderId(int leaderId) { this.leaderId = leaderId; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public boolean isTeam() {
        return "Equipe".equalsIgnoreCase(mode);
    }

    @Override
    public String toString() {
        return nom + " [" + mode + "]";
    }
}