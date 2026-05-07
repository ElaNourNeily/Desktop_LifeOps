package service.health;

import Model.health.DrugInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpenFDAService {

    private final OkHttpClient client = new OkHttpClient();

    public List<DrugInfo> searchDrug(String query) throws IOException {
        List<DrugInfo> results = new ArrayList<>();
        String url = "https://api.fda.gov/drug/label.json?search=brand_name:" +
                query.replace(" ", "+") + "&limit=5";

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return results;
            String body = response.body().string();
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray results_arr = root.getAsJsonArray("results");

            for (int i = 0; i < results_arr.size(); i++) {
                JsonObject item = results_arr.get(i).getAsJsonObject();
                JsonObject openfda = item.has("openfda") ? item.getAsJsonObject("openfda") : new JsonObject();

                String brand = getFirst(openfda, "brand_name");
                String generic = getFirst(openfda, "generic_name");
                String indications = getFirstField(item, "indications_and_usage");
                String adverse = getFirstField(item, "adverse_reactions");
                String dosage = getFirstField(item, "dosage_and_administration");

                results.add(new DrugInfo(brand, generic, indications, adverse, dosage));
            }
        }
        return results;
    }

    private String getFirst(JsonObject obj, String key) {
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray(key);
            if (arr.size() > 0) return arr.get(0).getAsString();
        }
        return "—";
    }

    private String getFirstField(JsonObject obj, String key) {
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray(key);
            if (arr.size() > 0) {
                String text = arr.get(0).getAsString();
                return text.length() > 300 ? text.substring(0, 300) + "..." : text;
            }
        }
        return "Information non disponible.";
    }
}
