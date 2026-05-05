package model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String mot_de_passe = "";
    private String photo;
    private String role = "ROLE_USER";
    private LocalDateTime banUntil;
    private int age;
    private LocalDateTime created_at = LocalDateTime.now();
    private int is_verified = 1;
    private String telephone;
    private boolean hasSetPassword = true;

    public User() {}

    public User(int id, String nom, String prenom, String email, String role) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = role;
    }

    public User(int id, String nom, String prenom, String email, String mot_de_passe, String telephone, String photo, String role, LocalDateTime banUntil, int age) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.mot_de_passe = mot_de_passe;
        this.photo = photo;
        this.role = role != null ? role : "ROLE_USER";
        this.banUntil = banUntil;
        this.age = age;
        this.created_at = LocalDateTime.now();
        this.is_verified = 1;
        this.telephone = telephone;
        this.hasSetPassword = true;
    }

    public User(String nom, String prenom, String email, int age) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.age = age;
        this.role = "ROLE_USER";
        this.created_at = LocalDateTime.now();
        this.is_verified = 1;
    }

    public User(String nom, String prenom, String email, int age, String mot_de_passe) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.age = age;
        this.mot_de_passe = mot_de_passe;
        this.role = "ROLE_USER";
        this.created_at = LocalDateTime.now();
        this.is_verified = 1;
        this.hasSetPassword = true;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMot_de_passe() {
        return mot_de_passe;
    }

    public void setMot_de_passe(String mot_de_passe) {
        this.mot_de_passe = mot_de_passe;
    }

    // Alias for compatibility with task management code
    public String getMotDePasse() {
        return mot_de_passe;
    }

    public void setMotDePasse(String motDePasse) {
        this.mot_de_passe = motDePasse;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getBanUntil() {
        return banUntil;
    }

    public void setBanUntil(LocalDateTime banUntil) {
        this.banUntil = banUntil;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    // Alias for compatibility
    public LocalDateTime getCreatedAt() {
        return created_at;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.created_at = createdAt;
    }

    public int isIs_verified() {
        return is_verified;
    }

    public void setIs_verified(int is_verified) {
        this.is_verified = is_verified;
    }

    // Alias for compatibility
    public boolean isVerified() {
        return is_verified == 1;
    }

    public void setVerified(boolean verified) {
        this.is_verified = verified ? 1 : 0;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public boolean hasSetPassword() {
        return hasSetPassword;
    }

    public void setHasSetPassword(boolean hasSetPassword) {
        this.hasSetPassword = hasSetPassword;
    }

    public String getFullName() {
        return prenom + " " + nom;
    }

    public boolean isAdmin() {
        return "ROLE_ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return getFullName() + " (" + email + ")";
    }
}
