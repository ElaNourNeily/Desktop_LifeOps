package Controllers;

import Service.PatternAnalysisService;
import Service.PatternAnalysisService.*;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private HBox  scoreContainer;
    @FXML private VBox  patternsContainer;
    @FXML private VBox  recommandationsContainer;
    @FXML private VBox  emptyContainer;
    @FXML private Button btnActualiser;

    private final PatternAnalysisService analysisService = new PatternAnalysisService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerAnalyse();
    }

    @FXML
    void handleActualiser(ActionEvent event) {
        // Animation de rotation sur le bouton
        scoreContainer.setOpacity(0);
        patternsContainer.setOpacity(0);
        recommandationsContainer.setOpacity(0);
        chargerAnalyse();
    }

    private void chargerAnalyse() {
        ScoreAnalyse score     = analysisService.calculerScore();
        List<Pattern> patterns = analysisService.analyserPatterns();
        List<Recommandation> recs = analysisService.genererRecommandations(patterns, score);

        afficherScore(score);
        afficherPatterns(patterns);
        afficherRecommandations(recs);

        boolean vide = patterns.isEmpty();
        emptyContainer.setVisible(vide);
        emptyContainer.setManaged(vide);
    }

    // ── Score global ──────────────────────────────────────────────────

    private void afficherScore(ScoreAnalyse score) {
        scoreContainer.getChildren().clear();
        scoreContainer.setSpacing(16);

        scoreContainer.getChildren().addAll(
            creerCarteScore("🏆", String.valueOf(score.scoreReussite) + "%",
                "Taux de réussite", couleurScore(score.scoreReussite)),
            creerCarteScore("📋", String.valueOf(score.totalObjectifs),
                "Total objectifs", "#8b5cf6"),
            creerCarteScore("✅", String.valueOf(score.completes),
                "Complétés", "#10b981"),
            creerCarteScore("🔄", String.valueOf(score.enCours),
                "En cours", "#3b82f6"),
            creerCarteScore("❌", String.valueOf(score.abandonnes),
                "Abandonnés", "#ef4444")
        );

        animer(scoreContainer, 0);
    }

    private VBox creerCarteScore(String emoji, String valeur, String label, String couleur) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setStyle(
            "-fx-background-color: #111318;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + couleur + ";" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 22px;");

        Label valeurLbl = new Label(valeur);
        valeurLbl.setStyle("-fx-text-fill: " + couleur + "; -fx-font-size: 28px; -fx-font-weight: bold;");

        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        card.getChildren().addAll(emojiLbl, valeurLbl, labelLbl);
        return card;
    }

    // ── Patterns ──────────────────────────────────────────────────────

    private void afficherPatterns(List<Pattern> patterns) {
        patternsContainer.getChildren().clear();

        if (patterns.isEmpty()) return;

        Label titre = new Label("🔍  Patterns détectés (" + patterns.size() + ")");
        titre.setStyle("-fx-text-fill: #f1f2f4; -fx-font-size: 16px; -fx-font-weight: bold;");
        VBox.setMargin(titre, new Insets(0, 0, 4, 0));
        patternsContainer.getChildren().add(titre);

        for (int i = 0; i < patterns.size(); i++) {
            VBox card = creerCartePattern(patterns.get(i));
            card.setOpacity(0);
            card.setTranslateY(15);
            patternsContainer.getChildren().add(card);
            animer(card, i * 80);
        }
    }

    private VBox creerCartePattern(Pattern pattern) {
        String couleur = switch (pattern.severite) {
            case "haute"   -> "#ef4444";
            case "moyenne" -> "#f59e0b";
            default        -> "#6b7280";
        };
        String bgColor     = switch (pattern.severite) {
            case "haute"   -> "rgba(239,68,68,0.06)";
            case "moyenne" -> "rgba(245,158,11,0.06)";
            default        -> "rgba(107,114,128,0.06)";
        };
        String borderColor = switch (pattern.severite) {
            case "haute"   -> "rgba(239,68,68,0.25)";
            case "moyenne" -> "rgba(245,158,11,0.25)";
            default        -> "rgba(107,114,128,0.2)";
        };

        VBox card = new VBox(10);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;"
        );

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label emojiLbl = new Label(pattern.emoji);
        emojiLbl.setStyle("-fx-font-size: 22px;");

        VBox textes = new VBox(3);
        Label titreLbl = new Label(pattern.titre);
        titreLbl.setStyle("-fx-text-fill: #f1f2f4; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label severiteLbl = new Label(pattern.severite.toUpperCase());
        severiteLbl.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-text-fill: " + couleur + ";" +
            "-fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-background-radius: 4; -fx-padding: 2 7;" +
            "-fx-border-color: " + borderColor + "; -fx-border-width: 1; -fx-border-radius: 4;"
        );

        textes.getChildren().addAll(titreLbl, severiteLbl);
        HBox.setHgrow(textes, Priority.ALWAYS);

        // Badge nb objectifs
        Label nbLbl = new Label(pattern.nbObjectifs + " objectif" + (pattern.nbObjectifs > 1 ? "s" : ""));
        nbLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        header.getChildren().addAll(emojiLbl, textes, nbLbl);

        // Description
        Label descLbl = new Label(pattern.description);
        descLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
        descLbl.setWrapText(true);

        card.getChildren().addAll(header, descLbl);
        return card;
    }

    // ── Recommandations ───────────────────────────────────────────────

    private void afficherRecommandations(List<Recommandation> recs) {
        recommandationsContainer.getChildren().clear();

        Label titre = new Label("💡  Recommandations");
        titre.setStyle("-fx-text-fill: #f1f2f4; -fx-font-size: 16px; -fx-font-weight: bold;");
        VBox.setMargin(titre, new Insets(8, 0, 4, 0));
        recommandationsContainer.getChildren().add(titre);

        for (int i = 0; i < recs.size(); i++) {
            VBox card = creerCarteRecommandation(recs.get(i));
            card.setOpacity(0);
            card.setTranslateY(15);
            recommandationsContainer.getChildren().add(card);
            animer(card, i * 80);
        }
    }

    private VBox creerCarteRecommandation(Recommandation rec) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(
            "-fx-background-color: rgba(139,92,246,0.05);" +
            "-fx-border-color: rgba(139,92,246,0.2);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;"
        );

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label emojiLbl = new Label(rec.emoji);
        emojiLbl.setStyle("-fx-font-size: 20px;");

        Label titreLbl = new Label(rec.titre);
        titreLbl.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 14px; -fx-font-weight: bold;");

        header.getChildren().addAll(emojiLbl, titreLbl);

        Label conseilLbl = new Label(rec.conseil);
        conseilLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
        conseilLbl.setWrapText(true);

        card.getChildren().addAll(header, conseilLbl);
        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private String couleurScore(int score) {
        if (score >= 70) return "#10b981";
        if (score >= 40) return "#f59e0b";
        return "#ef4444";
    }

    private void animer(javafx.scene.Node node, int delayMs) {
        FadeTransition ft = new FadeTransition(Duration.millis(400), node);
        ft.setToValue(1.0);
        ft.setDelay(Duration.millis(delayMs));

        TranslateTransition tt = new TranslateTransition(Duration.millis(400), node);
        tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs));

        new ParallelTransition(ft, tt).play();
    }
}
