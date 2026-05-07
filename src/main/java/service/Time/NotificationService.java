package service.Time;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import model.Time.Activite;
import model.Time.Planning;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationService {
    private static NotificationService instance;
    private final ActiviteService activiteService = new ActiviteService();
    private final PlanningService planningService = new PlanningService();
    private final Set<Integer> notifiedActivityIds = new HashSet<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private NotificationService() {}

    public static NotificationService getInstance() {
        if (instance == null) instance = new NotificationService();
        return instance;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkReminders, 0, 1, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void checkReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            java.sql.Date today = java.sql.Date.valueOf(now.toLocalDate());
            
            // 1. Get all activities for today
            // Note: We might need a better way to filter, but for now we get all and filter in memory
            List<Activite> all = activiteService.recuperer();
            
            for (Activite a : all) {
                if (a.getMinutesRappel() <= 0 || notifiedActivityIds.contains(a.getId())) continue;

                // We need to know the date of the activity. 
                // Since our model only stores planning_id, we need to fetch the planning date.
                Planning p = planningService.recupererParId(a.getPlanningId());
                if (p == null || !p.getDate().toLocalDate().equals(now.toLocalDate())) continue;

                LocalDateTime startTime = LocalDateTime.of(p.getDate().toLocalDate(), a.getHeureDebutEstimee().toLocalTime());
                LocalDateTime notificationTime = startTime.minusMinutes(a.getMinutesRappel());

                // If now is equal or after notification time, and not past start time too far
                if (now.isAfter(notificationTime) && now.isBefore(startTime)) {
                    showNotification(a);
                    notifiedActivityIds.add(a.getId());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showNotification(Activite a) {
        // Play Melody in background
        new Thread(() -> {
            playTone(1000, 150); // Ding
            try { Thread.sleep(50); } catch (InterruptedException e) {}
            playTone(1200, 300); // Dong!
        }).start();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Rappel LifeOps");
            alert.setHeaderText("📢 Il est l'heure !");
            alert.setContentText("Votre activité \"" + a.getTitre() + "\" commence dans " + a.getMinutesRappel() + " minutes.\n\n" +
                               "Heure : " + a.getHeureDebutEstimee().toLocalTime().toString());
            alert.show();
        });
    }

    private void playTone(int hz, int msecs) {
        try {
            byte[] buf = new byte[msecs * 8];
            for (int i = 0; i < buf.length; i++) {
                double angle = i / (8000.0 / hz) * 2.0 * Math.PI;
                buf[i] = (byte) (Math.sin(angle) * 127.0);
            }
            javax.sound.sampled.AudioFormat af = new javax.sound.sampled.AudioFormat(8000f, 8, 1, true, false);
            javax.sound.sampled.SourceDataLine sdl = javax.sound.sampled.AudioSystem.getSourceDataLine(af);
            sdl.open(af);
            sdl.start();
            sdl.write(buf, 0, buf.length);
            sdl.drain();
            sdl.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
