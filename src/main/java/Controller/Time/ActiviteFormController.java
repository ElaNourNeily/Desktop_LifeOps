package Controller.Time;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Time.Activite;
import service.Time.ActiviteService;
import service.Time.PlanningService;

import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.util.List;

public class ActiviteFormController {

    @FXML private Label lblFormTitle;
    @FXML private Label lblDateSubtitle;
    @FXML private TextField txtTitre;
    @FXML private ComboBox<String> comboCategorie;
    @FXML private ComboBox<String> comboPriorite;
    @FXML private ComboBox<String> comboUrgence;
    @FXML private ComboBox<String> comboRappel;
    @FXML private TextField txtHeureDebut;
    @FXML private TextField txtHeureFin;
    @FXML private Button btnDelete;
    @FXML private Label lblWeatherWarning;
    @FXML private VBox vboxNews;
    @FXML private HBox boxColorEmeraude, boxColorBleu, boxColorOrange, boxColorRose, boxColorViolet;

    // Recurrence fields
    @FXML private CheckBox chkRecurrent;
    @FXML private HBox boxRecurrenceSettings;
    @FXML private ComboBox<String> comboRecurrenceType;
    @FXML private TextField txtRecurrenceInterval;
    @FXML private TextField txtRecurrenceDays;

    private final PlanningService planningService = new PlanningService();
    private final service.Time.external.WeatherService weatherService = new service.Time.external.WeatherService();
    private final service.Time.external.NewsService newsService = new service.Time.external.NewsService();

    private final ActiviteService service = new ActiviteService();
    private Activite currentActivite;
    private int currentPlanningId;
    private java.time.LocalDate currentPlanningDate;
    private Runnable onSaveCallback;
    private String selectedColor = "#8b5cf6"; // Default Violet

    @FXML
    public void initialize() {
        comboCategorie.setItems(FXCollections.observableArrayList("Travail", "Santé", "Finance", "Loisir", "Autre"));
        comboPriorite.setItems(FXCollections.observableArrayList("Basse", "Moyenne", "Haute"));
        comboUrgence.setItems(FXCollections.observableArrayList("Basse", "Moyenne", "Haute"));
        comboRappel.setItems(FXCollections.observableArrayList("Aucun", "15 minutes", "30 minutes", "1 heure", "2 heures"));
        comboRecurrenceType.setItems(FXCollections.observableArrayList("DAILY", "WEEKLY", "MONTHLY"));
        
        // Listeners for context info
        comboCategorie.valueProperty().addListener((obs, oldVal, newVal) -> {
            checkWeatherForActivity(newVal);
            refreshNews(newVal);
        });
        
        txtTitre.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Lost focus
                refreshNews(comboCategorie.getValue());
            }
        });

        // Default values
        comboPriorite.setValue("Basse");
        comboUrgence.setValue("Moyenne");
        comboRappel.setValue("Aucun");
        resetColorHighlights();
        highlightColorBox(boxColorViolet);
    }

    private void refreshNews(String category) {
        if (category == null) return;
        
        String titre = txtTitre.getText();
        vboxNews.getChildren().clear();
        Label lblLoading = new Label("Chargement des conseils...");
        lblLoading.setStyle("-fx-text-fill: #71717a; -fx-font-size: 10; -fx-font-style: italic;");
        vboxNews.getChildren().add(lblLoading);

        javafx.concurrent.Task<List<model.Time.external.NewsArticle>> newsTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<model.Time.external.NewsArticle> call() throws Exception {
                return newsService.getActualitesParCategorie(titre, category, 3);
            }
        };

        newsTask.setOnSucceeded(e -> {
            vboxNews.getChildren().clear();
            List<model.Time.external.NewsArticle> articles = newsTask.getValue();
            if (articles.isEmpty()) {
                Label lblNone = new Label("Aucune actualité trouvée.");
                lblNone.setStyle("-fx-text-fill: #71717a; -fx-font-size: 10;");
                vboxNews.getChildren().add(lblNone);
            } else {
                for (model.Time.external.NewsArticle art : articles) {
                    addNewsCard(art);
                }
            }
        });

        newsTask.setOnFailed(e -> {
            vboxNews.getChildren().clear();
            Label lblError = new Label("Erreur lors du chargement des news.");
            lblError.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 10;");
            vboxNews.getChildren().add(lblError);
        });

        new Thread(newsTask).start();
    }

    private void addNewsCard(model.Time.external.NewsArticle art) {
        VBox card = new VBox(2);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-padding: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        
        Label lblTitle = new Label(art.getTitle());
        lblTitle.setWrapText(true);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11;");
        
        Label lblSource = new Label(art.getSource() + " • " + art.getPublishedAt().substring(0, 10));
        lblSource.setStyle("-fx-text-fill: #71717a; -fx-font-size: 9;");
        
        card.getChildren().addAll(lblTitle, lblSource);
        card.setOnMouseClicked(e -> {
            if (art.getUrl() != null && !art.getUrl().isEmpty()) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(art.getUrl()));
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
        
        vboxNews.getChildren().add(card);
    }

    private void checkWeatherForActivity(String category) {
        if (category == null || currentPlanningDate == null) return;
        
        if (category.equals("Santé") || category.equals("Loisir")) {
            javafx.concurrent.Task<Integer> weatherCheckTask = new javafx.concurrent.Task<>() {
                @Override
                protected Integer call() throws Exception {
                    // Coordinates for Tunis
                    return weatherService.getRawWeatherCode(36.8065, 10.1815, currentPlanningDate);
                }
            };
            
            weatherCheckTask.setOnSucceeded(e -> {
                int code = weatherCheckTask.getValue();
                if (weatherService.isBadWeather(code)) {
                    lblWeatherWarning.setText("⚠️ Attention : Mauvais temps prévu (" + weatherService.interpretWeatherCode(code) + "). Pensez à adapter votre activité !");
                } else {
                    lblWeatherWarning.setText("");
                }
            });
            
            new Thread(weatherCheckTask).start();
        } else {
            lblWeatherWarning.setText("");
        }
    }

    public void setPlanningId(int id) { this.currentPlanningId = id; }
    
    public void setOnSave(Runnable callback) { this.onSaveCallback = callback; }

    public void setDateSubtitle(java.util.Date date) {
        if (date != null) {
            this.currentPlanningDate = new java.sql.Date(date.getTime()).toLocalDate();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            lblDateSubtitle.setText("Pour le " + sdf.format(date));
        }
    }

    public void setActivite(Activite a) {
        this.currentActivite = a;
        if (a != null) {
            lblFormTitle.setText("Modifier l'Activité");
            txtTitre.setText(a.getTitre());
            comboCategorie.setValue(a.getCategorie());
            comboPriorite.setValue(priorityIntToString(a.getPriorite()));
            comboUrgence.setValue(a.getNiveauUrgence().substring(0, 1).toUpperCase() + a.getNiveauUrgence().substring(1).toLowerCase());
            txtHeureDebut.setText(a.getHeureDebutEstimee().toString().substring(0, 5));
            txtHeureFin.setText(a.getHeureFinEstimee().toString().substring(0, 5));
            comboRappel.setValue(minutesToRappelString(a.getMinutesRappel()));
            
            selectedColor = a.getCouleur();
            updateColorSelectionUI(selectedColor);

            chkRecurrent.setSelected(a.isRecurrent());
            handleRecurrenceToggle();
            comboRecurrenceType.setValue(a.getRecurrenceType());
            txtRecurrenceInterval.setText(String.valueOf(a.getRecurrenceInterval()));
            txtRecurrenceDays.setText(a.getRecurrenceDays());
            
            this.currentPlanningId = a.getPlanningId();
            btnDelete.setVisible(true);
            btnDelete.setManaged(true);
        }
    }

    public void setInitialTime(int hour) {
        txtHeureDebut.setText(String.format("%02d:00", hour));
        txtHeureFin.setText(String.format("%02d:00", hour + 1));
    }

    @FXML
    private void handleSave() {
        if (currentPlanningId <= 0 && currentActivite == null) {
            showAlert("Erreur", "Impossible d'associer l'activité : ID de planning invalide.");
            return;
        }
        if (validate()) {
            try {
                boolean isNew = (currentActivite == null);
                if (isNew) currentActivite = new Activite();

                currentActivite.setTitre(txtTitre.getText());
                currentActivite.setCategorie(comboCategorie.getValue());
                currentActivite.setPriorite(priorityStringToInt(comboPriorite.getValue()));
                
                String start = txtHeureDebut.getText();
                if (start.length() == 5) start += ":00";
                String end = txtHeureFin.getText();
                if (end.length() == 5) end += ":00";
                
                currentActivite.setHeureDebutEstimee(Time.valueOf(start));
                currentActivite.setHeureFinEstimee(Time.valueOf(end));
                
                // Check for overlaps (Conflicts)
                if (service.hasOverlap(currentPlanningId, currentActivite.getHeureDebutEstimee(), 
                        currentActivite.getHeureFinEstimee(), isNew ? -1 : currentActivite.getId())) {
                    showAlert("Conflit d'horaire", "Une autre activité est déjà prévue sur ce créneau.");
                    return;
                }

                // Calculate duration
                LocalTime s = LocalTime.parse(start);
                LocalTime e = LocalTime.parse(end);
                currentActivite.setDuree((int) java.time.Duration.between(s, e).toMinutes());
                
                currentActivite.setCouleur(selectedColor);
                currentActivite.setNiveauUrgence(comboUrgence.getValue().toLowerCase());
                currentActivite.setMinutesRappel(rappelStringToMinutes(comboRappel.getValue()));
                currentActivite.setPlanningId(currentPlanningId);
                currentActivite.setEtat("en_attente");
                
                // Set a unique group ID if it's a new recurrent activity
                if (isNew && chkRecurrent.isSelected()) {
                    currentActivite.setRecurrenceGroupId((int) (System.currentTimeMillis() / 1000));
                }

                currentActivite.setRecurrent(chkRecurrent.isSelected());
                if (chkRecurrent.isSelected()) {
                    currentActivite.setRecurrenceType(comboRecurrenceType.getValue());
                    currentActivite.setRecurrenceInterval(Integer.parseInt(txtRecurrenceInterval.getText()));
                    currentActivite.setRecurrenceDays(txtRecurrenceDays.getText());
                }

                if (isNew) {
                    service.ajouter(currentActivite);
                    if (currentActivite.isRecurrent()) {
                        genererOccurrences(currentActivite);
                    }
                } else {
                    service.modifier(currentActivite);
                }

                if (onSaveCallback != null) onSaveCallback.run();
                close();
            } catch (SQLException e) {
                showAlert("Erreur", "Base de données : " + e.getMessage());
            } catch (Exception e) {
                showAlert("Erreur", "Format invalide : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (currentActivite != null) {
            try {
                if (currentActivite.getRecurrenceGroupId() > 0) {
                    // Ask if user wants to delete only this or the whole series
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Activité Récurrente");
                    alert.setHeaderText("Cette activité fait partie d'une série.");
                    alert.setContentText("Voulez-vous supprimer toute la série ou seulement cette occurrence ?");
                    
                    javafx.scene.control.ButtonType btnSerie = new javafx.scene.control.ButtonType("Toute la série");
                    javafx.scene.control.ButtonType btnOnlyThis = new javafx.scene.control.ButtonType("Seulement celle-ci");
                    javafx.scene.control.ButtonType btnCancel = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                    
                    alert.getButtonTypes().setAll(btnSerie, btnOnlyThis, btnCancel);
                    
                    java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == btnSerie) {
                        service.supprimerSerie(currentActivite.getRecurrenceGroupId());
                    } else if (result.isPresent() && result.get() == btnOnlyThis) {
                        service.supprimer(currentActivite.getId());
                    } else {
                        return; // Cancelled
                    }
                } else {
                    service.supprimer(currentActivite.getId());
                }
                
                if (onSaveCallback != null) onSaveCallback.run();
                close();
            } catch (SQLException e) {
                showAlert("Erreur", "Suppression échouée : " + e.getMessage());
            }
        }
    }

    @FXML private void handleCancel() { close(); }

    @FXML private void selectEmeraude() { selectColor("#10b981", boxColorEmeraude); }
    @FXML private void selectBleu() { selectColor("#3b82f6", boxColorBleu); }
    @FXML private void selectOrange() { selectColor("#f59e0b", boxColorOrange); }
    @FXML private void selectRose() { selectColor("#ec4899", boxColorRose); }
    @FXML private void selectViolet() { selectColor("#8b5cf6", boxColorViolet); }

    private void selectColor(String hex, HBox box) {
        selectedColor = hex;
        resetColorHighlights();
        highlightColorBox(box);
    }

    private void resetColorHighlights() {
        HBox[] boxes = {boxColorEmeraude, boxColorBleu, boxColorOrange, boxColorRose, boxColorViolet};
        for (HBox b : boxes) {
            b.setStyle("-fx-opacity: 0.6; -fx-padding: 5; -fx-background-radius: 8;");
            b.getChildren().get(0).setStyle(""); // Circle
        }
    }

    private void highlightColorBox(HBox box) {
        box.setStyle("-fx-opacity: 1.0; -fx-background-color: rgba(255,255,255,0.05); -fx-padding: 5; -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8;");
        // We could also add a drop shadow to the circle here but style.css handles it via class if we wanted
    }

    private void updateColorSelectionUI(String color) {
        if (color == null) return;
        resetColorHighlights();
        if (color.equalsIgnoreCase("#10b981")) highlightColorBox(boxColorEmeraude);
        else if (color.equalsIgnoreCase("#3b82f6")) highlightColorBox(boxColorBleu);
        else if (color.equalsIgnoreCase("#f59e0b")) highlightColorBox(boxColorOrange);
        else if (color.equalsIgnoreCase("#ec4899")) highlightColorBox(boxColorRose);
        else if (color.equalsIgnoreCase("#8b5cf6")) highlightColorBox(boxColorViolet);
    }

    private boolean validate() {
        if (txtTitre.getText().isEmpty()) { showAlert("Erreur", "Le titre est requis."); return false; }
        if (comboCategorie.getValue() == null) { showAlert("Erreur", "La catégorie est requise."); return false; }
        try {
            String start = txtHeureDebut.getText();
            if (start.length() == 5) start += ":00";
            String end = txtHeureFin.getText();
            if (end.length() == 5) end += ":00";
            LocalTime.parse(start);
            LocalTime.parse(end);
        } catch (Exception e) {
            showAlert("Erreur", "Format d'heure invalide (ex: 08:00).");
            return false;
        }
        return true;
    }

    private void close() { ((Stage) txtTitre.getScene().getWindow()).close(); }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(content); alert.showAndWait();
    }

    private String priorityIntToString(int p) {
        return switch (p) { case 1 -> "Basse"; case 2 -> "Moyenne"; case 3 -> "Haute"; default -> "Basse"; };
    }

    private int priorityStringToInt(String p) {
        if (p == null) return 1;
        return switch (p) { case "Basse" -> 1; case "Moyenne" -> 2; case "Haute" -> 3; default -> 1; };
    }

    private String minutesToRappelString(int minutes) {
        return switch (minutes) {
            case 0 -> "Aucun";
            case 15 -> "15 minutes";
            case 30 -> "30 minutes";
            case 60 -> "1 heure";
            case 120 -> "2 heures";
            default -> "Aucun";
        };
    }

    private int rappelStringToMinutes(String rappel) {
        if (rappel == null) return 0;
        return switch (rappel) {
            case "15 minutes" -> 15;
            case "30 minutes" -> 30;
            case "1 heure" -> 60;
            case "2 heures" -> 120;
            default -> 0;
        };
    }

    @FXML
    private void handleRecurrenceToggle() {
        boolean show = chkRecurrent.isSelected();
        boxRecurrenceSettings.setVisible(show);
        boxRecurrenceSettings.setManaged(show);
    }

    private void genererOccurrences(Activite base) throws SQLException {
        model.Time.Planning p = planningService.recupererParId(base.getPlanningId());
        if (p == null) return;
        java.time.LocalDate startDate = p.getDate().toLocalDate();

        int maxOccurrences = 60;
        int interval = Math.max(1, base.getRecurrenceInterval());
        int occurrencesCreated = 0;
        
        java.time.LocalDate currentDay = startDate;
        
        // Loop up to 1 year lookahead
        for (int i = 1; i < 365; i++) {
            currentDay = currentDay.plusDays(1);
            boolean shouldAdd = false;

            if ("DAILY".equals(base.getRecurrenceType())) {
                if (i % interval == 0) shouldAdd = true;
            } else if ("WEEKLY".equals(base.getRecurrenceType())) {
                // Precise weekly logic: check day short names
                if ((i / 7) % interval == 0) {
                    String dayName = currentDay.getDayOfWeek().name().substring(0, 3); // MON, TUE...
                    String allowedDays = (base.getRecurrenceDays() == null) ? "" : base.getRecurrenceDays().toUpperCase();
                    if (allowedDays.isEmpty() || allowedDays.contains(dayName)) {
                        shouldAdd = true;
                    }
                }
            } else if ("MONTHLY".equals(base.getRecurrenceType())) {
                if (currentDay.getDayOfMonth() == startDate.getDayOfMonth()) {
                    // Approximate interval for months
                    shouldAdd = true;
                }
            }

            if (shouldAdd) {
                model.Time.Planning targetP = planningService.recupererParDate(currentDay, p.getUtilisateurId());
                if (targetP == null) {
                    targetP = new model.Time.Planning(java.sql.Date.valueOf(currentDay), true, p.getHeureDebutJournee(), p.getHeureFinJournee(), p.getUtilisateurId());
                    planningService.ajouter(targetP);
                }
                
                // Check if already exists to avoid duplicates
                if (service.hasOverlap(targetP.getId(), base.getHeureDebutEstimee(), base.getHeureFinEstimee(), -1)) {
                    continue; // Skip if there's a conflict
                }

                Activite occ = new Activite(0, base.getTitre(), base.getDuree(), base.getPriorite(), "en_attente", 
                    base.getHeureDebutEstimee(), base.getHeureFinEstimee(), base.getNiveauUrgence(), 
                    base.getCategorie(), base.getCouleur(), false, targetP.getId(), base.getMinutesRappel());
                
                occ.setRecurrent(true);
                occ.setRecurrenceType(base.getRecurrenceType());
                occ.setRecurrenceInterval(base.getRecurrenceInterval());
                occ.setRecurrenceDays(base.getRecurrenceDays());
                occ.setRecurrenceGroupId(base.getRecurrenceGroupId());
                
                service.ajouter(occ);
                occurrencesCreated++;
                if (occurrencesCreated >= maxOccurrences) break;
            }
        }
        System.out.println("[RECURRENCE] Created " + occurrencesCreated + " occurrences for Group ID: " + base.getRecurrenceGroupId());
    }
}
