package model;

public class Personne {
private  int id , age;

private  String nom , pernom ;


    public Personne(int id, int age, String nom, String pernom) {
        this.id = id;
        this.age = age;
        this.nom = nom;
        this.pernom = pernom;
    }

    public Personne(int age, String nom, String pernom) {
        this.age = age;
        this.nom = nom;
        this.pernom = pernom;
    }

    public Personne() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPernom() {
        return pernom;
    }

    public void setPernom(String pernom) {
        this.pernom = pernom;
    }

    @Override
    public String toString() {
        return "Personne{" +
                "id=" + id +
                ", age=" + age +
                ", nom='" + nom + '\'' +
                ", pernom='" + pernom + '\'' +
                '}';
    }
}

