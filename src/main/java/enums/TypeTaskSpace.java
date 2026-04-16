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
        if (val == null || val.trim().isEmpty()) {
            return AUTRE; // Fallback par défaut si la valeur est nulle
        }

        // Cherche une correspondance exacte
        for (TypeTaskSpace t : values()) {
            if (t.valeur.equalsIgnoreCase(val) || t.name().equalsIgnoreCase(val)) {
                return t;
            }
        }

        // Mapping manuel pour corriger l'erreur de la base de données ("research" en anglais)
        if (val.equalsIgnoreCase("research")) {
            return RECHERCHE;
        }

        // Au lieu de faire crasher l'application avec une Exception,
        // on affiche une erreur et on retourne "AUTRE"
        System.err.println("⚠️ Attention: Type de TaskSpace inconnu dans la BD : '" + val + "' - Fallback sur AUTRE");
        return AUTRE;
    }

    @Override
    public String toString() {
        return valeur;
    }
}