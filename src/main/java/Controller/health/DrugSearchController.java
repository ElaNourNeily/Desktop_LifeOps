package Controller.health;

import Model.health.DrugInfo;
import Controller.user.MainLayoutController;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import service.health.OpenFDAService;

import java.io.IOException;
import java.util.List;

public class DrugSearchController {

    @FXML private TextField txtKeyword;
    @FXML private VBox resultsContainer;

    private final OpenFDAService fdaService = new OpenFDAService();

    @FXML
    void onSearch(ActionEvent event) {
        String query = txtKeyword.getText().trim();
        if (query.isEmpty()) return;

        resultsContainer.getChildren().clear();
        Label loading = new Label("🔍 Recherche en cours...");
        loading.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 14px;");
        resultsContainer.getChildren().add(loading);

        Task<List<DrugInfo>> task = new Task<>() {
            @Override protected List<DrugInfo> call() throws Exception { return fdaService.searchDrug(query); }
        };

        task.setOnSucceeded(e -> {
            resultsContainer.getChildren().clear();
            List<DrugInfo> drugs = task.getValue();
            if (drugs.isEmpty()) {
                Label none = new Label("Aucun médicament trouvé pour : " + query);
                none.setStyle("-fx-text-fill: #f87171; -fx-font-size: 14px;");
                resultsContainer.getChildren().add(none);
            } else {
                drugs.forEach(d -> resultsContainer.getChildren().add(createCard(d)));
            }
        });

        task.setOnFailed(e -> {
            resultsContainer.getChildren().clear();
            Label err = new Label("Erreur : " + task.getException().getMessage());
            err.setStyle("-fx-text-fill: #f87171;");
            resultsContainer.getChildren().add(err);
        });

        new Thread(task).start();
    }

    private VBox createCard(DrugInfo drug) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle("-fx-background-color: #1c1e24; -fx-background-radius: 12; " +
                "-fx-border-color: rgba(139,92,246,0.3); -fx-border-width: 1; -fx-border-radius: 12;");

        Label title = new Label(drug.getBrandName() + " (" + drug.getGenericName() + ")");
        title.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold; -fx-font-size: 16px;");
        title.setWrapText(true);

        Label indTitle = new Label("Indications :");
        indTitle.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label ind = new Label(drug.getIndications());
        ind.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        ind.setWrapText(true);

        Label effTitle = new Label("Effets secondaires :");
        effTitle.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label eff = new Label(drug.getAdverseEffects());
        eff.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 13px;");
        eff.setWrapText(true);

        card.getChildren().addAll(title, indTitle, ind, effTitle, eff);
        return card;
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/health/Sante.fxml"));
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) ctrl.loadContent(view);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
