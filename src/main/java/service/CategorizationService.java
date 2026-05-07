package service;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class CategorizationService {

    private Map<String, List<String>> categoryRules;

    public CategorizationService() {
        categoryRules = new HashMap<>();
        initializeDefaultRules();
    }

    private void initializeDefaultRules() {
        // Alimentation category (Food)
        List<String> foodKeywords = new ArrayList<>();
        foodKeywords.add("mcdonald");
        foodKeywords.add("burger");
        foodKeywords.add("pizza");
        foodKeywords.add("restaurant");
        foodKeywords.add("cafe");
        foodKeywords.add("coffee");
        foodKeywords.add("lunch");
        foodKeywords.add("dinner");
        foodKeywords.add("food");
        foodKeywords.add("meal");
        categoryRules.put("Alimentation", foodKeywords);

        // Transport category
        List<String> transportKeywords = new ArrayList<>();
        transportKeywords.add("uber");
        transportKeywords.add("taxi");
        transportKeywords.add("bus");
        transportKeywords.add("train");
        transportKeywords.add("flight");
        transportKeywords.add("gas");
        transportKeywords.add("fuel");
        transportKeywords.add("parking");
        transportKeywords.add("transport");
        categoryRules.put("Transport", transportKeywords);

        // Shopping category
        List<String> shoppingKeywords = new ArrayList<>();
        shoppingKeywords.add("shopping");
        shoppingKeywords.add("achat");
        shoppingKeywords.add("store");
        shoppingKeywords.add("magasin");
        categoryRules.put("Shopping", shoppingKeywords);

        // Sante category (Health)
        List<String> healthKeywords = new ArrayList<>();
        healthKeywords.add("medecin");
        healthKeywords.add("doctor");
        healthKeywords.add("pharmacie");
        healthKeywords.add("pharmacy");
        healthKeywords.add("sante");
        healthKeywords.add("health");
        categoryRules.put("Sante", healthKeywords);

        // Loisirs category (Leisure)
        List<String> leisureKeywords = new ArrayList<>();
        leisureKeywords.add("cinema");
        leisureKeywords.add("movie");
        leisureKeywords.add("theater");
        leisureKeywords.add("concert");
        leisureKeywords.add("loisir");
        leisureKeywords.add("leisure");
        categoryRules.put("Loisirs", leisureKeywords);

        // Abonnements category (Subscriptions)
        List<String> subscriptionKeywords = new ArrayList<>();
        subscriptionKeywords.add("abonnement");
        subscriptionKeywords.add("subscription");
        subscriptionKeywords.add("netflix");
        subscriptionKeywords.add("spotify");
        subscriptionKeywords.add("prime");
        categoryRules.put("Abonnements", subscriptionKeywords);

        // Bills category
        List<String> billsKeywords = new ArrayList<>();
        billsKeywords.add("facture");
        billsKeywords.add("bill");
        billsKeywords.add("electricite");
        billsKeywords.add("electricity");
        billsKeywords.add("eau");
        billsKeywords.add("water");
        categoryRules.put("Bills", billsKeywords);

        // Rent category
        List<String> rentKeywords = new ArrayList<>();
        rentKeywords.add("loyer");
        rentKeywords.add("rent");
        rentKeywords.add("location");
        categoryRules.put("Rent", rentKeywords);

        // Education category
        List<String> educationKeywords = new ArrayList<>();
        educationKeywords.add("ecole");
        educationKeywords.add("school");
        educationKeywords.add("universite");
        educationKeywords.add("university");
        educationKeywords.add("cours");
        educationKeywords.add("course");
        educationKeywords.add("education");
        categoryRules.put("Education", educationKeywords);

        // Autre as default
        categoryRules.put("Autre", new ArrayList<>());
    }

    public CategorizationResult categorizeExpense(String description) {
        String lowerDesc = description.toLowerCase();
        String bestMatch = "Other";
        int maxMatches = 0;
        List<String> matchedKeywords = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : categoryRules.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();

            List<String> matches = new ArrayList<>();
            for (String keyword : keywords) {
                if (lowerDesc.contains(keyword)) {
                    matches.add(keyword);
                }
            }

            if (matches.size() > maxMatches) {
                maxMatches = matches.size();
                bestMatch = category;
                matchedKeywords = matches;
            }
        }

        // Calculate confidence
        int confidence = Math.min(100, (maxMatches * 20) + (matchedKeywords.size() > 0 ? 30 : 0));

        return new CategorizationResult(bestMatch, confidence, matchedKeywords);
    }

    public void addCategoryRule(String categoryName, List<String> keywords) {
        categoryRules.put(categoryName, keywords);
    }

    public List<String> getAvailableCategories() {
        return new ArrayList<>(categoryRules.keySet());
    }

    public static class CategorizationResult {
        private String categoryName;
        private int confidence;
        private List<String> matchedKeywords;

        public CategorizationResult(String categoryName, int confidence, List<String> matchedKeywords) {
            this.categoryName = categoryName;
            this.confidence = confidence;
            this.matchedKeywords = matchedKeywords;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public int getConfidence() {
            return confidence;
        }

        public List<String> getMatchedKeywords() {
            return matchedKeywords;
        }
    }
}