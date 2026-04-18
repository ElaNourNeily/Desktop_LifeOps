package Controllers;

import Model.Objectif;
import Service.ObjectifService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;

public class AjouterObjectifController {

    @FXML private TextField txtTitre;
    @FXML private ComboBox<String> comboCategorie;
    @FXML private ComboBox<String> comboStatut;
    @FXML private TextArea txtDescription;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private Slider sliderProgression;
    @FXML private Label labelProgression;

    // Labels d'erreur inline
    @FXML private Label errTitre;
    @FXML private Label errCategorie;
    @FXML private Label errDateDebut;
    @FXML private Label errDateFin;

    private static final String STYLE_ERREUR  = "-fx-border-color: #ff477e; -fx-border-width: 1.5; -fx-border-radius: 6;";
    private static final String STYLE_NORMAL  = "";
    private static final String STYLE_VALIDE  = "-fx-border-color: #00d285; -fx-border-width: 1.5; -fx-border-radius: 6;";

    private final ObjectifService objectifService = new ObjectifService();
    private Objectif nouvelObjectif;

    @FXML
    public void initialize() {
        comboCategorie.setItems(FXCollections.observableArrayList(
            "Santé", "Loisirs", "Personnel", "Finances", "Etudes"
        ));
        comboCategorie.setValue("Personnel");

        comboStatut.setItems(FXCollections.observableArrayList(
            "En cours", "Complété", "Abandonné", "En pause"
        ));
        comboStatut.setValue("En cours");

        dateDebut.setValue(LocalDate.now());
        dateFin.setValue(LocalDate.now().plusMonths(1));

        sliderProgression.valueProperty().addListener((obs, oldVal, newVal) ->
            labelProgression.setText((int) newVal.doubleValue() + "%")
        );

        // ── Validation en temps réel ──────────────────────────────────────

        // Titre : obligatoire, 3 à 100 caractères
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

        // Catégorie : obligatoire
        comboCategorie.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                setErreur(comboCategorie, errCategorie, "Veuillez choisir une catégorie.");
            } else {
                setValide(comboCategorie, errCategorie);
            }
        });

        // Date début : obligatoire
        dateDebut.valueProperty().addListener((obs, oldVal, newVal) -> {
            validerDates();
        });

        // Date fin : obligatoire + doit être après date début
        dateFin.valueProperty().addListener((obs, oldVal, newVal) -> {
            validerDates();
        });
    }

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

    @FXML
    void handleEnregistrer(ActionEvent event) {
        // Déclencher toutes les validations
        txtTitre.textProperty().set(txtTitre.getText()); // force listener
        validerDates();
        if (comboCategorie.getValue() == null)
            setErreur(comboCategorie, errCategorie, "Veuillez choisir une catégorie.");

        if (!toutEstValide()) return;

        String titre      = txtTitre.getText().trim();
        String categorie  = comboCategorie.getValue();
        String statut     = comboStatut.getValue() != null ? comboStatut.getValue() : "En cours";
        String description = txtDescription.getText();
        LocalDate debut   = dateDebut.getValue();
        LocalDate fin     = dateFin.getValue();
        int progression   = (int) sliderProgression.getValue();

        nouvelObjectif = new Objectif(titre, description, categorie, statut, debut, fin, progression);
        objectifService.create(nouvelObjectif);

        Stage stage = (Stage) txtTitre.getScene().getWindow();
        stage.close();
    }

    // ── Helpers de validation ─────────────────────────────────────────────

    private boolean toutEstValide() {
        String titre = txtTitre.getText();
        LocalDate debut = dateDebut.getValue();
        LocalDate fin   = dateFin.getValue();

        boolean titreOk    = titre != null && titre.trim().length() >= 3 && titre.trim().length() <= 100;
        boolean categorieOk = comboCategorie.getValue() != null && !comboCategorie.getValue().isEmpty();
        boolean debutOk    = debut != null;
        boolean finOk      = fin != null && debut != null && fin.isAfter(debut);

        return titreOk && categorieOk && debutOk && finOk;
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

    public Objectif getNouvelObjectif() {
        return nouvelObjectif;
    }
}
