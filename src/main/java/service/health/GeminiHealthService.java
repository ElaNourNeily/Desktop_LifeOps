package service.health;

import Model.health.BilanAnalyse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GeminiHealthService {

    private static final String MODEL_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    // Fallback key — loaded from config.properties if available
    private String apiKey = "";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public GeminiHealthService() {
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
        String fullPrompt =
                "Tu es un expert en santé et bien-être. Analyse ces données de suivi et retourne " +
                "UNIQUEMENT un objet JSON valide. " +
                "Structure du JSON : { \"niveauFatigue\": int, \"niveauStress\": int, " +
                "\"scoreFormeGlobal\": int, \"risqueBurnout\": \"FAIBLE/MODERE/ELEVE/CRITIQUE\", " +
                "\"recommandations\": [\"string\"] }. " +
                "Données : " + promptDonnees;

        String jsonRequest = "{ \"contents\": [{ \"parts\":[{ \"text\": \""
                + fullPrompt.replace("\"", "\\\"").replace("\n", " ")
                + "\" }] }] }";

        RequestBody body = RequestBody.create(
                jsonRequest,
                MediaType.parse("application/json; charset=utf-8"));

        String url = MODEL_URL + "?key=" + apiKey;

        Request request = new Request.Builder().url(url).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "Corps vide";

            if (!response.isSuccessful()) {
                throw new IOException("Erreur API Gemini (Code " + response.code() + ") : " + responseBody);
            }

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
