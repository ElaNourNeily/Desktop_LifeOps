package test;

import Service.ObjectifService;
import Utilis.MyDatabase;

public class Main {
    public static void main(String[] args) {
        // By calling MainFX.main from a class that doesn't extend Application,
        // we bypass the strict JavaFX module requirements.
        ObjectifService objectifService = new ObjectifService();
        MainFX.main(args);

    }
}