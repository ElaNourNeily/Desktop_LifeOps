package test;

public class Launcher {
    public static void main(String[] args) {
        // Run the JavaFX application from a class that doesn't extend Application
        // This bypasses the "JavaFX runtime components are missing" error
        MainFX.main(args);
    }
}
