package enums;

public enum StatutTache {
    A_FAIRE("todo"),
    EN_COURS("in-progress"),
    EN_REVISION("review"),
    TERMINE("done");

    private final String valeur;

    StatutTache(String valeur) {
        this.valeur = valeur;
    }

    public String getValeur() {
        return valeur;
    }

    public static StatutTache fromString(String val) {
        for (StatutTache s : values()) {
            if (s.valeur.equalsIgnoreCase(val) || s.name().equalsIgnoreCase(val)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Statut Tache inconnu : " + val);
    }

    @Override
    public String toString() {
        return valeur;
    }
}