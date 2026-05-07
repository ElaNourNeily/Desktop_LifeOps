package utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurrencyUtils {

    private static final String API_KEY = "aabae1f68e45379f5946a0af";
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/TND";

    // Cache for rates to avoid excessive API calls
    private static Map<String, Double> cachedRates = new HashMap<>();
    private static long lastFetchTime = 0;
    private static final long CACHE_DURATION = 3600000; // 1 hour in milliseconds

    // Fallback rates if API fails
    private static final Map<String, Double> FALLBACK_RATES = new HashMap<>();
    static {
        FALLBACK_RATES.put("EUR", 3.4);
        FALLBACK_RATES.put("USD", 3.1);
    }

    private static void updateRates() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFetchTime < CACHE_DURATION && !cachedRates.isEmpty()) {
            return;
        }

        try {
            URL url = new URL(BASE_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String json = response.toString();
                // Simple regex based parsing to avoid dependency issues
                Pattern pattern = Pattern.compile("\"([A-Z]{3})\":\\s*([0-9.]+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(json);

                Map<String, Double> newRates = new HashMap<>();
                while (matcher.find()) {
                    String currency = matcher.group(1).toUpperCase();
                    double rate = Double.parseDouble(matcher.group(2));
                    newRates.put(currency, rate);
                }
                
                if (!newRates.isEmpty()) {
                    cachedRates = newRates;
                    lastFetchTime = currentTime;
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching exchange rates: " + e.getMessage());
        }
    }

    private static double getRateToTnd(String currencyCode) {
        updateRates();
        String code = currencyCode.toUpperCase();
        
        // The API returns rates relative to TND (1 TND = X Foreign)
        // We want 1 Foreign = Y TND => Y = 1 / X
        if (cachedRates.containsKey(code)) {
            double rateFromTnd = cachedRates.get(code);
            if (rateFromTnd != 0) {
                return 1.0 / rateFromTnd;
            }
        }

        // Fallback logic
        if (code.equals("EUR")) return FALLBACK_RATES.get("EUR");
        if (code.equals("USD")) return FALLBACK_RATES.get("USD");
        
        return 1.0;
    }

    public static double parseAndConvert(String input, double defaultValue) {
        if (input == null || input.isBlank()) return defaultValue;
        
        String lowerInput = input.toLowerCase().trim();
        double rate = 1.0;

        if (lowerInput.contains("euro") || lowerInput.contains("eur") || lowerInput.contains("€")) {
            rate = getRateToTnd("EUR");
        } else if (lowerInput.contains("usd") || lowerInput.contains("$") || lowerInput.contains("dollar")) {
            rate = getRateToTnd("USD");
        }

        // Clean numeric part
        String sanitized = input.replaceAll("[^0-9.,-]", "")
                                .replace(',', '.');

        // Handle multiple dots
        if (sanitized.indexOf('.') != sanitized.lastIndexOf('.')) {
            int firstDot = sanitized.indexOf('.');
            sanitized = sanitized.substring(0, firstDot + 1) + sanitized.substring(firstDot + 1).replace(".", "");
        }

        try {
            double value = Double.parseDouble(sanitized);
            return value * rate;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
