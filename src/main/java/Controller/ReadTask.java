package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import model.Tache;
import service.TacheService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class ReadTask implements Initializable {

    // ── Kanban columns ────────────────────────────────────────────────
    @FXML private VBox colAFaire;
    @FXML private VBox colEnCours;
    @FXML private VBox colEnRevision;
    @FXML private VBox colTermine;

    // ── Count badges ──────────────────────────────────────────────────
    @FXML private Label badgeAFaire;
    @FXML private Label badgeEnCours;
    @FXML private Label badgeEnRevision;
    @FXML private Label badgeTermine;

    private final TacheService tacheService = new TacheService();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    // ─────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerTaches();
    }

    // ── Navigation: + Nouvelle Tâche → Creatask.fxml ─────────────────
    @FXML
    private void naviguerVersCreatask() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Creatask.fxml"));
            colAFaire.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation vers Creatask : " + e.getMessage());
        }
    }

    // ── Load all tasks from DB and sort into columns ──────────────────
    private void chargerTaches() {
        nettoyerColonne(colAFaire);
        nettoyerColonne(colEnCours);
        nettoyerColonne(colEnRevision);
        nettoyerColonne(colTermine);

        int cntAFaire = 0, cntEnCours = 0, cntEnRevision = 0, cntTermine = 0;

        try {
            List<Tache> taches = tacheService.recuperer();

            for (Tache t : taches) {
                VBox card = creerCarteTache(t);
                String statut = t.getStatut().getValeur().toLowerCase();

                if (statut.contains("faire") || statut.contains("todo")) {
                    colAFaire.getChildren().add(card);
                    cntAFaire++;
                } else if (statut.contains("cours") || statut.contains("progress")) {
                    colEnCours.getChildren().add(card);
                    cntEnCours++;
                } else if (statut.contains("vision") || statut.contains("review")) {
                    colEnRevision.getChildren().add(card);
                    cntEnRevision++;
                } else if (statut.contains("termin") || statut.contains("done")
                        || statut.contains("complet")) {
                    colTermine.getChildren().add(card);
                    cntTermine++;
                } else {
                    colAFaire.getChildren().add(card);
                    cntAFaire++;
                }
            }

        } catch (SQLException e) {
            System.err.println("Erreur chargement tâches : " + e.getMessage());
        }

        updateBadge(badgeAFaire,     cntAFaire);
        updateBadge(badgeEnCours,    cntEnCours);
        updateBadge(badgeEnRevision, cntEnRevision);
        updateBadge(badgeTermine,    cntTermine);
    }

    // ── Remove task cards but keep the column header ──────────────────
    private void nettoyerColonne(VBox colonne) {
        if (colonne == null) return;
        // Keep only the first child (HBox header); clear everything after
        if (colonne.getChildren().size() > 1) {
            colonne.getChildren().subList(1, colonne.getChildren().size()).clear();
        }
    }

    private void updateBadge(Label badge, int count) {
        if (badge != null) badge.setText(String.valueOf(count));
    }

    // ── Build one task card — clicking it navigates to UpdateTask ─────
    private VBox creerCarteTache(Tache t) {
        VBox card = new VBox(10);
        card.getStyleClass().add("task-card");
        card.setPadding(new Insets(15));

        // Header: Priorité badge + Difficulté dots
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label lblPriorite = new Label(t.getPriorite().getValeur());
        lblPriorite.getStyleClass().add(priorityStyleClass(t.getPriorite().getValeur()));

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        HBox diffBox = creerDifficultyDots(t.getDifficulte());
        headerBox.getChildren().addAll(lblPriorite, spacer1, diffBox);

        // Title
        Label lblTitre = new Label(t.getTitre());
        lblTitre.getStyleClass().add("task-title");
        lblTitre.setWrapText(true);

        // Description
        String descText = (t.getDescription() != null && !t.getDescription().isBlank())
                ? t.getDescription() : "Aucune description";
        Label lblDesc = new Label(descText);
        lblDesc.getStyleClass().add("task-description");
        lblDesc.setWrapText(true);

        // Footer: deadline
        Label lblDate = new Label(formatDeadline(t.getDeadline()));
        lblDate.getStyleClass().add("task-meta");

        card.getChildren().addAll(headerBox, lblTitre, lblDesc, lblDate);

        // ── Click anywhere on the card → navigate to UpdateTask.fxml ──
        card.setOnMouseClicked(e -> naviguerVersUpdateTask(t));

        return card;
    }

    // ── Navigate to UpdateTask.fxml passing the selected Tache ────────
    private void naviguerVersUpdateTask(Tache tache) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UpdateTask.fxml"));
            Parent root = loader.load();

            // Pass the selected task to the UpdateTask controller once it's ready:
            // UpdateTask controller = loader.getController();
            // controller.setTache(tache);

            colAFaire.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation vers UpdateTask : " + e.getMessage());
        }
    }

    // ── 5 difficulty dots ─────────────────────────────────────────────
    private HBox creerDifficultyDots(int difficulte) {
        HBox box = new HBox(4);
        box.getStyleClass().add("difficulty-dots");
        for (int i = 1; i <= 5; i++) {
            Circle dot = new Circle(4);
            dot.getStyleClass().add(i <= difficulte ? "difficulty-dot" : "difficulty-dot-empty");
            box.getChildren().add(dot);
        }
        return box;
    }

    // ── Priorité string → CSS class ───────────────────────────────────
    private String priorityStyleClass(String val) {
        if (val == null) return "priority-basse";
        return switch (val.toLowerCase()) {
            case "urgent"  -> "priority-urgent";
            case "haute"   -> "priority-haute";
            case "moyenne" -> "priority-moyenne";
            default        -> "priority-basse";
        };
    }

    // ── Format deadline + retard warning ─────────────────────────────
    private String formatDeadline(Date deadline) {
        if (deadline == null) return "📅 Aucune date";
        String s = "📅 " + SDF.format(deadline);
        if (deadline.before(new Date())) s += "  ⚠ Retard";
        return s;
    }

    // ── Public refresh (call after saving a task in Creatask) ─────────
    public void rafraichir() {
        chargerTaches();
    }
}