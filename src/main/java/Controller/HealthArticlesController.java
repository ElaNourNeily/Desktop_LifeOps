package Controller;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import model.HealthArticle;
import service.HealthFinderService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class HealthArticlesController {

    @FXML private TextField searchField;
    @FXML private VBox articlesContainer;
    @FXML private Label lblResultCount;
    
    private final HealthFinderService healthService = new HealthFinderService();

    @FXML
    void searchArticles(ActionEvent event) {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        articlesContainer.getChildren().clear();
        lblResultCount.setText("Recherche en cours pour : " + query + "...");

        Task<List<HealthArticle>> task = new Task<>() {
            @Override
            protected List<HealthArticle> call() throws Exception {
                return healthService.searchArticles(query);
            }
        };

        task.setOnSucceeded(e -> {
            articlesContainer.getChildren().clear();
            List<HealthArticle> articles = task.getValue();
            if (articles.isEmpty()) {
                lblResultCount.setText("Aucun article trouvé pour : " + query);
            } else {
                lblResultCount.setText(articles.size() + " articles trouvés pour votre recherche.");
                for (HealthArticle art : articles) {
                    articlesContainer.getChildren().add(createArticleCard(art));
                }
            }
        });

        new Thread(task).start();
    }

    private VBox createArticleCard(HealthArticle art) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 10; -fx-padding: 20; -fx-border-color: #334155; -fx-border-radius: 10;");
        card.setSpacing(10);
        
        Label titleLabel = new Label(art.getTitle());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);
        
        Label categoriesLabel = new Label("Catégories : " + art.getCategories());
        categoriesLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        categoriesLabel.setWrapText(true);

        Hyperlink link = new Hyperlink("Lire l'article complet →");
        link.setStyle("-fx-text-fill: #38bdf8;");
        link.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI(art.getAccessibleUrl()));
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        card.getChildren().addAll(titleLabel, categoriesLabel, link);
        return card;
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ReadSante.fxml"));
            Scene scene = ((Node) event.getSource()).getScene();
            scene.setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
