package service;

import model.Depense;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    private final DepenseService depenseService = new DepenseService();
    private final SmsService smsService = new SmsService();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "NotificationSchedulerThread");
        t.setDaemon(true);
        return t;
    });

    public void start() {
        // Run once at start, then every hour
        scheduler.scheduleAtFixedRate(this::checkAndSendNotifications, 0, 1, TimeUnit.HOURS);
        System.out.println("NotificationScheduler started.");
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void checkAndSendNotifications() {
        try {
            List<Depense> allDepenses = depenseService.recuperer();
            LocalDate today = LocalDate.now();

            for (Depense d : allDepenses) {
                if (d.isImportant() && !d.isSmsSent() && d.getDate() != null) {
                    LocalDate expenseDate = new java.sql.Date(d.getDate().getTime()).toLocalDate();
                    
                    // If date is today or has passed (in case app wasn't running)
                    if (expenseDate.isBefore(today) || expenseDate.isEqual(today)) {
                        sendNotification(d);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking notifications: " + e.getMessage());
        }
    }

    private void sendNotification(Depense d) {
        String msg = "Alerte LifeOps: Rappel pour votre dépense importante '" + d.getTitre() + 
                     "' de " + d.getMontant() + " TND prévue pour le " + d.getDate() + ".";
        
        System.out.println("Sending scheduled SMS for Depense ID: " + d.getId());
        smsService.sendSms(d.getPhoneNumber(), msg);
        
        d.setSmsSent(true);
        try {
            depenseService.modifier(d);
        } catch (SQLException e) {
            System.err.println("Failed to update sms_sent status for Depense ID " + d.getId() + ": " + e.getMessage());
        }
    }
}
