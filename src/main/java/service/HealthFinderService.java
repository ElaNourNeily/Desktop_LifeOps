package service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.HealthArticle;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HealthFinderService {

    private static final String BASE_URL = "https://health.gov/myhealthfinder/api/v3/topicsearch.json";
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Recherche des articles de santé par mot-clé (ex: nutrition, heart, fitness)
     */
    public List<HealthArticle> searchArticles(String keyword) throws IOException {
        String url = BASE_URL + "?keyword=" + keyword + "&lang=en";
        
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return new ArrayList<>();

            String jsonData = response.body().string();
            return parseArticles(jsonData);
        }
    }

    private List<HealthArticle> parseArticles(String json) {
        List<HealthArticle> list = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject result = root.getAsJsonObject("Result");
            
            if (result.has("Resources")) {
                JsonArray resources = result.getAsJsonObject("Resources").getAsJsonArray("Resource");
                
                for (int i = 0; i < resources.size(); i++) {
                    JsonObject res = resources.get(i).getAsJsonObject();
                    list.add(new HealthArticle(
                        res.get("Title").getAsString(),
                        res.get("Categories").getAsString(),
                        res.get("AccessibleVersion").getAsString(),
                        res.has("ImageUrl") ? res.get("ImageUrl").getAsString() : null
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
