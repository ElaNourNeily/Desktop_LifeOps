package service;

import model.SuiviSante;

import java.util.List;
import java.util.stream.Collectors;

public class HealthDataService {
    private final SuiviSanteService suiviSanteService;

    public HealthDataService() {
        this.suiviSanteService = new SuiviSanteService();
    }

    public List<SuiviSanteDTO> getRecentData() {
        try {
            // Fetch top 7 recent records or so, but let's fetch all and keep last 7 by date
            List<SuiviSante> tous = suiviSanteService.trier(); // trier() orders by date desc usually
            return tous.stream()
                .limit(7)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
    
    // Pour Radar Chart
    public double getMoyenneSommeil() {
        return getRecentData().stream().mapToDouble(SuiviSanteDTO::heuresSommeil).average().orElse(0);
    }
    public double getMoyenneActivite() {
        return getRecentData().stream().mapToDouble(SuiviSanteDTO::minutesActivite).average().orElse(0) / 60.0;
    }
    public double getMoyenneEau() {
        return getRecentData().stream().mapToDouble(SuiviSanteDTO::verresEau).average().orElse(0);
    }
    public double getMoyenneHumeur() {
        return getRecentData().stream().mapToDouble(SuiviSanteDTO::humeur).average().orElse(0);
    }

    private SuiviSanteDTO convertToDTO(SuiviSante s) {
        double score = calculateScoreForme(s);
        String burnout = calculateBurnout(s);
        return new SuiviSanteDTO(
            s.getDate().toString(),
            s.getHeuresSommeil(),
            s.getMinutesActivite(),
            s.getVerresEau(),
            s.getHumeur(),
            score,
            burnout
        );
    }

    private double calculateScoreForme(SuiviSante s) {
        // Formule personnalisée
        return (s.getHeuresSommeil() * 5) + (s.getMinutesActivite() * 0.2) + (s.getVerresEau() * 2) + (s.getHumeur() * 3);
    }

    private String calculateBurnout(SuiviSante s) {
        if (s.getHeuresSommeil() < 5 && s.getHumeur() < 4) return "CRITIQUE";
        if (s.getHeuresSommeil() < 6 && s.getHumeur() < 6) return "ELEVE";
        if (s.getHeuresSommeil() < 7) return "MODERE";
        return "FAIBLE";
    }

    public record SuiviSanteDTO(
        String date, 
        double heuresSommeil, 
        int minutesActivite, 
        int verresEau, 
        int humeur, 
        double scoreForme, 
        String risqueBurnout
    ) {}
}
