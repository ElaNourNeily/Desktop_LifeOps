package enums;

public enum TypeTaskSpace {
    DEVELOPPEMENT("development", "Developpement", "développement", "developpement"),
    DESIGN("design", "Design"),
    MARKETING("marketing", "Marketing"),
    RECHERCHE("research", "Recherche"),
    AUTRE("other", "Autre");

    private final String valeur;
    private final String label;
    private final String[] aliases;

    TypeTaskSpace(String valeur, String label, String... aliases) {
        this.valeur = valeur;
        this.label = label;
        this.aliases = aliases;
    }

    public String getValeur() {
        return valeur;
    }

    public String getLabel() {
        return label;
    }

    public static TypeTaskSpace fromString(String val) {
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException("Type inconnu : " + val);
        }

        for (TypeTaskSpace type : values()) {
            if (type.matches(val)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type inconnu : " + val);
    }

    private boolean matches(String value) {
        if (valeur.equalsIgnoreCase(value) || label.equalsIgnoreCase(value) || name().equalsIgnoreCase(value)) {
            return true;
        }

        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return label;
    }
}
