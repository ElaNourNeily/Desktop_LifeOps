package model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String mot_de_passe = " ";
    private String photo;
    private String role = "ROLE_USER";
    private LocalDateTime banUntil;
    private int age;
    private LocalDateTime created_at = LocalDateTime.now();
    private int is_verified ;
    private String telephone;

    public User (){}

    public User(int id, String nom, String prenom, String email, String mot_de_passe, String telephone, String photo, String role, LocalDateTime banUntil, int age) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.mot_de_passe = mot_de_passe;
        this.photo = photo;
        this.role = "ROLE_USER";
        this.banUntil = banUntil;
        this.age = age;
        this.created_at = LocalDateTime.now();
        is_verified = 1;
        this.telephone = telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getTelephone() {
        return telephone;
    }

    public User(String nom, String prenom, String email, int age) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.age = age;
        role = "ROLE_USER";
        this.created_at = LocalDateTime.now();
        is_verified = 1;


    }

    public int isIs_verified() {
        return is_verified;
    }

    public void setIs_verified(int is_verified) {
        this.is_verified = is_verified;
    }

    public User(String nom, String prenom, String email, int age , String mot_de_passe) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.age = age;
        this.mot_de_passe = mot_de_passe;
        role = "ROLE_USER";
        this.created_at = LocalDateTime.now();
        is_verified = 1;

    }

    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getEmail() {
        return email;
    }

    public String getMot_de_passe() {
        return mot_de_passe;
    }

    public String getPhoto() {
        return photo;
    }

    public String getRole() {
        return role;
    }

    public LocalDateTime getBanUntil() {
        return banUntil;
    }

    public int getAge() {
        return age;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setMot_de_passe(String mot_de_passe) {
        this.mot_de_passe = mot_de_passe;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setBanUntil(LocalDateTime banUntil) {
        this.banUntil = banUntil;
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
}

