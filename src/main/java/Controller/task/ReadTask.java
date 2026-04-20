package Controller.task;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import model.task.Tache;
import model.user.User;
import service.task.TacheService;
import service.user.Userservice;

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

    @FXML private ComboBox<User> userCombo;
    @FXML private ComboBox<String> sortCombo;

    @FXML private Button btnMesProjets;
    @FXML private Button btnNouvelleTache;

    private final TacheService tacheService = new TacheService();
    private final Userservice userService = new Userservice();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerOptionsDeTri();
        chargerUtilisateurs();
    }

    private void chargerOptionsDeTri() {
        sortCombo.getItems().addAll("Aucun tri", "Tri par Priorité", "Tri par Temps Estimé");
        sortCombo.getSelectionModel().selectFirst();

        sortCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (userCombo.getValue() != null) {
                chargerTachesParUtilisateur(userCombo.getValue().getId());
            }
        });
    }

    private void chargerUtilisateurs() {
        try {
            List<User> users = userService.findAll();
            userCombo.getItems().addAll(users);

            userCombo.setConverter(new StringConverter<User>() {
                @Override
                public String toString(User user) {
                    if (user == null) return "";
                    return user.getPrenom() + " " + user.getNom();
                }

                @Override
                public User fromString(String string) {
                    return null;
                }
            });

            userCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    chargerTachesParUtilisateur(newSelection.getId());
                }
            });

            if (!users.isEmpty()) {
                userCombo.getSelectionModel().selectFirst();
            }

        } catch (SQLException e) {
            System.err.println("Erreur chargement utilisateurs : " + e.getMessage());
        }
    }

    @FXML
    private void naviguerVersCreatask(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/task/Creatask.fxml"));
            Parent root = loader.load();

            User selectedUser = userCombo.getValue();
            if (selectedUser != null) {
                root.setUserData(selectedUser);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur navigation Creatask : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void naviguerVersProjets(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/task/TaskSpace.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur navigation TaskSpace : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void naviguerVersUpdateTask(ActionEvent event, Tache tacheSelectionnee) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/task/Updatetask.fxml"));
            Parent root = loader.load();

            root.setUserData(tacheSelectionnee);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur navigation UpdateTask : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerTachesParUtilisateur(int userId) {
        nettoyerColonne(colAFaire);
        nettoyerColonne(colEnCours);
        nettoyerColonne(colEnRevision);
        nettoyerColonne(colTermine);

        int cA = 0, cE = 0, cR = 0, cT = 0;

        try {
            List<Tache> taches = tacheService.recupererParUtilisateur(userId);

            String triChoisi = sortCombo.getValue();
            if ("Tri par Priorité".equals(triChoisi)) {
                taches.sort((t1, t2) -> {
                    if (t1.getPriorite() == null && t2.getPriorite() == null) return 0;
                    if (t1.getPriorite() == null) return 1;
                    if (t2.getPriorite() == null) return -1;
                    return t1.getPriorite().compareTo(t2.getPriorite());
                });
            } else if ("Tri par Temps Estimé".equals(triChoisi)) {
                taches.sort((t1, t2) -> Integer.compare(t1.getDifficulte(), t2.getDifficulte()));
            }

            for (Tache t : taches) {
                if (t.getStatut() == null) continue;

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

    private VBox creerCarteTache(Tache t) {
        VBox card = new VBox(10);
        card.getStyleClass().add("task-card");
        card.setPadding(new Insets(15));

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label lblPriorite = new Label(t.getPriorite() != null ? t.getPriorite().getValeur() : "Basse");
        lblPriorite.getStyleClass().add(priorityStyleClass(t.getPriorite() != null ? t.getPriorite().getValeur() : "basse"));

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);

        headerBox.getChildren().addAll(lblPriorite, sp1, creerDifficultyDots(t.getDifficulte()));

        Label lblTitre = new Label(t.getTitre());
        lblTitre.getStyleClass().add("task-title");
        lblTitre.setWrapText(true);

        Label lblDesc = new Label(t.getDescription());
        lblDesc.getStyleClass().add("task-description");
        lblDesc.setWrapText(true);

        HBox footerBox = new HBox(10);
        footerBox.setAlignment(Pos.CENTER_LEFT);

        Label lblDate = new Label(formatDeadline(t.getDeadline()));
        lblDate.getStyleClass().add("task-meta");

        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        Button btnEdit = new Button("✏️");
        btnEdit.getStyleClass().addAll("action-icon-button", "edit-button");
        btnEdit.setOnAction(e -> naviguerVersUpdateTask(e, t));

        Button btnDel = new Button("🗑");
        btnDel.getStyleClass().addAll("action-icon-button", "delete-button");
        btnDel.setOnAction(e -> supprimerTache(t));

        footerBox.getChildren().addAll(lblDate, sp2, btnEdit, btnDel);

        card.getChildren().addAll(headerBox, lblTitre, lblDesc, footerBox);
        return card;
    }

    private void supprimerTache(Tache t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la tâche ?");
        confirm.setContentText("Voulez-vous vraiment supprimer « " + t.getTitre() + " » ?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                tacheService.supprimer(t.getId());
                if (userCombo.getValue() != null) {
                    chargerTachesParUtilisateur(userCombo.getValue().getId());
                }
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
        return "📅 " + SDF.format(deadline);
    }
}