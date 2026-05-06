package enums;

public enum TypeTaskSpace {
    DEVELOPPEMENT("Développement"),
    DESIGN("Design"),
    MARKETING("Marketing"),
    RECHERCHE("Recherche"),
    AUTRE("Autre");

    private final String valeur;

    TypeTaskSpace(String valeur) {
        this.valeur = valeur;
    }

    public String getValeur() {
        return valeur;
    }

    // Convertir un String de la DB → Enum
    public static TypeTaskSpace fromString(String val) {
        for (TypeTaskSpace t : values()) {
            if (t.valeur.equalsIgnoreCase(val) || t.name().equalsIgnoreCase(val)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Type inconnu : " + val);
    }

    @Override
    public String toString() {
        return valeur;
    }
}