package Controller.health;

import Model.health.HealthArticle;
import controller.user.MainLayoutController;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import service.health.HealthFinderService;

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
            @Override protected List<HealthArticle> call() throws Exception { return healthService.searchArticles(query); }
        };

        task.setOnSucceeded(e -> {
            articlesContainer.getChildren().clear();
            List<HealthArticle> articles = task.getValue();
            if (articles.isEmpty()) {
                lblResultCount.setText("Aucun article trouvé pour : " + query);
            } else {
                lblResultCount.setText(articles.size() + " articles trouvés");
                articles.forEach(a -> articlesContainer.getChildren().add(createCard(a)));
            }
        });

        new Thread(task).start();
    }

    private VBox createCard(HealthArticle art) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: #1c1e24; -fx-background-radius: 12; " +
                "-fx-border-color: #2a2d32; -fx-border-width: 1; -fx-border-radius: 12;");

        Label title = new Label(art.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        title.setWrapText(true);

        Label cats = new Label(art.getCategories());
        cats.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 12px;");
        cats.setWrapText(true);

        Hyperlink link = new Hyperlink("Lire l'article →");
        link.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 13px;");
        link.setOnAction(e -> {
            try { Desktop.getDesktop().browse(new URI(art.getAccessibleUrl())); }
            catch (Exception ex) { ex.printStackTrace(); }
        });

        card.getChildren().addAll(title, cats, link);
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
