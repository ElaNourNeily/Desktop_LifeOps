package utils;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class RememberMe {

    private static final String DIR_NAME = ".lifeops";
    private static final String FILE_NAME = "remember.dat";
    private static final int EXPIRY_DAYS = 30;


    private static Path getFilePath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, DIR_NAME, FILE_NAME);
    }


    public static void save(String email) {
        try {
            Path dir = getFilePath().getParent();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String expiry = LocalDateTime.now().plusDays(EXPIRY_DAYS)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Simple format: line 1 = email, line 2 = expiry date
            String content = email + "\n" + expiry;
            Files.writeString(getFilePath(), content);

            System.out.println("Remember me saved for: " + email);
        } catch (IOException e) {
            System.out.println("Failed to save remember me: " + e.getMessage());
        }
    }


    public static String load() {
        try {
            Path file = getFilePath();
            if (!Files.exists(file)) {
                return null;
            }

            java.util.List<String> lines = Files.readAllLines(file);
            if (lines.size() < 2) {
                clear();
                return null;
            }

            String email = lines.get(0).trim();
            String expiryStr = lines.get(1).trim();

            LocalDateTime expiry = LocalDateTime.parse(expiryStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            if (LocalDateTime.now().isAfter(expiry)) {
                // Token expired
                System.out.println("Remember me expired, clearing...");
                clear();
                return null;
            }

            return email;
        } catch (Exception e) {
            System.out.println("Failed to load remember me: " + e.getMessage());
            clear();
            return null;
        }
    }


    public static void clear() {
        try {
            Files.deleteIfExists(getFilePath());
            System.out.println("Remember me cleared");
        } catch (IOException e) {
            System.out.println("Failed to clear remember me: " + e.getMessage());
        }
    }
}
