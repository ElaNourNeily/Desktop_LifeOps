package model;

import java.util.Date;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String role; // System role (e.g., ROLE_USER, ROLE_ADMIN)
    private String photo;
    private Date createdAt;
    private int age;
    private boolean isVerified;
    private String telephone;
    private boolean hasSetPassword;

    public User() {}

    public User(int id, String nom, String prenom, String email, String role) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = role;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public boolean hasSetPassword() { return hasSetPassword; }
    public void setHasSetPassword(boolean hasSetPassword) { this.hasSetPassword = hasSetPassword; }

    public String getFullName() {
        return prenom + " " + nom;
    }

    @Override
    public String toString() {
        return getFullName() + " (" + email + ")";
    }
}
