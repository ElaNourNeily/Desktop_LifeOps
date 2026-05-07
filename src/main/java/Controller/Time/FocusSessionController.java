package Controller.Time;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Time.Activite;
import service.Time.ActiviteService;

import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;

public class FocusSessionController {

    @FXML private Label lblActivityTitle;
    @FXML private Label lblTimer;
    @FXML private Label lblFocusStatus;
    @FXML private Button btnStartPause;
    @FXML private Circle timerCircle;
    @FXML private CheckBox chkMute;

    private Activite activite;
    private final ActiviteService service = new ActiviteService();
    
    private int secondsRemaining = 25 * 60;
    private boolean isRunning = false;
    private boolean isBreak = false;
    private Timeline timeline;

    public void setActivite(Activite a) {
        this.activite = a;
        lblActivityTitle.setText(a.getTitre());
        
        // Change status to en_cours when starting focus
        try {
            a.setEtat("en_cours");
            a.setHeureDebutReelle(Time.valueOf(LocalTime.now()));
            service.modifier(a);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void tick() {
        if (secondsRemaining > 0) {
            secondsRemaining--;
            updateTimerDisplay();
        } else {
            handleSessionEnd();
        }
    }

    private void updateTimerDisplay() {
        int mins = secondsRemaining / 60;
        int secs = secondsRemaining % 60;
        lblTimer.setText(String.format("%02d:%02d", mins, secs));
        
        // Update circle stroke dash offset (visual progress)
        double total = isBreak ? 5 * 60 : 25 * 60;
        double progress = (total - secondsRemaining) / total;
        // Circonférence = 2 * PI * 90 = ~565
        timerCircle.setStrokeDashOffset(565 * progress);
    }

    @FXML
    private void handleStartPause() {
        if (isRunning) {
            timeline.pause();
            btnStartPause.setText("DÉMARRER");
        } else {
            timeline.play();
            btnStartPause.setText("PAUSE");
        }
        isRunning = !isRunning;
    }

    @FXML
    private void handleStop() {
        timeline.stop();
        close();
    }

    private void handleSessionEnd() {
        timeline.stop();
        isRunning = false;
        
        if (!isBreak) {
            // End of work session
            java.awt.Toolkit.getDefaultToolkit().beep();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Focus Session");
            alert.setHeaderText("Félicitations !");
            alert.setContentText("Vous avez terminé votre session de travail. Voulez-vous prendre une pause de 5 minutes ?");
            
            alert.showAndWait().ifPresent(response -> {
                if (response.getText().contains("OK") || response.getButtonData().isDefaultButton()) {
                    startBreak();
                } else {
                    offerCompletion();
                }
            });
        } else {
            // End of break
            isBreak = false;
            secondsRemaining = 25 * 60;
            lblFocusStatus.setText("SESSION DE TRAVAIL");
            lblFocusStatus.getStyleClass().remove("badge-warning");
            lblFocusStatus.getStyleClass().add("badge-ai");
            btnStartPause.setText("DÉMARRER");
            updateTimerDisplay();
        }
    }

    private void startBreak() {
        isBreak = true;
        secondsRemaining = 5 * 60;
        lblFocusStatus.setText("SESSION DE PAUSE");
        lblFocusStatus.getStyleClass().remove("badge-ai");
        lblFocusStatus.getStyleClass().add("badge-warning");
        btnStartPause.setText("DÉMARRER");
        updateTimerDisplay();
    }

    private void offerCompletion() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Focus Session");
        alert.setHeaderText("Activité en cours");
        alert.setContentText("Voulez-vous marquer l'activité \"" + activite.getTitre() + "\" comme terminée ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response.getText().contains("OK") || response.getButtonData().isDefaultButton()) {
                try {
                    activite.setEtat("terminé");
                    activite.setHeureFinReelle(Time.valueOf(LocalTime.now()));
                    service.modifier(activite);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            close();
        });
    }

    private void close() {
        ((Stage) lblTimer.getScene().getWindow()).close();
    }
}
