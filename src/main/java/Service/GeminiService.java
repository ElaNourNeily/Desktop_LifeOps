package Service;

import Model.PlanAction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class GeminiService {

    private static final String API_KEY = "YOUR_GROQ_API_KEY_HERE";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";

    public List<PlanAction> suggererPlans(String titreObjectif, String categorieObjectif)
            throws IOException, InterruptedException {
        String prompt = construirePrompt(titreObjectif, categorieObjectif);
        String responseBody = appellerGroq(prompt);
        return parserReponse(responseBody);
    }

    private String construirePrompt(String titre, String categorie) {
        return """
            Tu es un assistant de productivité personnelle.
            L'utilisateur a un objectif intitulé : "%s" dans la catégorie "%s".

            Génère exactement 5 plans d'action concrets et réalistes pour atteindre cet objectif.

            Réponds UNIQUEMENT avec un tableau JSON valide, sans texte avant ni après, sans markdown.
            Chaque élément doit avoir exactement ces 3 champs :
            - "titre" : titre court du plan (max 60 caractères)
            - "description" : description détaillée et actionnable (max 150 caractères)
            - "priorite" : exactement "Haute", "Moyenne" ou "Basse"

            Exemple de format attendu :
            [
              {"titre": "...", "description": "...", "priorite": "Haute"},
              {"titre": "...", "description": "...", "priorite": "Moyenne"}
            ]

            Réponds en français uniquement.
            """.formatted(titre, categorie != null ? categorie : "Général");
    }

    private String appellerGroq(String prompt) throws IOException, InterruptedException {
        // Corps de la requête au format OpenAI
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.put(message);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1024);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorMsg;
            try {
                JSONObject errorJson = new JSONObject(response.body());
                errorMsg = errorJson.getJSONObject("error").optString("message", response.body());
                if (errorMsg.contains("rate_limit") || errorMsg.contains("quota")) {
                    errorMsg = "Quota API dépassé. Attendez quelques secondes et réessayez.";
                }
            } catch (Exception e) {
                errorMsg = response.body();
            }
            throw new IOException(errorMsg);
        }

        return response.body();
    }

    private List<PlanAction> parserReponse(String responseBody) {
        List<PlanAction> suggestions = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(responseBody);
            // Format OpenAI : choices[0].message.content
            String texte = json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();

            // Nettoyer les balises markdown si présentes
            texte = texte.replaceAll("(?s)```json", "").replaceAll("(?s)```", "").trim();

            JSONArray plansJson = new JSONArray(texte);

            for (int i = 0; i < plansJson.length(); i++) {
                JSONObject planJson = plansJson.getJSONObject(i);
                PlanAction plan = new PlanAction();
                plan.setTitre(planJson.optString("titre", "Plan " + (i + 1)));
                plan.setDescription(planJson.optString("description", ""));
                plan.setPriorite(normaliserPriorite(planJson.optString("priorite", "Moyenne")));
                plan.setStatut("À faire");
                suggestions.add(plan);
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing réponse Groq : " + e.getMessage());
        }
        return suggestions;
    }

    private String normaliserPriorite(String priorite) {
        return switch (priorite.toLowerCase().trim()) {
            case "haute", "high", "élevée", "elevee" -> "Haute";
            case "basse", "low", "faible"             -> "Basse";
            default                                    -> "Moyenne";
        };
    }
}
