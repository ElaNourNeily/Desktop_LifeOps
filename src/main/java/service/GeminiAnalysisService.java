package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.BilanAnalyse;
import okhttp3.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GeminiAnalysisService {

    private static final String MODEL_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private String apiKey = "AIzaSyCyLnytdlMKXEmvjsbrXdt7W7eivDsKQSk"; // Clé intégrée directement
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public GeminiAnalysisService() {
        // Tentative de chargement depuis le fichier, sinon garde la clé par défaut
        loadApiKey();
    }

    private void loadApiKey() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                String key = prop.getProperty("gemini.api.key");
                if (key != null && !key.isEmpty()) {
                    this.apiKey = key;
                }
            }
        } catch (Exception ignored) {}
    }

    public BilanAnalyse analyserDonnees(String promptDonnees) throws IOException {
        String fullPrompt = "Tu es un expert en santé et bien-être. Analyse ces données de suivi et retourne UNIQUEMENT un objet JSON valide. " +
                "Structure du JSON : { \"niveauFatigue\": int, \"niveauStress\": int, \"scoreForme\": int, \"risqueBurnout\": \"FAIBLE/MODERE/ELEVE/CRITIQUE\", \"recommandations\": [\"string\"] }. " +
                "Données : " + promptDonnees;

        // Corps de la requête
        String jsonRequest = "{ \"contents\": [{ \"parts\":[{ \"text\": \"" + fullPrompt.replace("\"", "\\\"").replace("\n", " ") + "\" }] }] }";

        RequestBody body = RequestBody.create(
                jsonRequest,
                MediaType.parse("application/json; charset=utf-8")
        );

        // Utilisation du dernier modèle Gemini Flash disponible
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "Corps vide";
            
            if (!response.isSuccessful()) {
                // On affiche le corps de l'erreur pour comprendre le 404
                throw new IOException("Erreur API Gemini (Code " + response.code() + ") : " + responseBody);
            }

            // Extraction du texte JSON de la réponse Gemini
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            String aiText = root.getAsJsonArray("candidates")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("content")
                                .getAsJsonArray("parts")
                                .get(0).getAsJsonObject()
                                .get("text").getAsString();
            
            aiText = aiText.replace("```json", "").replace("```", "").trim();
            return gson.fromJson(aiText, BilanAnalyse.class);
        }
    }
}
