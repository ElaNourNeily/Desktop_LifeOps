package service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.DrugInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpenFDAService {

    private static final String BASE_URL = "https://api.fda.gov/drug/label.json";
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Recherche des informations sur un médicament par son nom (marque ou générique).
     */
    public List<DrugInfo> searchDrug(String drugName) throws IOException {
        // Encodage propre du nom pour l'URL
        String encodedName = java.net.URLEncoder.encode(drugName, "UTF-8");
        
        // Syntaxe OpenFDA : recherche par marque OU par nom générique
        String query = "openfda.brand_name:\"" + encodedName + "\"+openfda.generic_name:\"" + encodedName + "\"";
        String url = BASE_URL + "?search=" + query + "&limit=5";
        
        System.out.println("Appel API OpenFDA : " + url); // Debug
        
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("Erreur API (Code " + response.code() + ") : " + response.message());
                // Fallback sur une recherche plus simple en cas d'échec
                return fuzzySearch(encodedName);
            }

            String jsonData = response.body().string();
            return parseResponse(jsonData);
        }
    }

    private List<DrugInfo> fuzzySearch(String encodedName) throws IOException {
        String url = BASE_URL + "?search=brand_name:" + encodedName + "&limit=5";
        System.out.println("Tentative Fuzzy Search : " + url);
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("Échec Fuzzy Search (Code " + response.code() + ")");
                return new ArrayList<>();
            }
            return parseResponse(response.body().string());
        }
    }

    private List<DrugInfo> parseResponse(String json) {
        List<DrugInfo> results = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray jsonResults = root.getAsJsonArray("results");

            for (int i = 0; i < jsonResults.size(); i++) {
                JsonObject item = jsonResults.get(i).getAsJsonObject();
                
                // Extraction sécurisée des champs
                String brand = getField(item, "openfda", "brand_name");
                String generic = getField(item, "openfda", "generic_name");
                String indications = getField(item, "indications_and_usage");
                String effects = getField(item, "adverse_reactions");
                String dosage = getField(item, "dosage_and_administration");

                results.add(new DrugInfo(brand, generic, indications, effects, dosage));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private String getField(JsonObject item, String parent, String child) {
        try {
            if (item.has(parent) && item.getAsJsonObject(parent).has(child)) {
                return item.getAsJsonObject(parent).getAsJsonArray(child).get(0).getAsString();
            }
        } catch (Exception ignored) {}
        return "Non disponible";
    }

    private String getField(JsonObject item, String field) {
        try {
            if (item.has(field)) {
                return item.getAsJsonArray(field).get(0).getAsString();
            }
        } catch (Exception ignored) {}
        return "Non disponible";
    }
}
