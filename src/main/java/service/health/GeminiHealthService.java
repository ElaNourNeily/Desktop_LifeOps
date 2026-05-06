package service.health;

import Model.health.BilanAnalyse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;

public class GeminiHealthService {

    private static final String API_KEY = "AIzaSyCyLnytdlMKXEmvjsbrXdt7W7eivDsKQSk";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public BilanAnalyse analyserDonnees(String promptDonnees) throws IOException {
        String fullPrompt = "Tu es un expert en santé et bien-être. Analyse ces données de suivi et retourne " +
                "UNIQUEMENT un objet JSON valide. Structure : { \"niveauFatigue\": int, \"niveauStress\": int, " +
                "\"scoreFormeGlobal\": int, \"risqueBurnout\": \"FAIBLE/MODERE/ELEVE/CRITIQUE\", " +
                "\"recommandations\": [\"string\"] }. Données : " + promptDonnees;

        String jsonRequest = "{ \"contents\": [{ \"parts\":[{ \"text\": \"" +
                fullPrompt.replace("\"", "\\\"").replace("\n", " ") + "\" }] }] }";

        RequestBody body = RequestBody.create(jsonRequest, MediaType.parse("application/json; charset=utf-8"));
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

        Request request = new Request.Builder().url(url).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) throw new IOException("Erreur API Gemini (" + response.code() + "): " + responseBody);

            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            String aiText = root.getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content").getAsJsonArray("parts")
                    .get(0).getAsJsonObject().get("text").getAsString();

            aiText = aiText.replace("```json", "").replace("```", "").trim();
            return gson.fromJson(aiText, BilanAnalyse.class);
        }
    }
}
