package service.Time.external;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Service pour récupérer les données météo via l'API Open-Meteo.
 * Ce service permet d'aider l'utilisateur à planifier ses activités en fonction du temps.
 */
public class WeatherService {

    private final HttpClient httpClient;
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast";

    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Récupère la météo pour aujourd'hui aux coordonnées spécifiées.
     */
    public String getMeteoAujourdHui(double lat, double lon) {
        return getMeteoPourDate(lat, lon, LocalDate.now());
    }

    /**
     * Récupère la météo pour demain aux coordonnées spécifiées.
     */
    public String getMeteoDemain(double lat, double lon) {
        return getMeteoPourDate(lat, lon, LocalDate.now().plusDays(1));
    }

    /**
     * Récupère la météo pour une date spécifique (dans la limite des prévisions de 7 jours).
     */
    public String getMeteoPourDate(double lat, double lon, LocalDate date) {
        try {
            String url = String.format(Locale.US, "%s?latitude=%f&longitude=%f&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto", 
                                        API_URL, lat, lon);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseWeatherResponse(response.body(), date);
            } else {
                return "Erreur météo (" + response.statusCode() + ")";
            }
        } catch (Exception e) {
            System.err.println("[WeatherService] Erreur: " + e.getMessage());
            return "Météo indisponible ⚠️";
        }
    }

    /**
     * Analyse le JSON de l'API et extrait les données pour la date demandée.
     */
    private String parseWeatherResponse(String json, LocalDate targetDate) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject daily = root.getAsJsonObject("daily");
        JsonArray times = daily.getAsJsonArray("time");

        int index = -1;
        String targetDateStr = targetDate.toString();
        for (int i = 0; i < times.size(); i++) {
            if (times.get(i).getAsString().equals(targetDateStr)) {
                index = i;
                break;
            }
        }

        if (index == -1) return "Date hors prévisions 📅";

        int code = daily.getAsJsonArray("weather_code").get(index).getAsInt();
        double maxTemp = daily.getAsJsonArray("temperature_2m_max").get(index).getAsDouble();
        double minTemp = daily.getAsJsonArray("temperature_2m_min").get(index).getAsDouble();

        return String.format("%s | Max: %.1f°C | Min: %.1f°C", interpretWeatherCode(code), maxTemp, minTemp);
    }

    /**
     * Convertit le code WMO d'Open-Meteo en une description textuelle claire avec emoji.
     */
    public String interpretWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Ciel dégagé ☀️";
            case 1, 2, 3 -> "Partiellement nuageux 🌤️";
            case 45, 48 -> "Brouillard 🌫️";
            case 51, 53, 55 -> "Bruine légère 🌦️";
            case 61, 63 -> "Pluie modérée 🌧️";
            case 65 -> "Forte pluie 🌧️";
            case 71, 73, 75 -> "Neige ❄️";
            case 77 -> "Grains de neige ❄️";
            case 80, 81, 82 -> "Averses de pluie 🌧️";
            case 85, 86 -> "Averses de neige ❄️";
            case 95 -> "Orage ⛈️";
            case 96, 99 -> "Orage et grêle ⛈️";
            default -> "Météo inconnue ❓";
        };
    }

    /**
     * Récupère un rapport complet (actuel, horaire, journalier) pour une vue détaillée.
     */
    public JsonObject getFullWeatherReport(double lat, double lon) {
        try {
            String url = String.format(Locale.US, 
                "%s?latitude=%f&longitude=%f&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code&hourly=temperature_2m,weather_code,precipitation_probability&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto", 
                API_URL, lat, lon);

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return JsonParser.parseString(response.body()).getAsJsonObject();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    /**
     * Vérifie si le temps est considéré comme "mauvais" (Pluie, Orage, Neige).
     */
    public boolean isBadWeather(int code) {
        return code >= 51;
    }
    
    /**
     * Récupère le code brut pour analyse métier.
     */
    public int getRawWeatherCode(double lat, double lon, LocalDate date) {
        try {
            String url = String.format(Locale.US, "%s?latitude=%f&longitude=%f&daily=weather_code&timezone=auto", API_URL, lat, lon);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject daily = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("daily");
                JsonArray codes = daily.getAsJsonArray("weather_code");
                return codes.get(0).getAsInt();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }
}
