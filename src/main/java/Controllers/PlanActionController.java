package Controllers;

import Model.Objectif;
import Model.PlanAction;
import Service.GeminiService;
import Service.PlanActionService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PlanActionController {

    @FXML private Label labelTitreObjectif;
    @FXML private Label labelFormTitre;
    @FXML private VBox plansContainer;

    @FXML private TextField txtTitre;
    @FXML private ComboBox<String> comboPriorite;
    @FXML private ComboBox<String> comboStatut;
    @FXML private TextArea txtDescription;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private Button btnAnnuler;
    @FXML private Button btnSauvegarder;
    @FXML private Button btnSuggestions;

    @FXML private Label errTitre;
    @FXML private Label errDateDebut;
    @FXML private Label errDateFin;

    private static final String STYLE_ERREUR = "-fx-border-color: #ff477e; -fx-border-width: 1.5; -fx-border-radius: 6;";
    private static final String STYLE_VALIDE = "-fx-border-color: #00d285; -fx-border-width: 1.5; -fx-border-radius: 6;";

    private final PlanActionService planActionService = new PlanActionService();
    private final GeminiService geminiService = new GeminiService();
    private Objectif objectifCourant;
    private PlanAction planEnEdition = null;

    @FXML
    public void initialize() {
        comboPriorite.setItems(FXCollections.observableArrayList("Haute", "Moyenne", "Basse"));
        comboPriorite.setValue("Moyenne");

        comboStatut.setItems(FXCollections.observableArrayList("À faire", "En cours", "Terminé", "Annulé"));
        comboStatut.setValue("À faire");

        dateDebut.setValue(LocalDate.now());
        dateFin.setValue(LocalDate.now().plusWeeks(1));

        txtTitre.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                setErreur(txtTitre, errTitre, "Le titre est obligatoire.");
            } else if (newVal.trim().length() < 3) {
                setErreur(txtTitre, errTitre, "Minimum 3 caractères.");
            } else if (newVal.trim().length() > 100) {
                setErreur(txtTitre, errTitre, "Maximum 100 caractères.");
            } else {
                setValide(txtTitre, errTitre);
            }
        });

        dateDebut.valueProperty().addListener((obs, oldVal, newVal) -> validerDates());
        dateFin.valueProperty().addListener((obs, oldVal, newVal) -> validerDates());
    }

    public void setObjectif(Objectif objectif) {
        this.objectifCourant = objectif;
        labelTitreObjectif.setText("Objectif : " + objectif.getTitre());
        chargerPlans();
    }

    // ── Suggestions IA ───────────────────────────────────────────────────

    @FXML
    void handleSuggestionsIA(ActionEvent event) {
        // Désactiver le bouton pendant le chargement
        btnSuggestions.setDisable(true);
        btnSuggestions.setText("⏳ Génération en cours...");

        // Appel en arrière-plan pour ne pas bloquer l'UI
        Thread thread = new Thread(() -> {
            try {
                List<PlanAction> suggestions = geminiService.suggererPlans(
                    objectifCourant.getTitre(),
                    objectifCourant.getCategorie()
                );

                // Retour sur le thread JavaFX
                Platform.runLater(() -> {
                    btnSuggestions.setDisable(false);
                    btnSuggestions.setText("✨ Suggestions IA");
                    if (suggestions.isEmpty()) {
                        afficherErreurIA("Aucune suggestion reçue. Vérifiez votre clé API.");
                    } else {
                        afficherDialogSuggestions(suggestions);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnSuggestions.setDisable(false);
                    btnSuggestions.setText("✨ Suggestions IA");
                    afficherErreurIA("Erreur : " + e.getMessage());
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Affiche une dialog avec les suggestions Gemini.
     * L'utilisateur peut cocher celles qu'il veut ajouter directement.
     */
    private void afficherDialogSuggestions(List<PlanAction> suggestions) {
        Dialog<List<PlanAction>> dialog = new Dialog<>();
        dialog.setTitle("✨ Suggestions IA — Gemini");
        dialog.setHeaderText("Sélectionnez les plans d'action à ajouter :");
        dialog.getDialogPane().setPrefWidth(560);
        dialog.getDialogPane().setStyle("-fx-background-color: #1c1e22;");

        // Boutons
        ButtonType btnAjouter = new ButtonType("➕ Ajouter la sélection", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnFermer  = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAjouter, btnFermer);

        // Contenu : liste de CheckBox pour chaque suggestion
        VBox contenu = new VBox(12);
        contenu.setPadding(new Insets(15));
        contenu.setStyle("-fx-background-color: #1c1e22;");

        List<CheckBox> checkBoxes = new ArrayList<>();

        for (PlanAction plan : suggestions) {
            CheckBox cb = new CheckBox();
            cb.setSelected(true); // coché par défaut
            cb.setUserData(plan);

            VBox infoBox = new VBox(4);

            HBox titreBox = new HBox(8);
            titreBox.setAlignment(Pos.CENTER_LEFT);

            Label titreLbl = new Label(plan.getTitre());
            titreLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

            Label prioriteLbl = new Label(plan.getPriorite());
            prioriteLbl.setStyle(
                "-fx-background-color: " + getCouleurPriorite(plan.getPriorite()) +
                "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 1 7; -fx-font-size: 11px; -fx-font-weight: bold;"
            );

            titreBox.getChildren().addAll(titreLbl, prioriteLbl);

            Label descLbl = new Label(plan.getDescription());
            descLbl.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 12px;");
            descLbl.setWrapText(true);

            infoBox.getChildren().addAll(titreBox, descLbl);

            HBox ligne = new HBox(12);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.setPadding(new Insets(10));
            ligne.setStyle(
                "-fx-background-color: #2a2d32; -fx-background-radius: 8; -fx-cursor: hand;"
            );
            ligne.getChildren().addAll(cb, infoBox);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            // Clic sur toute la ligne pour cocher/décocher
            ligne.setOnMouseClicked(e -> cb.setSelected(!cb.isSelected()));

            checkBoxes.add(cb);
            contenu.getChildren().add(ligne);
        }

        ScrollPane scroll = new ScrollPane(contenu);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1c1e22; -fx-background-color: transparent;");
        scroll.setPrefHeight(380);

        dialog.getDialogPane().setContent(scroll);

        // Résultat : retourner les plans cochés
        dialog.setResultConverter(buttonType -> {
            if (buttonType == btnAjouter) {
                List<PlanAction> selectionnes = new ArrayList<>();
                for (CheckBox cb : checkBoxes) {
                    if (cb.isSelected()) {
                        selectionnes.add((PlanAction) cb.getUserData());
                    }
                }
                return selectionnes;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(plansSelectionnes -> {
            if (!plansSelectionnes.isEmpty()) {
                ajouterPlansSelectionnes(plansSelectionnes);
            }
        });
    }

    /**
     * Sauvegarde en base les plans sélectionnés dans la dialog.
     */
    private void ajouterPlansSelectionnes(List<PlanAction> plans) {
        int ajoutés = 0;
        for (PlanAction plan : plans) {
            plan.setObjectif_id(objectifCourant.getId());
            plan.setDate_debut(LocalDate.now());
            plan.setDate_fin(LocalDate.now().plusWeeks(2));
            plan.setStatut("À faire");
            try {
                planActionService.create(plan);
                ajoutés++;
            } catch (SQLException e) {
                System.err.println("Erreur ajout plan suggéré : " + e.getMessage());
            }
        }

        chargerPlans(); // Rafraîchir la liste

        // Confirmation
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Plans ajoutés");
        info.setHeaderText(null);
        info.setContentText(ajoutés + " plan(s) d'action ajouté(s) avec succès !");
        info.showAndWait();
    }

    private void afficherErreurIA(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur Gemini IA");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── Chargement & affichage des plans ─────────────────────────────────

    private void chargerPlans() {
        plansContainer.getChildren().clear();

        List<PlanAction> plans;
        try {
            plans = planActionService.readByObjectif(objectifCourant.getId());
        } catch (SQLException e) {
            System.err.println("Erreur chargement plans : " + e.getMessage());
            plans = new ArrayList<>();
        }

        if (plans.isEmpty()) {
            Label vide = new Label("Aucun plan d'action pour cet objectif.");
            vide.setStyle("-fx-text-fill: #5a5d61; -fx-font-style: italic;");
            plansContainer.getChildren().add(vide);
        } else {
            plans.forEach(p -> plansContainer.getChildren().add(creerCartePlan(p)));
        }
    }

    private VBox creerCartePlan(PlanAction plan) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #1c1e22; -fx-background-radius: 10; -fx-border-color: #2a2d32; -fx-border-radius: 10; -fx-border-width: 1;");
        card.setPadding(new Insets(12, 16, 12, 16));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titreLabel = new Label(plan.getTitre());
        titreLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label prioriteLabel = new Label(plan.getPriorite() != null ? plan.getPriorite() : "—");
        prioriteLabel.setStyle("-fx-background-color: " + getCouleurPriorite(plan.getPriorite()) +
                "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label statutLabel = new Label(plan.getStatut() != null ? plan.getStatut() : "—");
        statutLabel.setStyle("-fx-background-color: " + getCouleurStatut(plan.getStatut()) +
                "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 11px;");

        header.getChildren().addAll(titreLabel, spacer, prioriteLabel, statutLabel);

        if (plan.getDescription() != null && !plan.getDescription().isBlank()) {
            Label desc = new Label(plan.getDescription());
            desc.setStyle("-fx-text-fill: #8a8d91;");
            desc.setWrapText(true);
            card.getChildren().add(desc);
        }

        HBox datesBox = new HBox(20);
        datesBox.setAlignment(Pos.CENTER_LEFT);
        if (plan.getDate_debut() != null)
            datesBox.getChildren().add(creerMiniInfo("Début", plan.getDate_debut().toString()));
        if (plan.getDate_fin() != null)
            datesBox.getChildren().add(creerMiniInfo("Fin", plan.getDate_fin().toString()));

        Region spacerActions = new Region();
        HBox.setHgrow(spacerActions, Priority.ALWAYS);

        Button btnModifier = new Button("✏ Modifier");
        btnModifier.setStyle("-fx-background-color: transparent; -fx-text-fill: #a78bfa; -fx-border-color: #8b5cf6; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 10;");
        btnModifier.setOnAction(e -> activerModeEdition(plan));

        Button btnSupprimer = new Button("🗑 Supprimer");
        btnSupprimer.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff477e; -fx-border-color: #ff477e; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 10;");
        btnSupprimer.setOnAction(e -> supprimerPlan(plan));

        HBox actionsBox = new HBox(8, spacerActions, btnModifier, btnSupprimer);
        card.getChildren().addAll(header, datesBox, actionsBox);
        return card;
    }

    private void activerModeEdition(PlanAction plan) {
        planEnEdition = plan;
        txtTitre.setText(plan.getTitre());
        txtDescription.setText(plan.getDescription() != null ? plan.getDescription() : "");
        comboPriorite.setValue(plan.getPriorite());
        comboStatut.setValue(plan.getStatut());
        dateDebut.setValue(plan.getDate_debut());
        dateFin.setValue(plan.getDate_fin());
        labelFormTitre.setText("✏ Modifier le plan d'action");
        btnAnnuler.setVisible(true);
        btnAnnuler.setManaged(true);
        reinitialiserStyles();
    }

    private void supprimerPlan(PlanAction plan) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Supprimer le plan");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Voulez-vous vraiment supprimer \"" + plan.getTitre() + "\" ?");
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    planActionService.delete(plan);
                } catch (SQLException e) {
                    System.err.println("Erreur suppression plan : " + e.getMessage());
                }
                chargerPlans();
            }
        });
    }

    @FXML
    void handleSauvegarder(ActionEvent event) {
        txtTitre.textProperty().set(txtTitre.getText());
        validerDates();

        if (!toutEstValide()) return;

        String titre = txtTitre.getText().trim();

        try {
            if (planEnEdition == null) {
                PlanAction nouveau = new PlanAction(
                    titre,
                    txtDescription.getText(),
                    comboPriorite.getValue(),
                    dateDebut.getValue(),
                    dateFin.getValue(),
                    objectifCourant.getId(),
                    comboStatut.getValue()
                );
                planActionService.create(nouveau);
            } else {
                planEnEdition.setTitre(titre);
                planEnEdition.setDescription(txtDescription.getText());
                planEnEdition.setPriorite(comboPriorite.getValue());
                planEnEdition.setStatut(comboStatut.getValue());
                planEnEdition.setDate_debut(dateDebut.getValue());
                planEnEdition.setDate_fin(dateFin.getValue());
                planActionService.update(planEnEdition);
            }
        } catch (SQLException e) {
            System.err.println("Erreur sauvegarde plan : " + e.getMessage());
        }

        reinitialiserFormulaire();
        chargerPlans();
    }

    @FXML
    void handleAnnuler(ActionEvent event) {
        reinitialiserFormulaire();
    }

    private void reinitialiserFormulaire() {
        planEnEdition = null;
        txtTitre.clear();
        txtDescription.clear();
        comboPriorite.setValue("Moyenne");
        comboStatut.setValue("À faire");
        dateDebut.setValue(LocalDate.now());
        dateFin.setValue(LocalDate.now().plusWeeks(1));
        labelFormTitre.setText("➕ Nouveau plan d'action");
        btnAnnuler.setVisible(false);
        btnAnnuler.setManaged(false);
        reinitialiserStyles();
    }

    // ── Validation ────────────────────────────────────────────────────────

    private void validerDates() {
        LocalDate debut = dateDebut.getValue();
        LocalDate fin   = dateFin.getValue();

        if (debut == null) {
            setErreur(dateDebut, errDateDebut, "La date de début est obligatoire.");
        } else {
            setValide(dateDebut, errDateDebut);
        }

        if (fin == null) {
            setErreur(dateFin, errDateFin, "La date de fin est obligatoire.");
        } else if (debut != null && !fin.isAfter(debut)) {
            setErreur(dateFin, errDateFin, "La date de fin doit être après la date de début.");
        } else {
            setValide(dateFin, errDateFin);
        }
    }

    private boolean toutEstValide() {
        String titre    = txtTitre.getText();
        LocalDate debut = dateDebut.getValue();
        LocalDate fin   = dateFin.getValue();

        boolean titreOk = titre != null && titre.trim().length() >= 3 && titre.trim().length() <= 100;
        boolean debutOk = debut != null;
        boolean finOk   = fin != null && debut != null && fin.isAfter(debut);

        return titreOk && debutOk && finOk;
    }

    private void setErreur(Control champ, Label labelErreur, String message) {
        champ.setStyle(STYLE_ERREUR);
        if (labelErreur != null) {
            labelErreur.setText("⚠ " + message);
            labelErreur.setVisible(true);
        }
    }

    private void setValide(Control champ, Label labelErreur) {
        champ.setStyle(STYLE_VALIDE);
        if (labelErreur != null) {
            labelErreur.setText("");
            labelErreur.setVisible(false);
        }
    }

    private void reinitialiserStyles() {
        txtTitre.setStyle("");
        dateDebut.setStyle("");
        dateFin.setStyle("");
        if (errTitre != null)     errTitre.setVisible(false);
        if (errDateDebut != null) errDateDebut.setVisible(false);
        if (errDateFin != null)   errDateFin.setVisible(false);
    }

    // ── Helpers visuels ───────────────────────────────────────────────────

    private VBox creerMiniInfo(String label, String valeur) {
        VBox vbox = new VBox(2);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #5a5d61; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label val = new Label(valeur);
        val.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 12px;");
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

    private String getCouleurStatut(String statut) {
        if (statut == null) return "#5a5d61";
        return switch (statut.toLowerCase()) {
            case "terminé", "termine" -> "#00d285";
            case "en cours"           -> "#6ea8fe";
            case "annulé", "annule"   -> "#ff477e";
            default                   -> "#5a5d61";
        };
    }
}
