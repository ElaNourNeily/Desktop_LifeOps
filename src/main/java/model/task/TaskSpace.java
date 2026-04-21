package model.task;

import enums.StatutTaskSpace;
import java.util.Date;

public class TaskSpace {

    private int id;
    private String nom;
    private String mode;      // solo / equipe
    private String category;  // Recherche, Marketing, Design, Développement, Autre
    private Date dateCreation;
    private String description;
    private int duration;
    private StatutTaskSpace status;
    private int utilisateurId;

    // ─── Constructeur sans ID (INSERT) ───────────────────────────────
    public TaskSpace(String nom, String mode, String category, Date dateCreation,
                     String description, int duration, StatutTaskSpace status, int utilisateurId) {
        this.nom = nom;
        this.mode = mode;
        this.category = category;
        this.dateCreation = dateCreation;
        this.description = description;
        this.duration = duration;
        this.status = status;
        this.utilisateurId = utilisateurId;
    }

    // ─── Constructeur avec ID (UPDATE / affichage) ────────────────────
    public TaskSpace(int id, String nom, String mode, String category, Date dateCreation,
                     String description, int duration, StatutTaskSpace status, int utilisateurId) {
        this.id = id;
        this.nom = nom;
        this.mode = mode;
        this.category = category;
        this.dateCreation = dateCreation;
        this.description = description;
        this.duration = duration;
        this.status = status;
        this.utilisateurId = utilisateurId;
    }

    public TaskSpace() {}

    // ─── Getters & Setters ────────────────────────────────────────────
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

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    @Override
    public String toString() {
        return "TaskSpace{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", mode='" + mode + '\'' +
                ", category='" + category + '\'' +
                ", status=" + (status != null ? status.getValeur() : "null") +
                ", duration=" + duration +
                '}';
    }
}