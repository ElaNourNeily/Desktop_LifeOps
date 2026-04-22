-- Create Database
CREATE DATABASE IF NOT EXISTS life_ops_symfony;
USE life_ops_symfony;


CREATE TABLE IF NOT EXISTS utilisateur (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(180) NOT NULL UNIQUE,
    roles JSON NOT NULL,
    password VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

-- Insert a default user for testing if id 1 doesn't exist
INSERT IGNORE INTO utilisateur (id, email, roles, password) 
VALUES (1, 'admin@lifeops.tn', '["ROLE_USER"]', 'password123');

-- Create Planning table
CREATE TABLE IF NOT EXISTS planning (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    disponibilite BOOLEAN DEFAULT TRUE,
    heure_debut_journee TIME NOT NULL,
    heure_fin_journee TIME NOT NULL,
    utilisateur_id INT NOT NULL,
    CONSTRAINT fk_planning_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    UNIQUE KEY unique_planning_date_user (date, utilisateur_id)
) ENGINE=InnoDB;

-- Create Activite table
CREATE TABLE IF NOT EXISTS activite (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    duree INT NOT NULL,
    priorite INT NOT NULL,
    etat VARCHAR(255) DEFAULT 'en_attente',
    heure_debut_estimee TIME NOT NULL,
    heure_fin_estimee TIME NOT NULL,
    niveau_urgence VARCHAR(255) DEFAULT 'moyen',
    categorie VARCHAR(50) NOT NULL,
    couleur VARCHAR(7) NOT NULL,
    suggested_by_ai BOOLEAN DEFAULT FALSE,
    planning_id INT NOT NULL,
    CONSTRAINT fk_activite_planning FOREIGN KEY (planning_id) REFERENCES planning(id) ON DELETE CASCADE
) ENGINE=InnoDB;
