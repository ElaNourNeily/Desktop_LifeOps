package service.Time;

import com.google.gson.*;
import model.Time.Activite;
import model.Time.Planning;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public class AIService {

    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;
    private final PlanningService planningService = new PlanningService();
    private final ActiviteService activiteService = new ActiviteService();

    public AIService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public JsonObject optimizePlanning(String userRequest, LocalDate startDate, LocalDate endDate, int userId) throws Exception {
        // 1. Collect current data
        List<Planning> plannings = planningService.recupererParPeriode(userId, java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate));
        String currentData = formatCurrentPlanning(plannings);

        // 2. Build Prompt
        String prompt = buildPrompt(userRequest, currentData);

        // 3. Prepare Payload
        JsonObject payload = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject parts = new JsonObject();
        parts.addProperty("text", prompt);
        JsonArray partsArray = new JsonArray();
        partsArray.add(parts);
        JsonObject contentObj = new JsonObject();
        contentObj.add("parts", partsArray);
        contents.add(contentObj);
        payload.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("responseMimeType", "application/json");
        payload.add("generationConfig", generationConfig);

        // 4. Send Request with Retry Logic (Matching Symfony implementation)
        int maxRetries = 3;
        long retryDelay = 2000; // start with 2 seconds

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                        .timeout(Duration.ofSeconds(60))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // 5. Parse Response
                    JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonArray candidates = responseJson.getAsJsonArray("candidates");
                    if (candidates == null || candidates.isEmpty()) {
                        throw new Exception("L'IA n'a pas renvoyé de candidat.");
                    }

                    String contentText = candidates.get(0).getAsJsonObject()
                            .getAsJsonObject("content")
                            .getAsJsonArray("parts").get(0).getAsJsonObject()
                            .get("text").getAsString();

                    return JsonParser.parseString(contentText).getAsJsonObject();
                }

                // If Error 503 (High Demand) or 429 (Quota), retry
                if ((response.statusCode() == 503 || response.statusCode() == 429) && attempt < maxRetries) {
                    Thread.sleep(retryDelay);
                    retryDelay *= 2; // Exponential backoff
                    continue;
                }

                String errorMsg = response.body();
                if (response.statusCode() == 429) {
                    errorMsg = "Quota épuisé. Le plan gratuit Gemini est limité. Veuillez attendre 60 secondes.";
                } else if (response.statusCode() == 503) {
                    errorMsg = "Le service Gemini est temporairement surchargé. Veuillez réessayer dans quelques instants.";
                }
                throw new Exception("Erreur API Gemini (" + response.statusCode() + "): " + errorMsg);

            } catch (Exception e) {
                if (attempt == maxRetries) throw e;
                Thread.sleep(retryDelay);
                retryDelay *= 2;
            }
        }
        throw new Exception("Nombre maximum de tentatives atteint.");
    }

    private String formatCurrentPlanning(List<Planning> plannings) {
        if (plannings == null || plannings.isEmpty()) {
            return "No activities scheduled for this period.";
        }

        StringBuilder sb = new StringBuilder("Current week plannings:\n");
        for (Planning p : plannings) {
            sb.append("- Date: ").append(p.getDate().toString()).append("\n");
            sb.append("  Hours: ").append(p.getHeureDebutJournee().toString().substring(0, 5))
              .append(" to ").append(p.getHeureFinJournee().toString().substring(0, 5)).append("\n");
            
            try {
                List<Activite> activities = activiteService.recupererParPlanning(p.getId());
                for (Activite a : activities) {
                    String priority = switch (a.getPriorite()) {
                        case 1 -> "Basse";
                        case 2 -> "Moyenne";
                        case 3 -> "Haute";
                        default -> "Inconnue";
                    };
                    sb.append("  - Activity: ").append(a.getTitre())
                      .append(" (Category: ").append(a.getCategorie())
                      .append(", Priority: ").append(priority)
                      .append(", Urgency: ").append(a.getNiveauUrgence()).append(")\n");
                    sb.append("    Time: ").append(a.getHeureDebutEstimee().toString().substring(0, 5))
                      .append(" - ").append(a.getHeureFinEstimee().toString().substring(0, 5)).append("\n");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    private String buildPrompt(String userRequest, String currentData) {
        String today = LocalDate.now().toString();
        return "User request: " + userRequest + "\n" +
                "Context: Today is " + today + ". " +
                "The users planning for the requested period starts from " + today + " onwards unless specified.\n\n" +
                currentData + "\n\n" +
                "Optimize the planning based on the user request.\n" +
                "CRITICAL RULES:\n" +
                "1. CORRECT DATES: Your response MUST use dates in YYYY-MM-DD format and they MUST match the current year/date context provided above.\n" +
                "2. NO OVERLAPS: Ensure no two activities overlap. Each time slot must be exclusive.\n" +
                "3. RESPECT EXISTING: If an activity exists and isn't modified, do not schedule something over it.\n" +
                "4. RESOLVE CONFLICTS: If a conflict arises, move one task or suggest a resolution.\n" +
                "5. FIXED CATEGORIES: Use ONLY: Travail, Personnel, Urgent, Loisir, Santé.\n" +
                "6. FIXED COLORS: Use ONLY: #10b981, #3b82f6, #f59e0b, #ec4899, #8b5cf6.\n" +
                "7. URGENCY: Use only 'faible', 'moyen', or 'eleve'.\n" +
                "8. BREAKS: For free_slots, use category 'Repos' and color '#9ca3af'.\n\n" +
                "Respond strictly as JSON with the following structure:\n" +
                "{\n" +
                "    \"suggestions\": [\n" +
                "        {\n" +
                "            \"date\": \"YYYY-MM-DD\",\n" +
                "            \"new_activities\": [\n" +
                "                {\"title\": \"...\", \"start\": \"HH:mm\", \"end\": \"HH:mm\", \"priority\": 1-3, \"urgency\": \"faible|moyen|eleve\", \"category\": \"...\", \"color\": \"#hex\", \"reason\": \"...\"}\n" +
                "            ],\n" +
                "            \"modifications\": [\n" +
                "                {\"original_title\": \"...\", \"new_start\": \"HH:mm\", \"new_end\": \"HH:mm\", \"reason\": \"...\"}\n" +
                "            ],\n" +
                "            \"free_slots\": [\n" +
                "                {\"start\": \"HH:mm\", \"end\": \"HH:mm\", \"recommendation\": \"...\"}\n" +
                "            ]\n" +
                "        }\n" +
                "    ],\n" +
                "    \"summary\": \"Summary of optimization.\"\n" +
                "}";
    }

    public JsonObject saveConfirmedSuggestions(JsonObject response, int userId, String userPrompt) throws SQLException {
        JsonArray suggestions = response.getAsJsonArray("suggestions");
        if (suggestions == null) return null;

        int totalAdds = 0;
        int totalMods = 0;
        int totalDeletes = 0;

        for (JsonElement dayElement : suggestions) {
            JsonObject daySuggestion = dayElement.getAsJsonObject();
            String dateStr = daySuggestion.get("date").getAsString();
            LocalDate date = LocalDate.parse(dateStr);

            // Find or create Planning
            Planning planning = planningService.recupererParDate(date, userId);
            if (planning == null) {
                planning = new Planning(java.sql.Date.valueOf(date), true, Time.valueOf("08:00:00"), Time.valueOf("18:00:00"), userId);
                planningService.ajouter(planning);
            } else {
                // Count existing UI suggestions for report
                totalDeletes += countSuggestionsIA(planning.getId());
                // Clear existing AI suggestions for this specific planning to avoid duplicates
                activiteService.supprimerSuggestionsIA(planning.getId());
            }

            // 1. New Activities
            JsonArray newActs = daySuggestion.getAsJsonArray("new_activities");
            if (newActs != null) {
                for (JsonElement actElem : newActs) {
                    JsonObject actData = actElem.getAsJsonObject();
                    Activite a = new Activite();
                    a.setTitre(actData.get("title").getAsString());
                    a.setHeureDebutEstimee(Time.valueOf(actData.get("start").getAsString() + ":00"));
                    a.setHeureFinEstimee(Time.valueOf(actData.get("end").getAsString() + ":00"));
                    a.setPriorite(actData.get("priority").getAsInt());
                    a.setNiveauUrgence(actData.get("urgency").getAsString());
                    a.setCategorie(actData.get("category").getAsString());
                    a.setCouleur(actData.get("color").getAsString());
                    a.setPlanningId(planning.getId());
                    a.setSuggestedByAi(true);
                    
                    long mins = Duration.between(a.getHeureDebutEstimee().toLocalTime(), a.getHeureFinEstimee().toLocalTime()).toMinutes();
                    a.setDuree((int) mins);
                    
                    activiteService.ajouter(a);
                    totalAdds++;
                }
            }

            // 2. Free Slots (as Repos activities)
            JsonArray freeSlots = daySuggestion.getAsJsonArray("free_slots");
            if (freeSlots != null) {
                for (JsonElement slotElem : freeSlots) {
                    JsonObject slotData = slotElem.getAsJsonObject();
                    Activite a = new Activite();
                    a.setTitre(slotData.get("recommendation").getAsString());
                    a.setHeureDebutEstimee(Time.valueOf(slotData.get("start").getAsString() + ":00"));
                    a.setHeureFinEstimee(Time.valueOf(slotData.get("end").getAsString() + ":00"));
                    a.setPriorite(1);
                    a.setNiveauUrgence("faible");
                    a.setCategorie("Repos");
                    a.setCouleur("#9ca3af");
                    a.setPlanningId(planning.getId());
                    a.setSuggestedByAi(true);
                    
                    long mins = Duration.between(a.getHeureDebutEstimee().toLocalTime(), a.getHeureFinEstimee().toLocalTime()).toMinutes();
                    a.setDuree((int) mins);
                    
                    activiteService.ajouter(a);
                    totalAdds++;
                }
            }

            // 3. Modifications
            JsonArray mods = daySuggestion.getAsJsonArray("modifications");
            if (mods != null) {
                List<Activite> currentActivities = activiteService.recupererParPlanning(planning.getId());
                for (JsonElement modElem : mods) {
                    JsonObject modData = modElem.getAsJsonObject();
                    String origTitle = modData.get("original_title").getAsString();
                    
                    for (Activite existing : currentActivities) {
                        if (existing.getTitre().equalsIgnoreCase(origTitle)) {
                            existing.setHeureDebutEstimee(Time.valueOf(modData.get("new_start").getAsString() + ":00"));
                            existing.setHeureFinEstimee(Time.valueOf(modData.get("new_end").getAsString() + ":00"));
                            
                            long mins = Duration.between(existing.getHeureDebutEstimee().toLocalTime(), existing.getHeureFinEstimee().toLocalTime()).toMinutes();
                            existing.setDuree((int) mins);
                            existing.setSuggestedByAi(true);
                            
                            activiteService.modifier(existing);
                            totalMods++;
                            break;
                        }
                    }
                }
            }
        }

        String summary = response.has("summary") ? response.get("summary").getAsString() : "Optimisation terminée";
        
        // Save History
        sauvegarderHistorique(userId, userPrompt, summary, totalAdds, totalMods, totalDeletes, gson.toJson(response));

        JsonObject report = new JsonObject();
        report.addProperty("nb_ajouts", totalAdds);
        report.addProperty("nb_modifications", totalMods);
        report.addProperty("nb_suppressions", totalDeletes);
        report.addProperty("resume", summary);
        return report;
    }

    private int countSuggestionsIA(int planningId) {
        try {
            String sql = "SELECT COUNT(*) FROM activite WHERE planning_id = ? AND suggested_by_ai = 1";
            java.sql.PreparedStatement ps = utils.MyDatabase.getInstance().getConnection().prepareStatement(sql);
            ps.setInt(1, planningId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private void sauvegarderHistorique(int userId, String prompt, String resume, int adds, int mods, int deletes, String details) {
        String sql = "INSERT INTO historique_optimisation_ia (user_id, prompt_utilisateur, resume, nb_ajouts, nb_modifications, nb_suppressions, details) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (java.sql.PreparedStatement ps = utils.MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, prompt);
            ps.setString(3, resume);
            ps.setInt(4, adds);
            ps.setInt(5, mods);
            ps.setInt(6, deletes);
            ps.setString(7, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
