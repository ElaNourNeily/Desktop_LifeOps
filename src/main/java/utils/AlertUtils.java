package utils;

import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ButtonType;
import javafx.scene.Node;
import javafx.stage.Stage;

public class AlertUtils {

    public static void showSuccess(String header, String content) {
        showAlert(Alert.AlertType.INFORMATION, "LifeOps - Succès", header, content);
    }

    public static void showError(String header, String content) {
        showAlert(Alert.AlertType.ERROR, "LifeOps - Erreur", header, content);
    }

    private static void showAlert(Alert.AlertType type, String title, String header, String content) {
        try {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);

            DialogPane dialogPane = alert.getDialogPane();
            
            // Applique le style directement via CSS Inline pour une visibilité maximale
            dialogPane.setStyle(
                "-fx-background-color: #1a1a2e; " +
                "-fx-border-color: #8b5cf6; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 10px; " +
                "-fx-background-radius: 10px;"
            );

            // On force la couleur de TOUS les textes en blanc brillant
            // On utilise un petit délai ou on attend l'affichage pour certains éléments
            // Mais le style sur le pane aide déjà énormément.
            
            // Injecter le CSS global pour que les labels héritent du style blanc
            if (AlertUtils.class.getResource("/style (2).css") != null) {
                dialogPane.getStylesheets().add(AlertUtils.class.getResource("/style (2).css").toExternalForm());
            }

            // Bouton OK très visible
            Node okButton = dialogPane.lookupButton(ButtonType.OK);
            if (okButton != null) {
                okButton.setStyle(
                    "-fx-background-color: #8b5cf6; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 8 20; " +
                    "-fx-background-radius: 5;"
                );
            }

            // Pour forcer la visibilité du texte dans le header et content
            // On passe par un lookup différé ou on injecte une règle CSS
            alert.show();
            
            // Une fois affiché on peut souvent accéder aux labels
            Node headerText = dialogPane.lookup(".header-panel .label");
            if (headerText != null) headerText.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");
            
            Node contentText = dialogPane.lookup(".content.label");
            if (contentText != null) contentText.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px;");

        } catch (Exception e) {
            Alert basic = new Alert(type);
            basic.setHeaderText(header);
            basic.setContentText(content);
            basic.show();
        }
    }
}
