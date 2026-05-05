package model.user;

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

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMot_de_passe() { return mot_de_passe; }
    public void setMot_de_passe(String mot_de_passe) { this.mot_de_passe = mot_de_passe; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getBanUntil() { return banUntil; }
    public void setBanUntil(LocalDateTime banUntil) { this.banUntil = banUntil; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public LocalDateTime getCreated_at() { return created_at; }
    public void setCreated_at(LocalDateTime created_at) { this.created_at = created_at; }

    public int isIs_verified() { return is_verified; }
    public void setIs_verified(int is_verified) { this.is_verified = is_verified; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public boolean hasSetPassword() { return hasSetPassword; }
    public void setHasSetPassword(boolean hasSetPassword) { this.hasSetPassword = hasSetPassword; }

    public String getFullName() { return prenom + " " + nom; }

    public boolean isAdmin() { return "ROLE_ADMIN".equalsIgnoreCase(role); }

    @Override
    public String toString() { return getFullName() + " (" + email + ")"; }
}
