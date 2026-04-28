package controller;

import enums.PrioriteTache;
import javafx.fxml.FXML;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.task.Tache;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TaskCardController {

    @FXML private VBox taskCardRoot;
    @FXML private Label lblTitle;
    @FXML private Label lblDescription;
    @FXML private Label lblEstimatedTime;
    @FXML private Label lblDeadlineAlert;
    @FXML private Label lblPriorityBadge;
    @FXML private Label lblRankBadge;
    @FXML private HBox trackingBadge;
    @FXML private Region trackingDot;
    @FXML private Label lblTrackingTime;
    @FXML private Region dot1;
    @FXML private Region dot2;
    @FXML private Region dot3;
    @FXML private Region dot4;

    private Tache task;
    private BoardViewController boardController;
    private ParallelTransition dotPulse;
    private Timeline timeTicker;
    private double score;
    private int rank = -1;

    public void setData(Tache task, BoardViewController boardController) {
        setData(task, boardController, -1, 0.0, false);
    }

    public void setData(Tache task, BoardViewController boardController, int rank, double score, boolean recommended) {
        this.task = task;
        this.boardController = boardController;
        this.rank = rank;
        this.score = score;

        lblTitle.setText(task.getTitre());
        lblDescription.setText(task.getDescription() == null || task.getDescription().isBlank() ? "-" : task.getDescription());
        setupDeadlineDisplay();
        setupPriorityBadge();
        setRank(rank);
        setScore(score);
        setRecommended(recommended);

        setupTrackingBadge();
    }

    public void setRank(int rank) {
        this.rank = rank;
        boolean show = rank > 0;
        lblRankBadge.setVisible(show);
        lblRankBadge.setManaged(show);
        if (show) lblRankBadge.setText("#" + rank);
    }

    public void setScore(double score) {
        this.score = score;
        int filled = scoreToDots(score);
        List<Region> dots = List.of(dot1, dot2, dot3, dot4);
        for (int i = 0; i < dots.size(); i++) {
            dots.get(i).getStyleClass().remove("intelligence-dot-filled");
            if (i < filled) dots.get(i).getStyleClass().add("intelligence-dot-filled");
        }
    }

    public void setRecommended(boolean recommended) {
        if (recommended) {
            if (!taskCardRoot.getStyleClass().contains("task-card-recommended")) {
                taskCardRoot.getStyleClass().add("task-card-recommended");
            }
        } else {
            taskCardRoot.getStyleClass().remove("task-card-recommended");
        }
    }

    private void setupDeadlineDisplay() {
        if (task.getDeadline() == null) {
            lblEstimatedTime.setText("No Date");
            lblEstimatedTime.getStyleClass().remove("task-date-overdue");
            lblDeadlineAlert.setVisible(false);
            lblDeadlineAlert.setManaged(false);
            return;
        }

        LocalDate due = task.getDeadline().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String formatted = due.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        boolean overdue = due.isBefore(LocalDate.now());

        lblEstimatedTime.setText(formatted);
        lblEstimatedTime.getStyleClass().remove("task-date-overdue");
        if (overdue) {
            lblEstimatedTime.getStyleClass().add("task-date-overdue");
        }
        lblDeadlineAlert.setVisible(overdue);
        lblDeadlineAlert.setManaged(overdue);
    }

    private void setupPriorityBadge() {
        lblPriorityBadge.getStyleClass().removeAll(
                "priority-urgente-pill", "priority-haute-pill", "priority-moyenne-pill", "priority-basse-pill"
        );
        PrioriteTache p = task.getPriorite();
        if (p == null) p = PrioriteTache.MOYENNE;
        switch (p) {
            case URGENTE -> {
                lblPriorityBadge.setText("Urgent");
                lblPriorityBadge.getStyleClass().add("priority-urgente-pill");
            }
            case HAUTE -> {
                lblPriorityBadge.setText("Haute");
                lblPriorityBadge.getStyleClass().add("priority-haute-pill");
            }
            case MOYENNE -> {
                lblPriorityBadge.setText("Moyenne");
                lblPriorityBadge.getStyleClass().add("priority-moyenne-pill");
            }
            case BASSE -> {
                lblPriorityBadge.setText("Basse");
                lblPriorityBadge.getStyleClass().add("priority-basse-pill");
            }
        }
    }

    private int scoreToDots(double score) {
        if (score >= 15) return 4;
        if (score >= 11) return 4;
        if (score >= 8) return 3;
        if (score >= 5) return 2;
        return 1;
    }

    private void setupTrackingBadge() {
        if (task == null) return;

        boolean isTracking = isTracking(task);

        trackingBadge.setVisible(isTracking);
        trackingBadge.setManaged(isTracking); // render only when tracking (no empty space)
        if (!isTracking) {
            stopPulse();
            stopTicker();
            return;
        }

        startPulse();
        startTicker();
        lblTrackingTime.setText(formatElapsedHHmm());
    }

    private boolean isTracking(Tache t) {
        if (t.getStartTime() == null) return false;
        if (t.getEndTime() != null) return false;
        // Only show in Kanban active columns (En Cours / Révision)
        return t.getStatut() != null && (t.getStatut().name().equals("EN_COURS") || t.getStatut().name().equals("EN_REVISION"));
    }

    private void startPulse() {
        if (dotPulse == null) {
            ScaleTransition scale = new ScaleTransition(javafx.util.Duration.millis(1400), trackingDot);
            scale.setFromX(1.0);
            scale.setFromY(1.0);
            scale.setToX(1.3);
            scale.setToY(1.3);
            scale.setAutoReverse(true);
            scale.setCycleCount(Timeline.INDEFINITE);
            scale.setInterpolator(Interpolator.EASE_BOTH);

            FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(1400), trackingDot);
            fade.setFromValue(0.6);
            fade.setToValue(1.0);
            fade.setAutoReverse(true);
            fade.setCycleCount(Timeline.INDEFINITE);
            fade.setInterpolator(Interpolator.EASE_BOTH);

            dotPulse = new ParallelTransition(scale, fade);
        }
        dotPulse.play();
    }

    private void stopPulse() {
        if (dotPulse != null) {
            dotPulse.stop();
            trackingDot.setScaleX(1.0);
            trackingDot.setScaleY(1.0);
            trackingDot.setOpacity(0.9);
        }
    }

    private void startTicker() {
        if (timeTicker == null) {
            timeTicker = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
                if (task != null && isTracking(task)) {
                    lblTrackingTime.setText(formatElapsedHHmm());
                } else {
                    stopTicker();
                }
            }));
            timeTicker.setCycleCount(Timeline.INDEFINITE);
        }
        timeTicker.play();
    }

    private void stopTicker() {
        if (timeTicker != null) {
            timeTicker.stop();
        }
    }

    private String formatElapsedHHmm() {
        if (task == null || task.getStartTime() == null) return "00:00";
        long startMs = task.getStartTime().getTime();
        long nowMs = System.currentTimeMillis();
        long elapsedSec = Math.max(0, (nowMs - startMs) / 1000L);
        Duration d = Duration.ofSeconds(elapsedSec);
        long hours = d.toHours();
        long minutes = (d.toMinutes() % 60);
        return String.format("%02d:%02d", hours, minutes);
    }

    @FXML
    private void handleCardClick() {
        boardController.showTaskDetails(task);
    }
}
