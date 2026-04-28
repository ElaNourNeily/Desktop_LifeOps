package enums;

public enum PrioriteTache {
    BASSE("low"),
    MOYENNE("medium"),
    HAUTE("high"),
    URGENTE("urgent");

    private final String valeur;

    PrioriteTache(String valeur) {
        this.valeur = valeur;
    }

    public String getValeur() {
        return valeur;
    }

    public static PrioriteTache fromString(String val) {
        for (PrioriteTache p : values()) {
            if (p.valeur.equalsIgnoreCase(val) || p.name().equalsIgnoreCase(val)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Priorité inconnue : " + val);
    }

    @Override
    public String toString() {
        return valeur;
    }
}