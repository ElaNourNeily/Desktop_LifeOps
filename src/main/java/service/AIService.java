package service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.User;
import model.task.Recommendation;
import model.task.Tache;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AIService {
    private static final String API_URL = "http://localhost:8000/predict";
    // Using Gson/JsonParser instead of Jackson

    public List<Recommendation> getRecommendations(Tache task, List<User> users) {
        List<Recommendation> recommendations = new ArrayList<>();
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Construct JSON request
            StringBuilder jsonInputString = new StringBuilder();
            jsonInputString.append("{");
            jsonInputString.append("\"task\": {");
            jsonInputString.append("\"difficulty\": ").append(task.getDifficulte()).append(",");
            jsonInputString.append("\"category\": \"Development\","); // Defaulting category for now
            jsonInputString.append("\"estimated_time\": 10.0,"); // Assuming a default if not tracked
            jsonInputString.append("\"deadline_days\": 3"); // Default
            jsonInputString.append("},");
            jsonInputString.append("\"users\": [");
            
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                jsonInputString.append("{");
                jsonInputString.append("\"id\": ").append(user.getId()).append(",");
                // Mocking data based on user, in a real app these would be computed from history
                jsonInputString.append("\"avg_time\": ").append(8.0).append(",");
                jsonInputString.append("\"workload\": ").append(0.3).append(",");
                jsonInputString.append("\"reliability\": ").append(0.9);
                jsonInputString.append("}");
                if (i < users.size() - 1) {
                    jsonInputString.append(",");
                }
            }
            jsonInputString.append("]");
            jsonInputString.append("}");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() == 200) {
                java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name());
                String responseBody = scanner.hasNext() ? scanner.useDelimiter("\\A").next() : "";
                scanner.close();

                JsonObject rootObject = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonArray recommendationsArray = rootObject.getAsJsonArray("recommendations");
                
                if (recommendationsArray != null) {
                    for (JsonElement element : recommendationsArray) {
                        JsonObject node = element.getAsJsonObject();
                        Recommendation rec = new Recommendation();
                        rec.setUserId(node.get("user_id").getAsInt());
                        rec.setScore(node.get("score").getAsDouble());
                        rec.setPredictedTime(node.get("predicted_time").getAsDouble());
                        rec.setReason(node.get("reason").getAsString());
                        recommendations.add(rec);
                    }
                }
            } else {
                System.err.println("AI Service Error: HTTP " + conn.getResponseCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return recommendations;
    }
}
