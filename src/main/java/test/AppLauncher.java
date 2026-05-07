package test;

/**
 * Launcher class used as the main entry point.
 * This class exists to work around the JavaFX module system restriction
 * that prevents launching Application subclasses directly.
 */
public class AppLauncher {
    public static void main(String[] args) {
        // MainApp is in the default package — call via reflection to avoid
        // compile-time dependency on the default package from a named package.
        try {
            Class<?> mainClass = Class.forName("MainApp");
            mainClass.getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
