package service.health;

import Model.health.HealthArticle;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HealthFinderService {

    private final OkHttpClient client = new OkHttpClient();

    public List<HealthArticle> searchArticles(String query) throws IOException {
        List<HealthArticle> articles = new ArrayList<>();
        String url = "https://health.gov/myhealthfinder/api/v3/topicsearch.json?lang=en&keyword=" +
                query.replace(" ", "+");

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return articles;
            String body = response.body().string();
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();

            JsonObject result = root.getAsJsonObject("Result");
            if (result == null) return articles;
            JsonObject resources = result.getAsJsonObject("Resources");
            if (resources == null) return articles;
            JsonArray items = resources.getAsJsonArray("Resource");
            if (items == null) return articles;

            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                String title = item.has("Title") ? item.get("Title").getAsString() : "Sans titre";
                String categories = item.has("Categories") ? item.get("Categories").getAsString() : "";
                String accessUrl = item.has("AccessibleVersion") ? item.get("AccessibleVersion").getAsString() : "";
                String imageUrl = "";
                articles.add(new HealthArticle(title, categories, accessUrl, imageUrl));
            }
        }
        return articles;
    }
}
