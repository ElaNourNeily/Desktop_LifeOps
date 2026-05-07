package service;

import model.Budget;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser {

    // Support 2 decimals (EUR/USD) and 3 decimals (ex: TND millimes).
    private static final Pattern MONEY_ANY =
            Pattern.compile("(?<!\\d)(\\d{1,6})([\\.,])(\\d{2,3})(?!\\d)");

    private static final Pattern DATE_DMY =
            Pattern.compile("(?<!\\d)(\\d{1,2})[\\-/\\.](\\d{1,2})[\\-/\\.](\\d{2,4})(?!\\d)");

    private static final Pattern DATE_YMD =
            Pattern.compile("(?<!\\d)(\\d{4})[\\-/\\.](\\d{1,2})[\\-/\\.](\\d{1,2})(?!\\d)");

    private static final Pattern DATE_DMY_NO_YEAR =
            Pattern.compile("(?<!\\d)(\\d{1,2})[\\-/\\.](\\d{1,2})(?!\\d)");

    private static final Pattern TOTAL_LINE =
            Pattern.compile("(?im)^(.*(?:TOTAL|T\\s*O\\s*T\\s*A\\s*L|TTC|A\\s*PAYER|NET\\s*A\\s*PAYER|AMOUNT\\s+DUE).*)$");

    private static final Map<String, String> KEYWORD_TO_CATEGORY = buildCategoryKeywords();

    public ReceiptData parse(String rawOcrText) {
        if (rawOcrText == null) {
            return new ReceiptData(null, null, null, null, null);
        }

        String text = rawOcrText.replace('\u00A0', ' ');
        String upper = normalizeForMatch(text).toUpperCase(Locale.ROOT);

        Double amount = extractAmount(text, upper).orElse(null);
        LocalDate date = extractDate(text).orElse(null);
        // If title extraction is low-confidence, return null so the UI won't overwrite user input.
        String title = extractTitle(text).orElse(null);
        String paymentType = inferPaymentType(upper);
        String category = inferCategory(upper);

        return new ReceiptData(amount, date, title, category, paymentType);
    }

    private Optional<Double> extractAmount(String text, String upper) {
        // Prefer amounts on "TOTAL" lines, otherwise pick the largest value found.
        List<Double> candidates = new ArrayList<>();

        Matcher tl = TOTAL_LINE.matcher(upper);
        while (tl.find()) {
            String line = tl.group(1);
            candidates.addAll(extractMoneyValues(line));
        }

        if (candidates.isEmpty()) {
            candidates.addAll(extractMoneyValues(text));
        }

        // Filter out obviously wrong tiny values (like 0.00) but keep small totals.
        return candidates.stream()
                .filter(v -> v != null && v > 0.01)
                .max(Double::compareTo);
    }

    private List<Double> extractMoneyValues(String s) {
        List<Double> vals = new ArrayList<>();
        Matcher m = MONEY_ANY.matcher(s);
        while (m.find()) {
            String normalized = m.group(1) + "." + m.group(3);
            try {
                vals.add(Double.parseDouble(normalized));
            } catch (NumberFormatException ignored) {
            }
        }
        return vals;
    }

    private Optional<LocalDate> extractDate(String text) {
        // Try YMD first.
        Matcher ymd = DATE_YMD.matcher(text);
        if (ymd.find()) {
            int y = parseIntSafe(ymd.group(1));
            int mo = parseIntSafe(ymd.group(2));
            int d = parseIntSafe(ymd.group(3));
            return safeDate(y, mo, d);
        }

        // Then DMY.
        Matcher dmy = DATE_DMY.matcher(text);
        if (dmy.find()) {
            int d = parseIntSafe(dmy.group(1));
            int mo = parseIntSafe(dmy.group(2));
            int y = parseIntSafe(dmy.group(3));
            if (y < 100) {
                // Assume current century.
                int century = (Year.now().getValue() / 100) * 100;
                y = century + y;
            }
            return safeDate(y, mo, d);
        }

        // Then DMY without year (common in some receipts): assume current year.
        Matcher dmyNoYear = DATE_DMY_NO_YEAR.matcher(text);
        while (dmyNoYear.find()) {
            int d = parseIntSafe(dmyNoYear.group(1));
            int mo = parseIntSafe(dmyNoYear.group(2));
            int y = Year.now().getValue();
            Optional<LocalDate> dt = safeDate(y, mo, d);
            if (dt.isPresent()) {
                return dt;
            }
        }

        // Month-name formats (FR/EN) are less reliable in OCR; still attempt.
        String cleaned = text.replaceAll("[,]", " ").replaceAll("\\s+", " ").trim();
        List<DateTimeFormatter> fmts = List.of(
                new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendValue(ChronoField.DAY_OF_MONTH)
                        .appendLiteral(' ')
                        .appendPattern("MMM")
                        .appendLiteral(' ')
                        .appendValue(ChronoField.YEAR, 4)
                        .toFormatter(Locale.ENGLISH)
                        .withResolverStyle(ResolverStyle.SMART),
                new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendValue(ChronoField.DAY_OF_MONTH)
                        .appendLiteral(' ')
                        .appendPattern("MMMM")
                        .appendLiteral(' ')
                        .appendValue(ChronoField.YEAR, 4)
                        .toFormatter(Locale.FRENCH)
                        .withResolverStyle(ResolverStyle.SMART)
        );

        for (DateTimeFormatter f : fmts) {
            for (String token : slidingDateTokens(cleaned, 3)) {
                try {
                    LocalDate dt = LocalDate.parse(token, f);
                    return Optional.of(dt);
                } catch (Exception ignored) {
                }
            }
        }

        return Optional.empty();
    }

    private List<String> slidingDateTokens(String cleaned, int words) {
        String[] parts = cleaned.split(" ");
        List<String> out = new ArrayList<>();
        if (parts.length < words) {
            return out;
        }
        for (int i = 0; i + words <= parts.length; i++) {
            out.add(String.join(" ", parts[i], parts[i + 1], parts[i + 2]));
        }
        return out;
    }

    private Optional<String> extractTitle(String rawText) {
        // Receipts often have the merchant/store name at the very top.
        // OCR can output garbage for that line, so we score candidates and reject low-signal lines.
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }

        String[] lines = rawText.replace('\r', '\n').split("\n");
        int maxLines = Math.min(lines.length, 12);

        String best = null;
        double bestScore = 0.0;

        for (int i = 0; i < maxLines; i++) {
            String line = lines[i] == null ? "" : lines[i].trim();
            if (line.isBlank()) continue;

            String u = normalizeForMatch(line).toUpperCase(Locale.ROOT);

            // Skip obvious non-title lines.
            if (u.contains("TOTAL") || u.contains("TTC") || u.contains("A PAYER") || u.contains("NET A PAYER")
                    || u.contains("FACTURE") || u.contains("INVOICE") || u.contains("TVA")
                    || u.contains("DATE") || u.contains("HEURE") || u.contains("TEL") || u.contains("TELEPHONE")
                    || u.contains("MERCI") || u.contains("CARTE") || u.contains("CB") || u.contains("VISA") || u.contains("MASTERCARD")) {
                continue;
            }

            // Normalize whitespace / remove very noisy punctuation for token checks.
            String cleaned = line.replaceAll("[^A-Za-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
            if (cleaned.length() < 4) continue;
            if (cleaned.length() > 48) continue; // usually addresses / long headers

            String[] parts = cleaned.split(" ");
            int tokens = 0;
            int shortTokens = 0;
            int longest = 0;
            int digits = 0;
            int letters = 0;
            for (char c : cleaned.toCharArray()) {
                if (Character.isDigit(c)) digits++;
                else if (Character.isLetter(c)) letters++;
            }
            for (String p : parts) {
                if (p.isBlank()) continue;
                tokens++;
                int len = p.length();
                if (len <= 2) shortTokens++;
                if (len > longest) longest = len;
            }
            if (tokens == 0) continue;
            if (longest < 4) continue; // avoid lines like "EN AC EA SS"
            if ((double) shortTokens / (double) tokens > 0.5) continue;

            double alphaRatio = (letters + digits) == 0 ? 0.0 : (double) letters / (double) (letters + digits);
            if (alphaRatio < 0.7) continue;

            // Score: prefer high alpha ratio and earlier lines; penalize many digits.
            double score = alphaRatio * 10.0 + Math.min(longest, 12) - digits * 0.5 - i * 0.25;
            if (score > bestScore) {
                bestScore = score;
                best = cleaned;
            }
        }

        if (best == null || bestScore < 10.0) {
            return Optional.empty();
        }
        return Optional.of(toTitleCase(best));
    }

    private String inferPaymentType(String upperNormalized) {
        String u = upperNormalized;
        if (u.contains("VISA") || u.contains("MASTERCARD") || u.contains("CARTE") || u.contains("CB") || u.contains("CARD")) {
            return "Card";
        }
        if (u.contains("CASH") || u.contains("ESPECES")) {
            return "Cash";
        }
        return "Card";
    }

    private String inferCategory(String upperNormalized) {
        for (Map.Entry<String, String> e : KEYWORD_TO_CATEGORY.entrySet()) {
            if (upperNormalized.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return "Autre";
    }

    public static Budget pickBudgetForDate(List<Budget> budgets, LocalDate date) {
        if (budgets == null || budgets.isEmpty() || date == null) {
            return null;
        }

        int month = date.getMonthValue();
        String frMonth = frenchMonthName(month);
        String enMonth = date.getMonth().name().toLowerCase(Locale.ROOT);
        String month2 = String.format(Locale.ROOT, "%02d", month);
        String month1 = String.valueOf(month);

        return budgets.stream()
                .filter(b -> b != null && b.getMois() != null)
                .map(b -> Map.entry(b, normalizeForMatch(b.getMois())))
                .filter(e -> {
                    String m = e.getValue();
                    return m.contains(frMonth)
                            || m.contains(enMonth)
                            || m.matches(".*\\b" + Pattern.quote(month2) + "\\b.*")
                            || m.matches(".*\\b" + Pattern.quote(month1) + "\\b.*");
                })
                .map(Map.Entry::getKey)
                .min(Comparator.comparingInt(Budget::getId))
                .orElse(null);
    }

    private static String frenchMonthName(int month) {
        return switch (month) {
            case 1 -> "janvier";
            case 2 -> "fevrier";
            case 3 -> "mars";
            case 4 -> "avril";
            case 5 -> "mai";
            case 6 -> "juin";
            case 7 -> "juillet";
            case 8 -> "aout";
            case 9 -> "septembre";
            case 10 -> "octobre";
            case 11 -> "novembre";
            case 12 -> "decembre";
            default -> "";
        };
    }

    private static String normalizeForMatch(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", ""); // strip accents
        return n.replace('\u00A0', ' ')
                .toLowerCase(Locale.ROOT);
    }

    private static Optional<LocalDate> safeDate(int y, int m, int d) {
        try {
            return Optional.of(LocalDate.of(y, m, d));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }

    private static String toTitleCase(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.isBlank()) {
            return s;
        }
        String[] parts = lower.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            String p = parts[i];
            if (p.isBlank()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
        }
        return sb.toString().trim();
    }

    private static Map<String, String> buildCategoryKeywords() {
        // Keys must be already uppercase-friendly (we normalize to uppercase before contains()).
        Map<String, String> m = new HashMap<>();

        // Alimentation
        m.put("CARREFOUR", "Alimentation");
        m.put("MONOPRIX", "Alimentation");
        m.put("SUPERMARCHE", "Alimentation");
        m.put("EPICERIE", "Alimentation");
        m.put("RESTAURANT", "Alimentation");
        m.put("FOOD", "Alimentation");
        m.put("CAFE", "Alimentation");

        // Transport
        m.put("UBER", "Transport");
        m.put("BOLT", "Transport");
        m.put("TAXI", "Transport");
        m.put("STATION", "Transport");
        m.put("ESSENCE", "Transport");
        m.put("FUEL", "Transport");
        m.put("TRAIN", "Transport");
        m.put("BUS", "Transport");
        m.put("METRO", "Transport");

        // Sante
        m.put("PHARMACIE", "Sante");
        m.put("PHARMACY", "Sante");
        m.put("CLINIQUE", "Sante");
        m.put("HOPITAL", "Sante");

        // Shopping
        m.put("ZARA", "Shopping");
        m.put("H&M", "Shopping");
        m.put("DECATHLON", "Shopping");
        m.put("AMAZON", "Shopping");

        // Abonnements
        m.put("NETFLIX", "Abonnements");
        m.put("SPOTIFY", "Abonnements");
        m.put("SUBSCRIPTION", "Abonnements");

        // Bills / Rent
        m.put("ELECTRICITE", "Bills");
        m.put("WATER", "Bills");
        m.put("EAU", "Bills");
        m.put("INTERNET", "Bills");
        m.put("LOYER", "Rent");
        m.put("RENT", "Rent");

        // Education / Loisirs
        m.put("COURS", "Education");
        m.put("SCHOOL", "Education");
        m.put("CINEMA", "Loisirs");
        m.put("MUSEE", "Loisirs");

        return m;
    }
}
