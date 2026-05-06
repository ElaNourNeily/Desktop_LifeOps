package Model.health;

public class DrugInfo {
    private String brandName;
    private String genericName;
    private String indications;
    private String adverseEffects;
    private String dosage;

    public DrugInfo(String brandName, String genericName, String indications,
                    String adverseEffects, String dosage) {
        this.brandName = brandName; this.genericName = genericName;
        this.indications = indications; this.adverseEffects = adverseEffects;
        this.dosage = dosage;
    }

    public String getBrandName() { return brandName; }
    public String getGenericName() { return genericName; }
    public String getIndications() { return indications; }
    public String getAdverseEffects() { return adverseEffects; }
    public String getDosage() { return dosage; }
}
