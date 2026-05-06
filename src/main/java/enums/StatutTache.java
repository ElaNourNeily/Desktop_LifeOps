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
        if (val == null) return A_FAIRE;
        
        String cleanVal = val.trim().toLowerCase();
        
        for (StatutTache s : values()) {
            // Match against: valeur ("todo"), name ("A_FAIRE"), or underscored/hyphenated variations ("a-faire", "en_revision")
            if (s.valeur.equalsIgnoreCase(cleanVal) || 
                s.name().equalsIgnoreCase(cleanVal) ||
                s.name().toLowerCase().replace("_", "-").equals(cleanVal)) {
                return s;
            }
        }
        
        // Fallback for common French variations if encountered
        if (cleanVal.contains("revision")) return EN_REVISION;
        if (cleanVal.contains("cours")) return EN_COURS;
        if (cleanVal.contains("faire") || cleanVal.contains("todo")) return A_FAIRE;
        if (cleanVal.contains("termine") || cleanVal.contains("fini") || cleanVal.contains("done")) return TERMINE;

        throw new IllegalArgumentException("Statut Tache inconnu : " + val);
    }

    @Override
    public String toString() {
        return valeur;
    }
}