-- Database initialization script for LifeOps Finance Management
-- Run this script to create the necessary tables

-- Create alert table
CREATE TABLE IF NOT EXISTS alert (
    id INT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    utilisateur_id INT NOT NULL,
    budget_id INT,
    depense_id INT,
    date_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,
    due_date TIMESTAMP NULL
);

-- Create reminder table
CREATE TABLE IF NOT EXISTS reminder (
    id INT PRIMARY KEY AUTO_INCREMENT,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    due_date TIMESTAMP NOT NULL,
    frequency VARCHAR(20) DEFAULT 'once',
    category VARCHAR(50) DEFAULT 'autre',
    amount DECIMAL(10,2) NOT NULL,
    utilisateur_id INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    date_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_notified TIMESTAMP NULL
);

-- Insert some sample data for testing
INSERT INTO alert (type, title, message, severity, utilisateur_id, date_created, is_read) VALUES
('info', 'Bienvenue', 'Bienvenue dans LifeOps Finance!', 'info', 1, NOW(), false);

INSERT INTO reminder (titre, description, due_date, frequency, category, amount, utilisateur_id) VALUES
('Facture STEG', 'Paiement électricité mensuel', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'monthly', 'facture', 45.50, 1),
('Abonnement Netflix', 'Renouvellement mensuel', DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'monthly', 'abonnement', 15.99, 1);