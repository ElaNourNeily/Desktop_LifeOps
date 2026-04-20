package Controller.task;

import enums.StatutTache;
import enums.StatutTaskSpace;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.task.Tache;
import model.task.TaskSpace;
import service.task.TacheService;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ReadTaskSpace implements Initializable {

    @FXML private AnchorPane root;
    @FXML private Label lblBreadcrumb;
    @FXML private Label lblProjectTitle;
    @FXML private Label lblRoleBadge;
    @FXML private Label lblStatusBadge;

    @FXML private Label lblType;
    @FXML private Label lblDuration;
    @FXML private Label lblDateCreation;

    @FXML private Label cntAFaire;
    @FXML private Label cntEnCours;
    @FXML private Label cntEnRevision;
    @FXML private Label cntTermine;

    @FXML private Label badgeAFaire;
    @FXML private Label badgeEnCours;
    @FXML private Label badgeEnRevision;
    @FXML private Label badgeTermine;

    @FXML private VBox cardsAFaire;
    @FXML private VBox cardsEnCours;
    @FXML private VBox cardsEnRevision;
    @FXML private VBox cardsTermine;

    @FXML private Label emptyAFaire;
    @FXML private Label emptyEnCours;
    @FXML private Label emptyEnRevision;
    @FXML private Label emptyTermine;

    private TaskSpace taskSpace;
    private int currentUserId;
    private boolean isLeader;

    private final TacheService tacheService = new TacheService();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    private static final String UI_AFAIRE      = "À faire";
    private static final String UI_EN_COURS    = "En cours";
    private static final String UI_EN_REVISION = "En révision";
    private static final String UI_TERMINE     = "Terminé";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setTaskSpace(TaskSpace ts, int currentUserId, boolean isLeader) {
        this.taskSpace     = ts;
        this.currentUserId = currentUserId;
        this.isLeader      = isLeader;

        lblBreadcrumb.setText(ts.getNom());
        lblProjectTitle.setText(ts.getNom());

        if (lblRoleBadge != null) {
            lblRoleBadge.setText(isLeader ? "Leader" : "Membre");
        }

        lblType.setText(ts.getType() != null ? ts.getType().getValeur() : "Projet");
        lblDuration.setText("⏱ " + ts.getDuration() + " jours");
        if (ts.getDateCreation() != null) {
            lblDateCreation.setText("📅 Créé le " + SDF.format(ts.getDateCreation()));
        }

        applyStatusBadge(ts.getStatus());
        chargerTaches();
    }

    private void chargerTaches() {
        try {
            List<Tache> all;
            if (isLeader) {
                all = tacheService.recupererParTaskSpace(taskSpace.getId());
            } else {
                all = tacheService.recupererParTaskSpaceEtUtilisateur(taskSpace.getId(), currentUserId);
            }

            List<Tache> aFaire     = all.stream().filter(t -> t.getStatut().getValeur().equalsIgnoreCase("todo")).toList();
            List<Tache> enCours    = all.stream().filter(t -> t.getStatut().getValeur().equalsIgnoreCase("in-progress")).toList();
            List<Tache> enRevision = all.stream().filter(t -> t.getStatut().getValeur().equalsIgnoreCase("review")).toList();
            List<Tache> termine    = all.stream().filter(t -> t.getStatut().getValeur().equalsIgnoreCase("done")).toList();

            renderColumn(cardsAFaire,     emptyAFaire,     badgeAFaire,     cntAFaire,     aFaire);
            renderColumn(cardsEnCours,    emptyEnCours,    badgeEnCours,    cntEnCours,    enCours);
            renderColumn(cardsEnRevision, emptyEnRevision, badgeEnRevision, cntEnRevision, enRevision);
            renderColumn(cardsTermine,    emptyTermine,    badgeTermine,    cntTermine,    termine);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur chargement", e.getMessage());
        }
    }

    private void renderColumn(VBox container, Label emptyLabel, Label badge, Label counterLabel, List<Tache> tasks) {
        container.getChildren().removeIf(n -> n != emptyLabel);

        String count = String.valueOf(tasks.size());
        badge.setText(count);
        counterLabel.setText(count);

        if (tasks.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
        } else {
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
            for (Tache t : tasks) {
                container.getChildren().add(buildTaskCard(t));
            }
        }
    }

    private VBox buildTaskCard(Tache t) {
        VBox card = new VBox(9);
        card.getStyleClass().add("project-card");
        card.setPadding(new Insets(14, 14, 12, 14));
        card.setMaxWidth(Double.MAX_VALUE);

        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);

        String priorite = t.getPriorite() != null ? t.getPriorite().getValeur() : "low";
        Label lblPriority = new Label(priorite);
        lblPriority.getStyleClass().add("type-chip");
        lblPriority.setStyle(priorityStyle(priorite)
                + "-fx-background-radius:6;-fx-font-size:10px;-fx-padding:2 8 2 8;");

        Region rowSpacer = new Region();
        HBox.setHgrow(rowSpacer, Priority.ALWAYS);

        Label lblDiff = new Label(difficultyStars(t.getDifficulte()));
        lblDiff.setStyle("-fx-text-fill:#6b6b75;-fx-font-size:11px;");

        row1.getChildren().addAll(lblPriority, rowSpacer, lblDiff);

        Label lblTitre = new Label(t.getTitre());
        lblTitre.getStyleClass().add("project-card-name");
        lblTitre.setWrapText(true);
        lblTitre.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:white;");

        HBox row3 = new HBox(8);
        row3.setAlignment(Pos.CENTER_LEFT);

        String deadlineStr = (t.getDeadline() != null) ? "📅 " + SDF.format(t.getDeadline()) : "";
        Label lblDeadline = new Label(deadlineStr);
        lblDeadline.getStyleClass().add("project-card-meta");
        lblDeadline.setStyle("-fx-font-size:11px;");
        HBox.setHgrow(lblDeadline, Priority.ALWAYS);

        Button btnEdit = new Button("✏");
        btnEdit.setStyle(
                "-fx-background-color:#1e1e26;" +
                        "-fx-background-radius:9;" +
                        "-fx-border-color:#39393B;" +
                        "-fx-border-radius:9;" +
                        "-fx-text-fill:#a1a1aa;" +
                        "-fx-font-size:13px;" +
                        "-fx-cursor:hand;" +
                        "-fx-min-width:34;-fx-min-height:30;");
        btnEdit.setOnAction(e -> naviguerVersUpdateTache(t));

        Button btnDelete = new Button("🗑");
        btnDelete.setStyle(
                "-fx-background-color:#1e1e26;" +
                        "-fx-background-radius:9;" +
                        "-fx-border-color:#39393B;" +
                        "-fx-border-radius:9;" +
                        "-fx-text-fill:#fca5a5;" +
                        "-fx-font-size:13px;" +
                        "-fx-cursor:hand;" +
                        "-fx-min-width:34;-fx-min-height:30;");
        btnDelete.setOnAction(e -> supprimerTache(t));

        row3.getChildren().addAll(lblDeadline, btnEdit, btnDelete);

        ChoiceBox<String> statusBox = new ChoiceBox<>();
        statusBox.getItems().addAll(UI_AFAIRE, UI_EN_COURS, UI_EN_REVISION, UI_TERMINE);

        String dbStatut = t.getStatut().getValeur().toLowerCase();
        if (dbStatut.equals("todo"))        statusBox.setValue(UI_AFAIRE);
        else if (dbStatut.equals("in-progress")) statusBox.setValue(UI_EN_COURS);
        else if (dbStatut.equals("review")) statusBox.setValue(UI_EN_REVISION);
        else                                statusBox.setValue(UI_TERMINE);

        statusBox.getStyleClass().add("form-choice");
        statusBox.setMaxWidth(Double.MAX_VALUE);
        statusBox.setOnAction(ev -> changerStatut(t, statusBox.getValue()));

        card.getChildren().addAll(row1, lblTitre, row3, statusBox);
        return card;
    }

    private void changerStatut(Tache t, String newValueUI) {
        try {
            String newDbValue = "todo";
            if (newValueUI.equals(UI_EN_COURS))    newDbValue = "in-progress";
            else if (newValueUI.equals(UI_EN_REVISION)) newDbValue = "review";
            else if (newValueUI.equals(UI_TERMINE)) newDbValue = "done";

            StatutTache newStatut = StatutTache.fromString(newDbValue);
            t.setStatut(newStatut);
            tacheService.modifier(t);
            chargerTaches();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void supprimerTache(Tache t) {
        if (!isLeader) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Seul le Leader du projet peut supprimer des tâches.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer la tâche « " + t.getTitre() + " » ?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                tacheService.supprimer(t.getId());
                chargerTaches();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur suppression", e.getMessage());
            }
        }
    }

    private void naviguerVersUpdateTache(Tache t) {
        if (!isLeader) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Seul le Leader du projet peut modifier la configuration de la tâche.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/task/UpdateTache.fxml"));
            Parent updateRoot = loader.load();
            root.getScene().setRoot(updateRoot);
        } catch (Exception e) {
            showAlert(Alert.AlertType.INFORMATION, "Information", "Édition de la tâche : " + t.getTitre());
        }
    }

    @FXML
    private void ouvrirCreerTache(ActionEvent event) {
        if (!isLeader) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Seul le Leader du projet peut créer de nouvelles tâches.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/task/CreerTache.fxml"));
            Parent createRoot = loader.load();
            root.getScene().setRoot(createRoot);
        } catch (Exception e) {
            showAlert(Alert.AlertType.INFORMATION, "Information", "Création d'une nouvelle tâche pour le projet : " + taskSpace.getNom());
        }
    }

    @FXML
    private void retourVersTaskSpace(ActionEvent event) {
        try {
            Parent tsRoot = FXMLLoader.load(getClass().getResource("/task/TaskSpace.fxml"));
            root.getScene().setRoot(tsRoot);
        } catch (IOException e) {
            System.err.println("Erreur retour TaskSpace : " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur navigation", e.getMessage());
        }
    }

    @FXML
    private void naviguerVersModifier(ActionEvent event) {
        if (!isLeader) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Seul le Leader peut modifier le projet.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/task/UpdateTaskSpace.fxml"));
            Parent updateRoot = loader.load();

            // TRANSFERT DES DONNÉES ICI !
            UpdateTaskSpace updateCtrl = loader.getController();
            updateCtrl.setTaskSpace(this.taskSpace);

            root.getScene().setRoot(updateRoot);
        } catch (Exception e) {
            System.err.println("Erreur navigation Modifier : " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur navigation Modifier", e.getMessage());
        }
    }

    @FXML
    private void autoAssignationIA(ActionEvent event) {
        if (!isLeader) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Seul le Leader peut utiliser l'auto-assignation IA.");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "✨ Auto-Assignation IA",
                "L'IA analyse la charge de travail de chaque membre et distribue les tâches automatiquement.");
    }

    private void applyStatusBadge(StatutTaskSpace s) {
        if (s == null) { lblStatusBadge.setText(""); return; }
        String v = s.getValeur();
        lblStatusBadge.setText(v);
        String style = switch (v.toLowerCase()) {
            case "actif", "active" -> "-fx-background-color:#14532d;-fx-text-fill:#4ade80;";
            case "en pause"        -> "-fx-background-color:#422006;-fx-text-fill:#fb923c;";
            default                -> "-fx-background-color:#2a2a30;-fx-text-fill:#a1a1aa;";
        };
        lblStatusBadge.setStyle(style + "-fx-background-radius:6;-fx-font-size:11px;-fx-padding:3 8 3 8;");
    }

    private String priorityStyle(String priorite) {
        if (priorite == null) return "-fx-background-color:#1E1333;-fx-text-fill:#c084fc;";
        return switch (priorite.toLowerCase()) {
            case "urgent" -> "-fx-background-color:#7f1d1d;-fx-text-fill:#fca5a5;";
            case "high"   -> "-fx-background-color:#431407;-fx-text-fill:#fb923c;";
            case "medium" -> "-fx-background-color:#422006;-fx-text-fill:#fbbf24;";
            default       -> "-fx-background-color:#1E1333;-fx-text-fill:#c084fc;";
        };
    }

    private String difficultyStars(int diff) {
        int d = Math.max(1, Math.min(5, diff));
        return "★".repeat(d) + "☆".repeat(5 - d);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}