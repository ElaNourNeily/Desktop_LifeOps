package service;

import model.Depense;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ExpenseInsightsService {

    public enum Priority {
        HIGH,
        MEDIUM,
        LOW
    }

    public static final class PriorityResult {
        private final Priority priority;
        private final String reason;

        public PriorityResult(Priority priority, String reason) {
            this.priority = priority;
            this.reason = reason;
        }

        public Priority getPriority() {
            return priority;
        }

        public String getReason() {
            return reason;
        }
    }

    public static final class RecurringCandidate {
        private final String label;
        private final double amount;
        private final String cadence; // e.g. "monthly"
        private final LocalDate nextExpectedDate;
        private final int occurrences;
        private final int confidence; // 0-100

        public RecurringCandidate(String label, double amount, String cadence, LocalDate nextExpectedDate, int occurrences, int confidence) {
            this.label = label;
            this.amount = amount;
            this.cadence = cadence;
            this.nextExpectedDate = nextExpectedDate;
            this.occurrences = occurrences;
            this.confidence = confidence;
        }

        public String getLabel() {
            return label;
        }

        public double getAmount() {
            return amount;
        }

        public String getCadence() {
            return cadence;
        }

        public LocalDate getNextExpectedDate() {
            return nextExpectedDate;
        }

        public int getOccurrences() {
            return occurrences;
        }

        public int getConfidence() {
            return confidence;
        }
    }

    private final ZoneId zone;

    public ExpenseInsightsService() {
        this.zone = ZoneId.systemDefault();
    }

    public PriorityResult prioritize(Depense depense) {
        String titre = safeLower(depense.getTitre());
        String cat = safeLower(depense.getCategorie());

        // Hard rules by category
        if (containsAny(cat, "rent", "loyer", "bills", "facture", "sante", "health", "education", "transport")) {
            return new PriorityResult(Priority.HIGH, "Categorie essentielle");
        }
        if (containsAny(cat, "abonnement", "subscription", "loisirs", "shopping")) {
            return new PriorityResult(Priority.LOW, "Categorie discretionary");
        }

        // Keyword rules by title
        if (containsAny(titre, "loyer", "rent", "steg", "sonede", "electric", "water", "assurance", "insurance", "credit", "loan")) {
            return new PriorityResult(Priority.HIGH, "Paiement important");
        }
        if (containsAny(titre, "netflix", "spotify", "deezer", "prime", "disney", "youtube", "xbox", "playstation", "steam")) {
            return new PriorityResult(Priority.LOW, "Abonnement / loisir probable");
        }

        // Amount-based nudge: large amounts tend to be higher attention
        if (depense.getMontant() >= 500) {
            return new PriorityResult(Priority.HIGH, "Montant eleve");
        }
        if (depense.getMontant() <= 30) {
            return new PriorityResult(Priority.LOW, "Petit montant");
        }
        return new PriorityResult(Priority.MEDIUM, "Par defaut");
    }

    public List<RecurringCandidate> detectRecurring(List<Depense> depenses) {
        if (depenses == null || depenses.isEmpty()) {
            return List.of();
        }

        // Group by "merchant-ish" key derived from title + (optional) category.
        Map<String, List<Depense>> groups = new HashMap<>();
        for (Depense d : depenses) {
            if (d == null || d.getDate() == null) continue;
            String key = recurringKey(d);
            if (key.isBlank()) continue;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }

        List<RecurringCandidate> out = new ArrayList<>();
        for (Map.Entry<String, List<Depense>> e : groups.entrySet()) {
            List<Depense> list = e.getValue();
            if (list.size() < 3) continue; // need evidence

            list.sort(Comparator.comparing(d -> toLocalDate(d)));

            // Try detect monthly cadence with stable amount.
            RecurringCandidate monthly = detectCadence(list, e.getKey(), "monthly", 28, 35, 1);
            if (monthly != null) {
                out.add(monthly);
                continue;
            }

            // Try weekly cadence
            RecurringCandidate weekly = detectCadence(list, e.getKey(), "weekly", 6, 9, 0);
            if (weekly != null) {
                out.add(weekly);
            }
        }

        out.sort(Comparator.comparingInt(RecurringCandidate::getConfidence).reversed());
        return out;
    }

    private RecurringCandidate detectCadence(List<Depense> list, String key, String cadence, int minDays, int maxDays, int indexBump) {
        // Use last 3 occurrences as signal
        Depense a = list.get(list.size() - 3);
        Depense b = list.get(list.size() - 2);
        Depense c = list.get(list.size() - 1);

        LocalDate da = toLocalDate(a);
        LocalDate db = toLocalDate(b);
        LocalDate dc = toLocalDate(c);

        long ab = ChronoUnit.DAYS.between(da, db);
        long bc = ChronoUnit.DAYS.between(db, dc);

        if (!inRange(ab, minDays, maxDays) || !inRange(bc, minDays, maxDays)) {
            return null;
        }

        double amount = c.getMontant();
        if (!amountStable(a.getMontant(), b.getMontant(), c.getMontant())) {
            return null;
        }

        int confidence = 70;
        if (looksLikeSubscription(key)) confidence += 15;
        if (list.size() >= 4) confidence += 5;
        if (Math.abs(ab - bc) <= 2) confidence += 10;
        confidence = Math.min(95, confidence);

        LocalDate next = cadence.equals("weekly") ? dc.plusDays(7) : dc.plusMonths(1);
        String label = humanizeKey(key);
        return new RecurringCandidate(label, amount, cadence, next, list.size(), confidence);
    }

    private boolean amountStable(double x, double y, double z) {
        double avg = (x + y + z) / 3.0;
        if (avg <= 0) return false;
        // within 2% or 1 TND
        double tol = Math.max(1.0, avg * 0.02);
        return Math.abs(x - avg) <= tol && Math.abs(y - avg) <= tol && Math.abs(z - avg) <= tol;
    }

    private boolean looksLikeSubscription(String key) {
        String s = safeLower(key);
        return containsAny(s, "spotify", "netflix", "deezer", "prime", "disney", "youtube", "abonnement", "subscription", "gym", "internet", "box");
    }

    private String recurringKey(Depense d) {
        String titre = normalizeText(d.getTitre());
        if (titre.isBlank()) return "";
        // Keep only the first chunk (merchant-ish) and remove digits.
        titre = titre.replaceAll("\\d+", "").trim();
        // reduce noise words
        titre = titre.replaceAll("\\b(paiement|payment|facture|bill|achat|purchase)\\b", "").trim();
        if (titre.length() > 32) {
            titre = titre.substring(0, 32).trim();
        }

        String cat = normalizeText(d.getCategorie());
        if (!cat.isBlank()) {
            return (titre + "|" + cat).trim();
        }
        return titre;
    }

    private String humanizeKey(String key) {
        String[] parts = key.split("\\|", 2);
        String t = parts.length > 0 ? parts[0] : key;
        t = t.trim();
        if (t.isEmpty()) return "Depense recurrente";
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private LocalDate toLocalDate(Depense d) {
        Instant inst = Instant.ofEpochMilli(d.getDate().getTime());
        return inst.atZone(zone).toLocalDate();
    }

    private static boolean inRange(long v, int min, int max) {
        return v >= min && v <= max;
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String hay, String... needles) {
        if (hay == null || hay.isBlank()) return false;
        for (String n : needles) {
            if (n != null && !n.isBlank() && hay.contains(n)) return true;
        }
        return false;
    }

    private static String normalizeText(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        t = t.toLowerCase(Locale.ROOT).trim();
        t = t.replaceAll("[^a-z0-9\\s\\-_/]", " ");
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }
}

