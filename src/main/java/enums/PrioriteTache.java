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
        if (val == null) return MOYENNE;
        
        String cleanVal = val.trim().toLowerCase();
        
        for (PrioriteTache p : values()) {
            if (p.valeur.equalsIgnoreCase(cleanVal) || p.name().equalsIgnoreCase(cleanVal)) {
                return p;
            }
        }
        
        // Fallback
        if (cleanVal.contains("bas") || cleanVal.contains("low")) return BASSE;
        if (cleanVal.contains("moy") || cleanVal.contains("med")) return MOYENNE;
        if (cleanVal.contains("hau") || cleanVal.contains("high")) return HAUTE;
        if (cleanVal.contains("urg")) return URGENTE;

        throw new IllegalArgumentException("Priorité inconnue : " + val);
    }

    @Override
    public String toString() {
        return valeur;
    }
}