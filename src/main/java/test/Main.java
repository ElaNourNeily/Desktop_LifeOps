package test;

import model.BilanSante;
import model.SuiviSante;
import service.BilanSanteService;
import service.SuiviSanteService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        SuiviSanteService suiviService = new SuiviSanteService();
        BilanSanteService bilanService = new BilanSanteService();

        System.out.println("=====================================");
        System.out.println("   TEST GESTION DE SANTÉ (CRUD)   ");
        System.out.println("=====================================\n");

        // --------------- SUIVI SANTE ---------------
        System.out.println("📌 AJOUTER SuiviSante");
        SuiviSante suivi = new SuiviSante(
                0, // ID généré par DB
                LocalDate.now(),
                7.5f,
                4,
                8,
                45,
                70.5f,
                4,
                "Excellente journée",
                "Course à pied"
        );
        try {
            suiviService.ajouter(suivi);
            System.out.println("✅ Suivi ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        System.out.println("\n📋 LIRE SuivisSante existants :");
        int dernierSuiviId = 0;
        try {
            List<SuiviSante> suivis = suiviService.recuperer();
            for (SuiviSante s : suivis) {
                System.out.println("  " + s);
                dernierSuiviId = s.getId();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("\n📌 MODIFIER SuiviSante id=" + dernierSuiviId);
        if (dernierSuiviId != 0) {
            SuiviSante suiviModif = new SuiviSante(
                    dernierSuiviId,
                    LocalDate.now(),
                    8.0f,
                    5,
                    10,
                    60,
                    70.0f,
                    5,
                    "Très bonne journée",
                    "Natation"
            );
            try {
                suiviService.modifier(suiviModif);
                System.out.println("✅ Suivi modifié avec succès !");
            } catch (SQLException e) {
                System.out.println("Erreur: " + e.getMessage());
            }
        }

        // --------------- BILAN SANTE ---------------
        System.out.println("\n📌 AJOUTER BilanSante");
        BilanSante bilan = new BilanSante(
                0,
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                2,
                3,
                8.5f,
                false,
                "Continuer l'entraînement régulier"
        );
        try {
            bilanService.ajouter(bilan);
            System.out.println("✅ Bilan ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        System.out.println("\n📋 LIRE BilansSante existants :");
        int dernierBilanId = 0;
        try {
            List<BilanSante> bilans = bilanService.recuperer();
            for (BilanSante b : bilans) {
                System.out.println("  " + b);
                dernierBilanId = b.getId();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --------------- SUPPRESSION ---------------
        System.out.println("\n📌 SUPPRIMER SuiviSante id=" + dernierSuiviId);
        try {
            if (dernierSuiviId != 0) {
                suiviService.supprimer(dernierSuiviId);
                System.out.println("✅ Suivi supprimé !");
            }
        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        System.out.println("\n📌 SUPPRIMER BilanSante id=" + dernierBilanId);
        try {
            if (dernierBilanId != 0) {
                bilanService.supprimer(dernierBilanId);
                System.out.println("✅ Bilan supprimé !");
            }
        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        // --------------- FINAL VERIFICATION ---------------
        System.out.println("\n📂 ÉTAT FINAL DES DONNÉES :");
        try {
            System.out.println(">> Suivis : " + suiviService.recuperer().size());
            System.out.println(">> Bilans : " + bilanService.recuperer().size());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }
}