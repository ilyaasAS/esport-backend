# Gestionnaire de Tournoi e-Sport

## Présentation
Ce projet est une application Java développée pour gérer des tournois de sport. Elle permet de piloter les joueurs, d'enregistrer les scores des matchs et de suivre les statistiques globales du tournoi.

## Fonctionnalités
L'application propose un menu interactif complet :
- **Gestion des joueurs** : Inscription avec pseudo et niveau.
- **Organisation des matchs** : Enregistrement des rencontres et des scores.
- **Statistiques** : Affichage du classement (Top 3) et du score total du tournoi.
- **Historique** : Consultation de tous les matchs joués.

## Architecture du projet
Le code est structuré de manière modulaire pour être robuste et évolutif :
- **Models** : Classes Player et Match, avec utilisation de l'interface Scorable pour les calculs.
- **Services** : Logique métier utilisant les Java Streams pour le traitement des données.
- **DAO** : Gestion de la persistance des données au format CSV (chargement et sauvegarde automatique).
- **Exceptions** : Gestion personnalisée des erreurs (doublons, joueurs introuvables, matchs invalides).

## Qualité et Tests
- **Logs** : Suivi des activités de l'application (niveaux INFO, WARN et ERROR).
- **Tests Unitaires** : Validation du bon fonctionnement des fonctionnalités critiques avec JUnit 5.

## Installation et Lancement
1. Importer le projet dans un IDE (IntelliJ, Eclipse, etc.) via Maven.
2. Vérifier la présence du dossier `data/` à la racine pour le stockage des fichiers CSV.
3. Lancer la classe `Main.java` située dans le package `org.example`.