# LifeOps - Application de Gestion du Temps

Application JavaFX de gestion du temps et des activités avec intelligence artificielle.

## Prérequis

- **Java 17** ou supérieur
- **Maven 3.6+**
- **MySQL 8.0+** (serveur de base de données)
- **Connexion Internet** (pour les services externes: météo, actualités, IA)

## Configuration de la Base de Données

1. Démarrez votre serveur MySQL
2. Exécutez le script SQL fourni:
   ```bash
   mysql -u root -p < schema.sql
   ```
   
   Ou manuellement:
   - Créez la base de données `life_ops_symfony`
   - Exécutez les commandes SQL du fichier `schema.sql`

3. Vérifiez la configuration de connexion dans `MyDatabase.java`:
   - URL: `jdbc:mysql://localhost:3306/life_ops_symfony`
   - Utilisateur: `root`
   - Mot de passe: `` (vide par défaut)

## Compilation et Exécution

### Avec Maven (Recommandé)

```bash
# Compiler le projet
mvn clean compile

# Exécuter l'application
mvn javafx:run
```

### Avec Maven (Package JAR)

```bash
# Créer le JAR
mvn clean package

# Exécuter le JAR
java -jar target/Workshop1-1.0-SNAPSHOT.jar
```

## Structure du Projet

```
Desktop_LifeOps/
├── src/main/java/
│   ├── Controller/Time/          # Contrôleurs JavaFX
│   ├── model/Time/                # Modèles de données
│   ├── service/Time/              # Services métier
│   │   └── external/              # Services externes (météo, news)
│   ├── utils/                     # Utilitaires (connexion DB)
│   ├── test/                      # Lanceur d'application
│   └── MainApp.java               # Point d'entrée principal
├── src/main/resources/Time/       # Fichiers FXML et CSS
├── schema.sql                     # Script de création de la BDD
└── pom.xml                        # Configuration Maven
```

## Fonctionnalités

### ✅ Gestion des Activités
- Création, modification, suppression d'activités
- Catégorisation et priorisation
- Gestion des horaires et durées
- Activités récurrentes (quotidiennes, hebdomadaires, mensuelles)
- Rappels personnalisables

### 📅 Planification
- Vue calendrier (jour/semaine)
- Plannings personnalisés par jour
- Détection automatique des conflits d'horaires
- Filtrage par priorité et statut

### 🤖 Intelligence Artificielle
- Optimisation automatique du planning via Gemini AI
- Suggestions d'activités intelligentes
- Analyse de productivité

### 🌤️ Intégrations Externes
- **Météo**: Prévisions via Open-Meteo API
- **Actualités**: Conseils contextuels via NewsAPI
- **Notifications**: Rappels automatiques

### 📊 Statistiques
- Taux de complétion des tâches
- Temps de focus hebdomadaire
- Distribution des priorités
- Précision de planification

### 🎯 Session Focus (Pomodoro)
- Timer de concentration
- Gestion des pauses
- Suivi du temps réel

## Configuration des API Externes

### API Gemini (IA)
Modifiez la clé API dans `AIService.java`:
```java
private final service.Time.AIService aiService = new service.Time.AIService("VOTRE_CLE_API");
```
Obtenez une clé gratuite sur: https://makersuite.google.com/app/apikey

### API NewsAPI (Actualités)
Modifiez la clé API dans `NewsService.java`:
```java
private static final String API_KEY = "VOTRE_CLE_API";
```
Obtenez une clé gratuite sur: https://newsapi.org/

### API Open-Meteo (Météo)
Aucune clé requise - service gratuit et ouvert.

## Dépannage

### Erreur de connexion MySQL
- Vérifiez que MySQL est démarré
- Vérifiez les identifiants dans `MyDatabase.java`
- Assurez-vous que la base de données `life_ops_symfony` existe

### Erreur "FXML not found"
- Vérifiez que les fichiers FXML sont dans `src/main/resources/Time/`
- Recompilez avec `mvn clean compile`

### Erreur JavaFX
- Assurez-vous d'utiliser Java 17
- Vérifiez que les dépendances JavaFX sont bien dans le `pom.xml`

### Erreur de compilation
```bash
# Nettoyer et recompiler
mvn clean install -U
```

## Technologies Utilisées

- **JavaFX 17.0.2** - Interface graphique
- **MySQL 8.0** - Base de données
- **Maven** - Gestion des dépendances
- **Gson 2.10.1** - Parsing JSON
- **OpenPDF 1.3.30** - Export PDF
- **Gemini AI** - Intelligence artificielle
- **Open-Meteo API** - Données météorologiques
- **NewsAPI** - Actualités contextuelles

## Auteurs

Projet développé dans le cadre d'un workshop de gestion du temps et de productivité.

## Licence

Ce projet est à usage éducatif.
