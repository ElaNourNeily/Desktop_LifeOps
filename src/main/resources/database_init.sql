-- Database initialization script for LifeOps Finance Management
-- Run this script to create the necessary tables

-- Create utilisateur table
CREATE TABLE IF NOT EXISTS utilisateur (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100),
    email VARCHAR(100),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create budget table
CREATE TABLE IF NOT EXISTS budget (
    id INT PRIMARY KEY AUTO_INCREMENT,
    revenu_mensuel DOUBLE NOT NULL,
    plafond DOUBLE NOT NULL,
    mois VARCHAR(50) NOT NULL,
    economies DOUBLE DEFAULT 0,
    utilisateur_id INT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

-- Create depense table
CREATE TABLE IF NOT EXISTS depense (
    id INT PRIMARY KEY AUTO_INCREMENT,
    titre VARCHAR(255) NOT NULL,
    montant DOUBLE NOT NULL,
    categorie VARCHAR(100),
    date_depense DATE NOT NULL,
    type_paiement VARCHAR(50),
    utilisateur_id INT NOT NULL,
    budget_id INT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    FOREIGN KEY (budget_id) REFERENCES budget(id) ON DELETE CASCADE
);

-- Insert default user
INSERT INTO utilisateur (id, nom, email) VALUES (1, 'Utilisateur Test', 'test@lifeops.com')
ON DUPLICATE KEY UPDATE nom=nom;

-- Insert sample budget
INSERT INTO budget (revenu_mensuel, plafond, mois, economies, utilisateur_id) VALUES
(5000.00, 4000.00, 'Mai 2026', 1000.00, 1);

-- Insert sample expenses
INSERT INTO depense (titre, montant, categorie, date_depense, type_paiement, utilisateur_id, budget_id) VALUES
('Courses Carrefour', 150.50, 'Alimentation', CURDATE(), 'Carte', 1, 1),
('Essence', 80.00, 'Transport', CURDATE(), 'Especes', 1, 1),
('Restaurant', 45.00, 'Alimentation', CURDATE(), 'Carte', 1, 1);
