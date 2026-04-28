package Service;

import Utilis.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PatternAnalysisService {

    private final Connection connection;

    public PatternAnalysisService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // ── Modèle de résultat ────────────────────────────────────────────

    public static class Pattern {
        public final String type;        // "abandon_categorie", "abandon_rapide", "stagnation", etc.
        public final String emoji;
        public final String titre;
        public final String description;
        public final String severite;    // "haute", "moyenne", "faible"
        public final int nbObjectifs;

        public Pattern(String type, String emoji, String titre,
                       String description, String severite, int nbObjectifs) {
            this.type        = type;
            this.emoji       = emoji;
            this.titre       = titre;
            this.description = description;
            this.severite    = severite;
            this.nbObjectifs = nbObjectifs;
        }
    }

    public static class Recommandation {
        public final String emoji;
        public final String titre;
        public final String conseil;

        public Recommandation(String emoji, String titre, String conseil) {
            this.emoji  = emoji;
            this.titre  = titre;
            this.conseil = conseil;
        }
    }

    public static class ScoreAnalyse {
        public final int scoreReussite;   // 0-100
        public final int totalObjectifs;
        public final int abandonnes;
        public final int completes;
        public final int enCours;

        public ScoreAnalyse(int score, int total, int abandonnes, int completes, int enCours) {
            this.scoreReussite  = score;
            this.totalObjectifs = total;
            this.abandonnes     = abandonnes;
            this.completes      = completes;
            this.enCours        = enCours;
        }
    }

    // ── Score global ──────────────────────────────────────────────────

    public ScoreAnalyse calculerScore() {
        String sql = """
            SELECT
                COUNT(*) as total,
                SUM(CASE WHEN LOWER(statut) IN ('abandonné','abandonne') THEN 1 ELSE 0 END) as abandonnes,
                SUM(CASE WHEN LOWER(statut) IN ('complété','complete') THEN 1 ELSE 0 END) as completes,
                SUM(CASE WHEN LOWER(statut) = 'en cours' THEN 1 ELSE 0 END) as en_cours
            FROM objectif
            """;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int total     = rs.getInt("total");
                int abandonnes = rs.getInt("abandonnes");
                int completes  = rs.getInt("completes");
                int enCours    = rs.getInt("en_cours");
                int score = total == 0 ? 100 :
                    (int) Math.round((double)(completes) / total * 100);
                return new ScoreAnalyse(score, total, abandonnes, completes, enCours);
            }
        } catch (SQLException e) {
            System.err.println("Erreur calculerScore : " + e.getMessage());
        }
        return new ScoreAnalyse(0, 0, 0, 0, 0);
    }

    // ── Analyse des patterns ──────────────────────────────────────────

    public List<Pattern> analyserPatterns() {
        List<Pattern> patterns = new ArrayList<>();

        patterns.addAll(detecterAbandonParCategorie());
        patterns.addAll(detecterAbandonRapide());
        patterns.addAll(detecterStagnation());
        patterns.addAll(detecterSurchargePlans());

        return patterns;
    }

    /**
     * Pattern 1 : Abandon fréquent dans une catégorie spécifique
     * Ex: "Vous abandonnez souvent les objectifs de catégorie Santé"
     */
    private List<Pattern> detecterAbandonParCategorie() {
        List<Pattern> patterns = new ArrayList<>();
        String sql = """
            SELECT
                categorie,
                COUNT(*) as total,
                SUM(CASE WHEN LOWER(statut) IN ('abandonné','abandonne') THEN 1 ELSE 0 END) as abandonnes
            FROM objectif
            GROUP BY categorie
            HAVING COUNT(*) >= 2
              AND SUM(CASE WHEN LOWER(statut) IN ('abandonné','abandonne') THEN 1 ELSE 0 END) >= 1
            ORDER BY SUM(CASE WHEN LOWER(statut) IN ('abandonné','abandonne') THEN 1 ELSE 0 END) / COUNT(*) DESC
            """;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String cat      = rs.getString("categorie");
                int total       = rs.getInt("total");
                int abandonnes  = rs.getInt("abandonnes");
                double taux     = (double) abandonnes / total * 100;

                if (taux >= 40) {
                    String severite = taux >= 70 ? "haute" : "moyenne";
                    String emoji    = getEmojiCategorie(cat);
                    patterns.add(new Pattern(
                        "abandon_categorie",
                        emoji,
                        "Abandon fréquent en " + cat,
                        String.format(
                            "Vous abandonnez souvent les objectifs de catégorie \"%s\" — %d sur %d abandonnés (%.0f%%). " +
                            "Essayez de définir des objectifs plus petits et atteignables dans cette catégorie.",
                            cat, abandonnes, total, taux
                        ),
                        severite,
                        abandonnes
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur detecterAbandonParCategorie : " + e.getMessage());
        }
        return patterns;
    }

    /**
     * Pattern 2 : Abandon rapide (objectifs abandonnés en moins de 14 jours)
     */
    private List<Pattern> detecterAbandonRapide() {
        List<Pattern> patterns = new ArrayList<>();
        String sql = """
            SELECT COUNT(*) as nb,
                   AVG(DATEDIFF(COALESCE(date_fin, CURDATE()), date_debut)) as duree_moy
            FROM objectif
            WHERE LOWER(statut) IN ('abandonné','abandonne')
              AND date_debut IS NOT NULL
              AND DATEDIFF(COALESCE(date_fin, CURDATE()), date_debut) <= 14
            """;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int nb         = rs.getInt("nb");
                double dureMoy = rs.getDouble("duree_moy");
                if (nb >= 1) {
                    patterns.add(new Pattern(
                        "abandon_rapide",
                        "⚡",
                        "Abandon rapide détecté",
                        String.format(
                            "%d objectif%s abandonné%s en moins de 2 semaines (durée moyenne : %.0f jours). " +
                            "Cela peut indiquer des objectifs trop ambitieux ou un manque de motivation initiale.",
                            nb, nb > 1 ? "s" : "", nb > 1 ? "s" : "", dureMoy
                        ),
                        nb >= 3 ? "haute" : "moyenne",
                        nb
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur detecterAbandonRapide : " + e.getMessage());
        }
        return patterns;
    }

    /**
     * Pattern 3 : Stagnation (objectifs en cours avec progression < 20% depuis longtemps)
     */
    private List<Pattern> detecterStagnation() {
        List<Pattern> patterns = new ArrayList<>();
        String sql = """
            SELECT COUNT(*) as nb
            FROM objectif
            WHERE LOWER(statut) = 'en cours'
              AND progression < 20
              AND date_debut IS NOT NULL
              AND DATEDIFF(CURDATE(), date_debut) > 30
            """;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int nb = rs.getInt("nb");
                if (nb >= 1) {
                    patterns.add(new Pattern(
                        "stagnation",
                        "🐌",
                        "Stagnation détectée",
                        String.format(
                            "%d objectif%s en cours depuis plus de 30 jours avec moins de 20%% de progression. " +
                            "Essayez de décomposer ces objectifs en plans d'action plus petits et actionnables.",
                            nb, nb > 1 ? "s" : ""
                        ),
                        nb >= 3 ? "haute" : "moyenne",
                        nb
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur detecterStagnation : " + e.getMessage());
        }
        return patterns;
    }

    /**
     * Pattern 4 : Surcharge de plans d'action (objectifs avec trop de plans non terminés)
     */
    private List<Pattern> detecterSurchargePlans() {
        List<Pattern> patterns = new ArrayList<>();
        String sql = """
            SELECT o.titre, COUNT(p.id) as nb_plans
            FROM objectif o
            JOIN plan_action p ON p.objectif_id = o.id
            WHERE LOWER(p.statut) NOT IN ('terminé','termine')
              AND LOWER(o.statut) = 'en cours'
            GROUP BY o.id, o.titre
            HAVING nb_plans >= 6
            ORDER BY nb_plans DESC
            LIMIT 3
            """;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<String> objectifsSurcharges = new ArrayList<>();
            while (rs.next()) {
                objectifsSurcharges.add(rs.getString("titre") + " (" + rs.getInt("nb_plans") + " plans)");
            }
            if (!objectifsSurcharges.isEmpty()) {
                patterns.add(new Pattern(
                    "surcharge",
                    "📚",
                    "Surcharge de plans d'action",
                    "Certains objectifs ont trop de plans non terminés : " +
                    String.join(", ", objectifsSurcharges) +
                    ". Une surcharge peut décourager. Priorisez 3 plans maximum à la fois.",
                    "moyenne",
                    objectifsSurcharges.size()
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur detecterSurchargePlans : " + e.getMessage());
        }
        return patterns;
    }

    // ── Recommandations ───────────────────────────────────────────────

    public List<Recommandation> genererRecommandations(List<Pattern> patterns, ScoreAnalyse score) {
        List<Recommandation> recs = new ArrayList<>();

        boolean aAbandonCategorie = patterns.stream().anyMatch(p -> p.type.equals("abandon_categorie"));
        boolean aAbandonRapide    = patterns.stream().anyMatch(p -> p.type.equals("abandon_rapide"));
        boolean aStagnation       = patterns.stream().anyMatch(p -> p.type.equals("stagnation"));
        boolean aSurcharge        = patterns.stream().anyMatch(p -> p.type.equals("surcharge"));

        if (aAbandonRapide) {
            recs.add(new Recommandation("🎯",
                "Définissez des objectifs SMART",
                "Vos objectifs doivent être Spécifiques, Mesurables, Atteignables, Réalistes et Temporels. " +
                "Évitez les objectifs vagues comme \"être en forme\" — préférez \"courir 3km, 3 fois par semaine\"."
            ));
        }

        if (aAbandonCategorie) {
            recs.add(new Recommandation("🔬",
                "Analysez vos blocages par catégorie",
                "Identifiez pourquoi vous abandonnez dans certaines catégories. " +
                "Manque de temps ? Motivation insuffisante ? Objectif trop difficile ? " +
                "Commencez par un objectif très simple dans cette catégorie pour reconstruire la confiance."
            ));
        }

        if (aStagnation) {
            recs.add(new Recommandation("⚡",
                "Décomposez vos objectifs bloqués",
                "Pour les objectifs qui stagnent, créez des plans d'action très petits et actionnables. " +
                "Une action de 5 minutes par jour vaut mieux qu'un grand plan jamais exécuté."
            ));
        }

        if (aSurcharge) {
            recs.add(new Recommandation("🎪",
                "Limitez-vous à 3 plans actifs à la fois",
                "La règle du WIP (Work In Progress) : ne travaillez pas sur plus de 3 plans simultanément. " +
                "Terminez-en un avant d'en commencer un nouveau."
            ));
        }

        if (score.scoreReussite >= 70) {
            recs.add(new Recommandation("🏆",
                "Excellent taux de réussite !",
                "Vous complétez " + score.scoreReussite + "% de vos objectifs. " +
                "Continuez à appliquer vos méthodes actuelles et challengez-vous avec des objectifs plus ambitieux."
            ));
        }

        if (recs.isEmpty()) {
            recs.add(new Recommandation("✨",
                "Continuez sur cette lancée",
                "Aucun pattern négatif détecté. Maintenez votre rythme et pensez à célébrer vos succès !"
            ));
        }

        return recs;
    }

    // ── Helper ────────────────────────────────────────────────────────

    private String getEmojiCategorie(String cat) {
        if (cat == null) return "🎯";
        return switch (cat.toLowerCase()) {
            case "santé", "sante" -> "💪";
            case "finances"       -> "💰";
            case "etudes"         -> "📚";
            case "loisirs"        -> "🎮";
            case "personnel"      -> "🌟";
            default               -> "🎯";
        };
    }
}
