package Controller.task;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import model.task.Tache;
import service.task.TacheService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ReadTask implements Initializable {

    @FXML private VBox colAFaire;
    @FXML private VBox colEnCours;
    @FXML private VBox colEnRevision;
    @FXML private VBox colTermine;

    @FXML private Label badgeAFaire;
    @FXML private Label badgeEnCours;
    @FXML private Label badgeEnRevision;
    @FXML private Label badgeTermine;

    private final TacheService tacheService = new TacheService();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerTaches();
    }

    // ── + Nouvelle Tâche → Creatask.fxml ─────────────────────────────
    @FXML
    private void naviguerVersCreatask() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/task/Creatask.fxml"));
            colAFaire.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation Creatask : " + e.getMessage());
        }
    }

    // ── Load & distribute tasks into columns ──────────────────────────
    private void chargerTaches() {
        nettoyerColonne(colAFaire);
        nettoyerColonne(colEnCours);
        nettoyerColonne(colEnRevision);
        nettoyerColonne(colTermine);

        int cA = 0, cE = 0, cR = 0, cT = 0;

        try {
            List<Tache> taches = tacheService.recuperer();
            for (Tache t : taches) {
                VBox card = creerCarteTache(t);
                String s = t.getStatut().getValeur().toLowerCase();

                if (s.contains("faire") || s.contains("todo")) {
                    colAFaire.getChildren().add(card); cA++;
                } else if (s.contains("cours") || s.contains("progress")) {
                    colEnCours.getChildren().add(card); cE++;
                } else if (s.contains("vision") || s.contains("review")) {
                    colEnRevision.getChildren().add(card); cR++;
                } else if (s.contains("termin") || s.contains("done") || s.contains("complet")) {
                    colTermine.getChildren().add(card); cT++;
                } else {
                    colAFaire.getChildren().add(card); cA++;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement tâches : " + e.getMessage());
        }

        updateBadge(badgeAFaire, cA);
        updateBadge(badgeEnCours, cE);
        updateBadge(badgeEnRevision, cR);
        updateBadge(badgeTermine, cT);
    }

    private void nettoyerColonne(VBox col) {
        if (col == null) return;
        if (col.getChildren().size() > 1)
            col.getChildren().subList(1, col.getChildren().size()).clear();
    }

    private void updateBadge(Label b, int count) {
        if (b != null) b.setText(String.valueOf(count));
    }

    // ── Build one task card with ✏️ edit + 🗑 delete buttons ──────────
    private VBox creerCarteTache(Tache t) {
        VBox card = new VBox(10);
        card.getStyleClass().add("task-card");
        card.setPadding(new Insets(15));

        // Row 1: Priorité badge + Difficulté dots
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label lblPriorite = new Label(t.getPriorite().getValeur());
        lblPriorite.getStyleClass().add(priorityStyleClass(t.getPriorite().getValeur()));
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        headerBox.getChildren().addAll(lblPriorite, sp1, creerDifficultyDots(t.getDifficulte()));

        // Row 2: Title
        Label lblTitre = new Label(t.getTitre());
        lblTitre.getStyleClass().add("task-title");
        lblTitre.setWrapText(true);

        // Row 3: Description
        String descText = (t.getDescription() != null && !t.getDescription().isBlank())
                ? t.getDescription() : "Aucune description";
        Label lblDesc = new Label(descText);
        lblDesc.getStyleClass().add("task-description");
        lblDesc.setWrapText(true);

        // Row 4: Deadline label + Edit icon button + Delete icon button
        HBox footerBox = new HBox(8);
        footerBox.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(footerBox, new Insets(4, 0, 0, 0));

        Label lblDate = new Label(formatDeadline(t.getDeadline()));
        lblDate.getStyleClass().add("task-meta");

        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        // ✏ Edit — purple rounded icon button (like screenshot 3)
        Button btnEdit = new Button("✏");
        btnEdit.getStyleClass().add("card-icon-button-edit");
        btnEdit.setOnAction(e -> naviguerVersUpdateTask(t));

        // 🗑 Delete — dark rounded icon button (like screenshot 3)
        Button btnDelete = new Button("🗑");
        btnDelete.getStyleClass().add("card-icon-button-delete");
        btnDelete.setOnAction(e -> supprimerTache(t));

        footerBox.getChildren().addAll(lblDate, sp2, btnEdit, btnDelete);

        card.getChildren().addAll(headerBox, lblTitre, lblDesc, footerBox);
        return card;
    }

    // ── Navigate to UpdateTask.fxml passing the Tache ─────────────────
    private void naviguerVersUpdateTask(Tache tache) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/task/UpdateTask.fxml"));
            Parent root = loader.load();

            UpdateTask controller = loader.getController();
            controller.setTache(tache);

            colAFaire.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation UpdateTask : " + e.getMessage());
        }
    }

    // ── Delete directly from card with confirmation ───────────────────
    private void supprimerTache(Tache t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer la tâche « " + t.getTitre() + " » ?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                tacheService.supprimer(t.getId());
                chargerTaches();
            } catch (SQLException e) {
                System.err.println("Erreur suppression : " + e.getMessage());
            }
        }
    }

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

    private String priorityStyleClass(String val) {
        if (val == null) return "priority-basse";
        return switch (val.toLowerCase()) {
            case "urgent"  -> "priority-urgent";
            case "haute"   -> "priority-haute";
            case "moyenne" -> "priority-moyenne";
            default        -> "priority-basse";
        };
    }

    private String formatDeadline(Date deadline) {
        if (deadline == null) return "📅 Aucune date";
        String s = "📅 " + SDF.format(deadline);
        if (deadline.before(new Date())) s += "  ⚠ Retard";
        return s;
    }

    public void rafraichir() {
        chargerTaches();
    }
}
