package Controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import model.DrugInfo;
import service.OpenFDAService;

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
        Label loadingLabel = new Label("Recherche d'informations pharmacologiques...");
        loadingLabel.setStyle("-fx-text-fill: #c4b5fd;");
        resultsContainer.getChildren().add(loadingLabel);

        Task<List<DrugInfo>> task = new Task<>() {
            @Override
            protected List<DrugInfo> call() throws Exception {
                return fdaService.searchDrug(query);
            }
        };

        task.setOnSucceeded(e -> {
            resultsContainer.getChildren().clear();
            List<DrugInfo> drugs = task.getValue();
            if (drugs.isEmpty()) {
                Label noResult = new Label("Aucun médicament trouvé pour : " + query);
                noResult.setStyle("-fx-text-fill: #ef4444;");
                resultsContainer.getChildren().add(noResult);
            } else {
                for (DrugInfo drug : drugs) {
                    resultsContainer.getChildren().add(createDrugCard(drug));
                }
            }
        });

        task.setOnFailed(e -> {
            resultsContainer.getChildren().clear();
            Label errorLabel = new Label("Erreur de connexion : " + task.getException().getMessage());
            errorLabel.setStyle("-fx-text-fill: #ef4444;");
            resultsContainer.getChildren().add(errorLabel);
        });

        new Thread(task).start();
    }

    private VBox createDrugCard(DrugInfo drug) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #1e293b; -fx-padding: 20; -fx-background-radius: 12; -fx-border-color: #8b5cf6; -fx-border-width: 0.5;");

        Label title = new Label(drug.getBrandName() + " (" + drug.getGenericName() + ")");
        title.setStyle("-fx-text-fill: #c4b5fd; -fx-font-weight: bold; -fx-font-size: 19px;");

        Label indicationsTitle = new Label("Indications :");
        indicationsTitle.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold;");
        Label indications = new Label(drug.getIndications());
        indications.setStyle("-fx-text-fill: #e2e8f0;");
        indications.setWrapText(true);

        Label effectsTitle = new Label("Effets secondaires :");
        effectsTitle.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        Label effects = new Label(drug.getAdverseEffects());
        effects.setStyle("-fx-text-fill: #cbd5e1;");
        effects.setWrapText(true);

        card.getChildren().addAll(title, indicationsTitle, indications, effectsTitle, effects);
        return card;
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ReadSante.fxml"));
            Scene scene = ((Node) event.getSource()).getScene();
            scene.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
