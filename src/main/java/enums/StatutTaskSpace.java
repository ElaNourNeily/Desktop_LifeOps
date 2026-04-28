package enums;

public enum StatutTaskSpace {
    ACTIF("active"),
    EN_PAUSE("paused"),
    TERMINE("completed"),
    ARCHIVE("archived");

    private final String valeur;

    StatutTaskSpace(String valeur) {
        this.valeur = valeur;
    }

    public String getValeur() {
        return valeur;
    }

    public static StatutTaskSpace fromString(String val) {
        for (StatutTaskSpace s : values()) {
            if (s.valeur.equalsIgnoreCase(val) || s.name().equalsIgnoreCase(val)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Statut TaskSpace inconnu : " + val);
    }

    @Override
    public String toString() {
        return valeur;
    }
}