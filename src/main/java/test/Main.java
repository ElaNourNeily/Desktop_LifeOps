package test;

import enums.PrioriteTache;
import enums.StatutTache;
import enums.StatutTaskSpace;
import enums.TypeTaskSpace;
import model.Budget;
import model.Depense;
import model.Tache;
import model.TaskSpace;
import service.BudgetService;
import service.DepenseService;
import service.TacheService;
import service.TaskSpaceService;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        BudgetService budgetService = new BudgetService();
        DepenseService depenseService = new DepenseService();
        TaskSpaceService tsService = new TaskSpaceService();
        TacheService tacheService = new TacheService();

        System.out.println("\nBudgets existants :");
        try {
            for (Budget budget : budgetService.recuperer()) {
                System.out.println("  " + budget);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("\nDepenses existantes :");
        try {
            for (Depense depense : depenseService.recuperer()) {
                System.out.println("  " + depense);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("\nTaskSpaces existants :");
        try {
            for (TaskSpace ts : tsService.recuperer()) {
                System.out.println("  " + ts);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("\nTaches existantes :");
        try {
            for (Tache tache : tacheService.recuperer()) {
                System.out.println("  " + tache);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("\nAJOUTER Budget");
        Budget budget = new Budget(4500, 3000, "Avril 2026", 1200, 1);
        try {
            budgetService.ajouter(budget);
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }

        int nouveauBudgetId = 0;
        try {
            List<Budget> budgets = budgetService.recuperer();
            System.out.println("\nTous les budgets :");
            for (Budget currentBudget : budgets) {
                System.out.println("  " + currentBudget);
            }
            if (!budgets.isEmpty()) {
                nouveauBudgetId = budgets.get(budgets.size() - 1).getId();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        if (nouveauBudgetId != 0) {
            System.out.println("\nMODIFIER Budget id=" + nouveauBudgetId);
            Budget budgetModifie = new Budget(nouveauBudgetId, 5000, 3200, "Mai 2026", 1500, 1);
            try {
                budgetService.modifier(budgetModifie);
            } catch (SQLException e) {
                System.out.println("Erreur : " + e.getMessage());
            }

            System.out.println("\nAJOUTER Depense");
            Depense depense = new Depense(
                    "Courses",
                    180.5,
                    "Alimentation",
                    new Date(),
                    "Carte",
                    1,
                    nouveauBudgetId
            );
            try {
                depenseService.ajouter(depense);
            } catch (SQLException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }

        int nouvelleDepenseId = 0;
        try {
            List<Depense> depenses = depenseService.recuperer();
            System.out.println("\nToutes les depenses :");
            for (Depense currentDepense : depenses) {
                System.out.println("  " + currentDepense);
            }
            if (!depenses.isEmpty()) {
                nouvelleDepenseId = depenses.get(depenses.size() - 1).getId();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        if (nouveauBudgetId != 0 && nouvelleDepenseId != 0) {
            System.out.println("\nMODIFIER Depense id=" + nouvelleDepenseId);
            Depense depenseModifiee = new Depense(
                    nouvelleDepenseId,
                    "Courses du mois",
                    220.0,
                    "Alimentation",
                    new Date(),
                    "Especes",
                    1,
                    nouveauBudgetId
            );
            try {
                depenseService.modifier(depenseModifiee);
            } catch (SQLException e) {
                System.out.println("Erreur : " + e.getMessage());
            }

            System.out.println("\nDepenses du budget id=" + nouveauBudgetId + " :");
            try {
                for (Depense depense : depenseService.recupererParBudget(nouveauBudgetId)) {
                    System.out.println("  " + depense);
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        System.out.println("\nAJOUTER TaskSpace");
        TaskSpace ts = new TaskSpace(
                "Projet Java Desktop",
                TypeTaskSpace.DEVELOPPEMENT,
                new Date(),
                "Application Java avec CRUD",
                21,
                StatutTaskSpace.ACTIF,
                1
        );
        try {
            tsService.ajouter(ts);
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }

        int nouveauTsId = 0;
        try {
            List<TaskSpace> liste = tsService.recuperer();
            System.out.println("\nTous les TaskSpaces :");
            for (TaskSpace taskSpace : liste) {
                System.out.println("  " + taskSpace);
            }
            if (!liste.isEmpty()) {
                nouveauTsId = liste.get(liste.size() - 1).getId();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("\nTaskSpaces de l'utilisateur id=1 :");
        try {
            for (TaskSpace taskSpace : tsService.recupererParUtilisateur(1)) {
                System.out.println("  " + taskSpace);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        if (nouveauTsId != 0) {
            System.out.println("\nMODIFIER TaskSpace id=" + nouveauTsId);
            TaskSpace tsModif = new TaskSpace(
                    nouveauTsId,
                    "Projet Java Modifie",
                    TypeTaskSpace.RECHERCHE,
                    new Date(),
                    "Description mise a jour",
                    30,
                    StatutTaskSpace.EN_PAUSE,
                    1
            );
            try {
                tsService.modifier(tsModif);
            } catch (SQLException e) {
                System.out.println("Erreur : " + e.getMessage());
            }

            System.out.println("\nAJOUTER Tache dans projet");
            Tache tacheProjet = new Tache(
                    "Implementer le login",
                    "Faire l'ecran de connexion JavaFX",
                    PrioriteTache.HAUTE,
                    4,
                    StatutTache.A_FAIRE,
                    new Date(),
                    nouveauTsId,
                    1
            );
            try {
                tacheService.ajouter(tacheProjet);
            } catch (SQLException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }

        System.out.println("\nAJOUTER Tache SOLO");
        Tache tacheSolo = new Tache(
                "Lire la documentation Java",
                "Reviser les streams et lambdas",
                PrioriteTache.MOYENNE,
                2,
                StatutTache.EN_COURS,
                null,
                0,
                1
        );
        try {
            tacheService.ajouter(tacheSolo);
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }

        List<Tache> toutesLesTaches = null;
        try {
            toutesLesTaches = tacheService.recuperer();
            System.out.println("\nToutes les taches :");
            for (Tache tache : toutesLesTaches) {
                System.out.println("  " + tache);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        if (nouveauTsId != 0) {
            System.out.println("\nTaches du TaskSpace id=" + nouveauTsId + " :");
            try {
                for (Tache tache : tacheService.recupererParTaskSpace(nouveauTsId)) {
                    System.out.println("  " + tache);
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        System.out.println("\nTaches solo de l'utilisateur id=1 :");
        try {
            for (Tache tache : tacheService.recupererTachesSolo(1)) {
                System.out.println("  " + tache);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        int dernierTacheId = 0;
        if (toutesLesTaches != null && !toutesLesTaches.isEmpty()) {
            dernierTacheId = toutesLesTaches.get(toutesLesTaches.size() - 1).getId();
        }

        if (dernierTacheId != 0) {
            System.out.println("\nMODIFIER Tache id=" + dernierTacheId);
            Tache tacheModif = new Tache(
                    dernierTacheId,
                    "Titre modifie",
                    "Description modifiee",
                    PrioriteTache.BASSE,
                    1,
                    StatutTache.TERMINE,
                    null,
                    0,
                    1
            );
            try {
                tacheService.modifier(tacheModif);
            } catch (SQLException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }

        if (dernierTacheId != 0) {
            System.out.println("\nSUPPRIMER Tache id=" + dernierTacheId);
            try {
                tacheService.supprimer(dernierTacheId);
            } catch (SQLException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }

        if (nouveauTsId != 0) {
            System.out.println("\nSUPPRIMER TaskSpace id=" + nouveauTsId);
            try {
                for (Tache tache : tacheService.recupererParTaskSpace(nouveauTsId)) {
                    tacheService.supprimer(tache.getId());
                }
                tsService.supprimer(nouveauTsId);
            } catch (SQLException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }

        if (nouvelleDepenseId != 0) {
            System.out.println("\nSUPPRIMER Depense id=" + nouvelleDepenseId);
            try {
                depenseService.supprimer(nouvelleDepenseId);
            } catch (SQLException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }

        if (nouveauBudgetId != 0) {
            System.out.println("\nSUPPRIMER Budget id=" + nouveauBudgetId);
            try {
                budgetService.supprimer(nouveauBudgetId);
            } catch (SQLException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }

        try {
            System.out.println("\nBudgets :");
            for (Budget currentBudget : budgetService.recuperer()) {
                System.out.println("  " + currentBudget);
            }
            System.out.println("\nDepenses :");
            for (Depense currentDepense : depenseService.recuperer()) {
                System.out.println("  " + currentDepense);
            }
            System.out.println("\nTaskSpaces :");
            for (TaskSpace taskSpace : tsService.recuperer()) {
                System.out.println("  " + taskSpace);
            }
            System.out.println("\nTaches :");
            for (Tache tache : tacheService.recuperer()) {
                System.out.println("  " + tache);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("\nTRI TACHES (Priorite + Difficulte)");
        for (Tache tache : tacheService.trier()) {
            System.out.println("  " + tache);
        }

        System.out.println("\nRECHERCHE TACHE (login)");
        for (Tache tache : tacheService.rechercher("login")) {
            System.out.println("  " + tache);
        }

        System.out.println("\nRECHERCHE TASKSPACE (Developpement)");
        for (TaskSpace taskSpace : tsService.rechercher("Developpement")) {
            System.out.println("  " + taskSpace);
        }

        System.out.println("\nRECHERCHE BUDGET (mai)");
        for (Budget currentBudget : budgetService.rechercher("mai")) {
            System.out.println("  " + currentBudget);
        }

        System.out.println("\nTRI DEPENSES (Montant)");
        for (Depense currentDepense : depenseService.trier()) {
            System.out.println("  " + currentDepense);
        }
    }
}
