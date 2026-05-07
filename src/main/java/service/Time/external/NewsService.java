package service.Time.external;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.Time.external.NewsArticle;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Service pour récupérer des actualités via NewsAPI.org.
 * Permet d'afficher du contenu pertinent selon la catégorie de l'activité.
 */
public class NewsService {

    // IMPORTANT: Remplacez par votre propre clé API NewsAPI.org
    private static final String API_KEY = "f384644ac2cd498d944804a4c444feeb"; 
    private static final String BASE_URL = "https://newsapi.org/v2/everything";

    private final HttpClient httpClient;

    public NewsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Récupère les actualités pour aujourd'hui selon une catégorie spécifique.
     */
    /**
     * Récupère une liste d'articles (conseils) selon le titre de l'activité et sa catégorie.
     */
    public List<NewsArticle> getActualitesParCategorie(String titre, String categorie, int nombreArticles) {
        List<NewsArticle> articles = new ArrayList<>();
        try {
            String query = buildSearchQuery(titre, categorie);
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            // Utilisation de qInTitle pour forcer la présence des mots-clés dans le titre de l'article
            String url = String.format(Locale.US, "%s?qInTitle=%s&language=fr&sortBy=relevancy&pageSize=%d&apiKey=%s", 
                                        BASE_URL, encodedQuery, nombreArticles, API_KEY);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                articles = parseNewsResponse(response.body());
            } else if (response.statusCode() == 429) {
                System.err.println("[NewsService] Limite de débit atteinte (Rate limit).");
            } else {
                System.err.println("[NewsService] Erreur API: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("[NewsService] Erreur réseau: " + e.getMessage());
        }
        return articles;
    }

    /**
     * Construit une requête de recherche combinant le titre, la catégorie et des mots-clés de "conseils".
     */
    private String buildSearchQuery(String titre, String categorie) {
        if (titre != null && titre.length() > 3) {
            // Si on a un titre assez long, on cherche directement le titre dans les titres d'articles
            return titre;
        }

        // Sinon on se base sur la catégorie avec des mots-clés de type "conseils"
        String catKeyword = getCategoryKeyword(categorie);
        return String.format("%s AND (conseils OR astuces OR guide)", catKeyword);
    }

    private String getCategoryKeyword(String categorie) {
        return switch (categorie) {
            case "Finance" -> "économie finance";
            case "Santé" -> "santé bien-être";
            case "Travail" -> "productivité travail";
            case "Loisir" -> "loisirs lifestyle";
            default -> "actualités";
        };
    }

    /**
     * Analyse le JSON et retourne une liste de NewsArticle.
     */
    private List<NewsArticle> parseNewsResponse(String json) {
        List<NewsArticle> articles = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray jsonArticles = root.getAsJsonArray("articles");

        for (int i = 0; i < jsonArticles.size(); i++) {
            JsonObject obj = jsonArticles.get(i).getAsJsonObject();
            
            String title = obj.get("title").isJsonNull() ? "Sans titre" : obj.get("title").getAsString();
            String desc = obj.get("description").isJsonNull() ? "" : obj.get("description").getAsString();
            String url = obj.get("url").isJsonNull() ? "" : obj.get("url").getAsString();
            String source = obj.getAsJsonObject("source").get("name").getAsString();
            String publishedAt = obj.get("publishedAt").getAsString();
            String imageUrl = obj.get("urlToImage").isJsonNull() ? null : obj.get("urlToImage").getAsString();

            articles.add(new NewsArticle(title, desc, url, source, publishedAt, imageUrl));
        }
        return articles;
    }
}
