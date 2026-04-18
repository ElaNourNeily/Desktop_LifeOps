package Controllers;

import Model.Objectif;
import Model.PlanAction;
import Service.ObjectifService;
import Service.PlanActionService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ObjectifController implements Initializable {

    @FXML private VBox objectifsContainer;
    @FXML private Button btnNouvelObjectif;
    @FXML private Button btnTous;
    @FXML private Button btnPersonnel;
    @FXML private Button btnSante;
    @FXML private Button btnEtudes;
    @FXML private Button btnFinances;
    @FXML private Button btnLoisirs;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> comboTri;
    @FXML private Label labelCount;

    private ObjectifService objectifService = new ObjectifService();
    private PlanActionService planActionService = new PlanActionService();
    private String categorieActive = "Tous";
    private String triActif = "Date de création (récent)";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialiser le ComboBox de tri
        comboTri.setItems(FXCollections.observableArrayList(
            "Date de création (récent)",
            "Date de création (ancien)",
            "Titre (A-Z)",
            "Titre (Z-A)",
            "Progression (croissant)",
            "Progression (décroissant)",
            "Date de fin (proche)",
            "Date de fin (éloignée)"
        ));
        comboTri.setValue(triActif);
        comboTri.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                triActif = newVal;
                chargerObjectifs();
            }
        });

        // Filtrage en temps réel lors de la saisie dans la barre de recherche
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
            afficherObjectifsFiltres(categorieActive, newVal);
        });
        chargerObjectifs();
    }

    private void chargerObjectifs() {
        afficherObjectifsFiltres(categorieActive, txtRecherche.getText());
    }

    // Méthode centrale : filtre par catégorie ET par texte de recherche via Streams
    private void afficherObjectifsFiltres(String categorie, String recherche) {
        objectifsContainer.getChildren().clear();

        List<Objectif> resultats = objectifService.readAll().stream()
                .filter(o -> categorie.equals("Tous") ||
                             o.getCategorie() != null &&
                             o.getCategorie().equalsIgnoreCase(categorie))
                .filter(o -> recherche == null || recherche.isEmpty() ||
                             (o.getTitre() != null && o.getTitre().toLowerCase().contains(recherche.toLowerCase())) ||
                             (o.getDescription() != null && o.getDescription().toLowerCase().contains(recherche.toLowerCase())))
                .sorted(getComparateurTri())
                .collect(Collectors.toList());

        labelCount.setText("EN COURS (" + resultats.size() + ")");
        resultats.forEach(obj -> objectifsContainer.getChildren().add(creerCarte(obj)));
    }

    // Retourne le Comparator correspondant au tri sélectionné
    private Comparator<Objectif> getComparateurTri() {
        return switch (triActif) {
            case "Titre (A-Z)"               -> Comparator.comparing(o -> o.getTitre() != null ? o.getTitre().toLowerCase() : "");
            case "Titre (Z-A)"               -> Comparator.comparing((Objectif o) -> o.getTitre() != null ? o.getTitre().toLowerCase() : "").reversed();
            case "Progression (croissant)"   -> Comparator.comparingInt(Objectif::getProgression);
            case "Progression (décroissant)" -> Comparator.comparingInt(Objectif::getProgression).reversed();
            case "Date de fin (proche)"      -> Comparator.comparing(o -> o.getDate_fin() != null ? o.getDate_fin() : java.time.LocalDate.MAX);
            case "Date de fin (éloignée)"    -> Comparator.comparing((Objectif o) -> o.getDate_fin() != null ? o.getDate_fin() : java.time.LocalDate.MIN).reversed();
            case "Date de création (ancien)" -> Comparator.comparingInt(Objectif::getId);
            default                          -> Comparator.comparingInt(Objectif::getId).reversed(); // récent = ID décroissant
        };
    }

    // Gère les clics sur les boutons de filtres
    @FXML
    void handleFiltrer(ActionEvent event) {
        Button btnClique = (Button) event.getSource();
        categorieActive = btnClique.getText().equals("Tous") ? "Tous" : btnClique.getText();

        // Mise à jour du style des boutons (actif / inactif)
        List<Button> boutonsFiltres = List.of(btnTous, btnPersonnel, btnSante, btnEtudes, btnFinances, btnLoisirs);
        boutonsFiltres.forEach(btn -> {
            btn.getStyleClass().removeAll("filter-btn-active", "filter-btn");
            btn.getStyleClass().add(btn == btnClique ? "filter-btn-active" : "filter-btn");
        });

        afficherObjectifsFiltres(categorieActive, txtRecherche.getText());
    }

    @FXML
    void handleNouvelObjectif(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/AjouterObjectif.fxml"));
            javafx.scene.Parent root = loader.load();

            AjouterObjectifController controller = loader.getController();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Nouvel Objectif");
            stage.setScene(new javafx.scene.Scene(root));
            stage.setResizable(false);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            Objectif nouvelObj = controller.getNouvelObjectif();
            if (nouvelObj != null) {
                // L'objectif est déjà sauvegardé en base par AjouterObjectifController
                chargerObjectifs(); // Rafraîchir la liste
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Construction visuelle d'une carte Objectif avec panneau plan d'action expandable
    private VBox creerCarte(Objectif obj) {
        VBox card = new VBox(0);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(0));

        // ── ZONE PRINCIPALE (toujours visible) ──────────────────────────────
        VBox mainZone = new VBox(10);
        mainZone.setPadding(new Insets(15, 25, 15, 25));
        mainZone.setStyle("-fx-cursor: hand;");

        // Header : catégorie | titre | barre de progression | %
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        String couleur = getCouleurCategorie(obj.getCategorie());
        Label catLabel = new Label(obj.getCategorie() != null ? obj.getCategorie() : "Autre");
        catLabel.setStyle("-fx-text-fill: " + couleur + "; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label titleLabel = new Label(obj.getTitre());
        titleLabel.getStyleClass().add("card-title");

        Region spacerH = new Region();
        HBox.setHgrow(spacerH, Priority.ALWAYS);

        ProgressBar progress = new ProgressBar((double) obj.getProgression() / 100);
        progress.setPrefWidth(200);
        progress.setStyle("-fx-accent: " + couleur + ";");

        Label progressLabel = new Label(obj.getProgression() + "%");
        progressLabel.setStyle("-fx-text-fill: " + couleur + "; -fx-font-weight: bold;");

        // Flèche toggle
        Label fleche = new Label("▶");
        fleche.setStyle("-fx-text-fill: #5a5d61; -fx-font-size: 13px;");

        header.getChildren().addAll(catLabel, titleLabel, spacerH, progress, progressLabel, fleche);

        // Description
        Label descLabel = new Label(obj.getDescription() != null ? obj.getDescription() : "");
        descLabel.setStyle("-fx-text-fill: #8a8d91;");
        descLabel.setWrapText(true);

        // Dates + statut
        HBox infoBox = new HBox(40);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        if (obj.getDate_debut() != null)
            infoBox.getChildren().add(creerInfoVBox("Début", obj.getDate_debut().toString(), "white"));
        if (obj.getDate_fin() != null)
            infoBox.getChildren().add(creerInfoVBox("Fin", obj.getDate_fin().toString(), "white"));
        if (obj.getStatut() != null)
            infoBox.getChildren().add(creerInfoVBox("Statut", obj.getStatut(), "#ffb703"));

        // Boutons Modifier / Supprimer
        Region spacerActions = new Region();
        HBox.setHgrow(spacerActions, Priority.ALWAYS);

        Button btnModifier = new Button("✏ Modifier");
        btnModifier.setStyle("-fx-background-color: #1c1e22; -fx-text-fill: #a78bfa; -fx-border-color: #8b5cf6; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        btnModifier.setOnAction(e -> { e.consume(); handleModifier(obj); });

        Button btnSupprimer = new Button("🗑 Supprimer");
        btnSupprimer.setStyle("-fx-background-color: #1c1e22; -fx-text-fill: #ff477e; -fx-border-color: #ff477e; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSupprimer.setOnAction(e -> { e.consume(); handleSupprimer(obj); });

        HBox actionsBox = new HBox(10, spacerActions, btnModifier, btnSupprimer);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        mainZone.getChildren().addAll(header, descLabel, infoBox, actionsBox);

        // ── PANNEAU PLAN D'ACTION (caché par défaut) ─────────────────────────
        VBox planPanel = new VBox(10);
        planPanel.setVisible(false);
        planPanel.setManaged(false);
        planPanel.setPadding(new Insets(10, 25, 15, 25));
        planPanel.setStyle("-fx-background-color: #16181b; -fx-border-color: #2a2d32; -fx-border-width: 1 0 0 0;");

        // Toggle : clic sur la zone principale ou sur la flèche
        Runnable togglePanel = () -> {
            boolean ouvert = planPanel.isVisible();
            planPanel.setVisible(!ouvert);
            planPanel.setManaged(!ouvert);
            fleche.setText(ouvert ? "▶" : "▼");
            if (!ouvert) {
                // Recharger les plans à chaque ouverture
                planPanel.getChildren().clear();
                construirePanelPlans(obj, planPanel);
            }
        };

        mainZone.setOnMouseClicked(e -> togglePanel.run());

        card.getChildren().addAll(mainZone, planPanel);
        return card;
    }

    // Construit le contenu du panneau plan d'action
    private void construirePanelPlans(Objectif obj, VBox planPanel) {
        List<PlanAction> plans = planActionService.readByObjectif(obj.getId());

        if (plans.isEmpty()) {
            // Aucun plan : message + bouton de création
            Label vide = new Label("Aucun plan d'action pour cet objectif.");
            vide.setStyle("-fx-text-fill: #5a5d61; -fx-font-style: italic;");

            Button btnCreer = new Button("➕ Créer un plan d'action");
            btnCreer.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;");
            btnCreer.setOnAction(e -> { e.consume(); handlePlanAction(obj, planPanel); });

            planPanel.getChildren().addAll(vide, btnCreer);
        } else {
            // Afficher les plans existants sous forme de lignes compactes
            Label titreSection = new Label("📋  Plans d'action (" + plans.size() + ")");
            titreSection.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold; -fx-font-size: 13px;");

            planPanel.getChildren().add(titreSection);

            for (PlanAction plan : plans) {
                planPanel.getChildren().add(creerLignePlan(plan, obj, planPanel));
            }

            // Bouton pour ajouter un plan supplémentaire
            Button btnAjouter = new Button("➕ Ajouter un plan");
            btnAjouter.setStyle("-fx-background-color: transparent; -fx-text-fill: #a78bfa; -fx-border-color: #8b5cf6; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 5 14; -fx-cursor: hand;");
            btnAjouter.setOnAction(e -> { e.consume(); handlePlanAction(obj, planPanel); });
            planPanel.getChildren().add(btnAjouter);
        }
    }

    // Ligne compacte pour un plan d'action dans le panneau
    private HBox creerLignePlan(PlanAction plan, Objectif obj, VBox planPanel) {
        HBox ligne = new HBox(10);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(8, 10, 8, 10));
        ligne.setStyle("-fx-background-color: #1c1e22; -fx-background-radius: 8;");

        // Indicateur priorité
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + getCouleurPriorite(plan.getPriorite()) + "; -fx-font-size: 10px;");

        Label titrePlan = new Label(plan.getTitre());
        titrePlan.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label statutPlan = new Label(plan.getStatut() != null ? plan.getStatut() : "—");
        statutPlan.setStyle("-fx-background-color: " + getCouleurStatutPlan(plan.getStatut()) +
                "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 2 8; -fx-font-size: 11px;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnEdit = new Button("✏");
        btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #a78bfa; -fx-cursor: hand; -fx-font-size: 13px;");
        btnEdit.setOnAction(e -> { e.consume(); handlePlanAction(obj, planPanel); });

        Button btnDel = new Button("🗑");
        btnDel.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff477e; -fx-cursor: hand; -fx-font-size: 13px;");
        btnDel.setOnAction(e -> {
            e.consume();
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
            conf.setTitle("Supprimer le plan");
            conf.setHeaderText(null);
            conf.setContentText("Supprimer \"" + plan.getTitre() + "\" ?");
            conf.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    planActionService.delete(plan.getId());
                    planPanel.getChildren().clear();
                    construirePanelPlans(obj, planPanel);
                }
            });
        });

        ligne.getChildren().addAll(dot, titrePlan, sp, statutPlan, btnEdit, btnDel);
        return ligne;
    }

    private void handlePlanAction(Objectif obj, VBox planPanel) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/PlanAction.fxml"));
            javafx.scene.Parent root = loader.load();

            PlanActionController controller = loader.getController();
            controller.setObjectif(obj);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Plans d'action — " + obj.getTitre());
            stage.setScene(new javafx.scene.Scene(root));
            stage.setResizable(true);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Rafraîchir le panneau inline après fermeture
            planPanel.getChildren().clear();
            construirePanelPlans(obj, planPanel);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void handleModifier(Objectif obj) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ModifierObjectif.fxml"));
            javafx.scene.Parent root = loader.load();

            ModifierObjectifController controller = loader.getController();
            controller.setObjectif(obj);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Modifier l'Objectif");
            stage.setScene(new javafx.scene.Scene(root));
            stage.setResizable(false);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            chargerObjectifs(); // Rafraîchir après modification
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSupprimer(Objectif obj) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Supprimer l'objectif");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Voulez-vous vraiment supprimer l'objectif \"" + obj.getTitre() + "\" ?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                objectifService.delete(obj.getId());
                chargerObjectifs(); // Rafraîchir après suppression
            }
        });
    }

    private VBox creerInfoVBox(String label, String valeur, String couleurValeur) {
        VBox vbox = new VBox(3);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("section-title");
        Label val = new Label(valeur);
        val.setStyle("-fx-text-fill: " + couleurValeur + "; -fx-font-weight: bold;");
        vbox.getChildren().addAll(lbl, val);
        return vbox;
    }

    private String getCouleurPriorite(String priorite) {
        if (priorite == null) return "#5a5d61";
        return switch (priorite.toLowerCase()) {
            case "haute"   -> "#ff477e";
            case "moyenne" -> "#ffb703";
            case "basse"   -> "#00d285";
            default        -> "#5a5d61";
        };
    }

    private String getCouleurStatutPlan(String statut) {
        if (statut == null) return "#5a5d61";
        return switch (statut.toLowerCase()) {
            case "terminé", "termine" -> "#00d285";
            case "en cours"           -> "#6ea8fe";
            case "annulé", "annule"   -> "#ff477e";
            default                   -> "#5a5d61";
        };
    }

    // Couleur par catégorie
    private String getCouleurCategorie(String categorie) {
        if (categorie == null) return "#8a8d91";
        return switch (categorie.toLowerCase()) {
            case "santé", "sante" -> "#ff477e";
            case "finances"       -> "#00d285";
            case "etudes"         -> "#6ea8fe";
            case "loisirs"        -> "#ffb703";
            case "personnel"      -> "#c77dff";
            default               -> "#8a8d91";
        };
    }
}
