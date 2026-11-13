# Règlement Officiel de la Ligue E-Sport — Esport Hub (Saison 2026)

## Préambule
Le présent règlement régit l'ensemble des compétitions, tournois officiels, matchs de ligue et classements organisés au sein de la plateforme **Esport Hub**.
Tout joueur inscrit, participant ou gestionnaire accepte sans réserve l'application des règles stipulées ci-après. Les décisions prises par les arbitres accrédités et l'Oracle impérial font foi en toute circonstance.

---

## Article 1 — Inscription, Éligibilité des Joueurs et Création de Profil

### 1.1 Conditions Générales d'Éligibilité
1. La participation aux tournois de la ligue est ouverte à toute personne physique titulaire d'un compte utilisateur valide et authentifié sur la plateforme Esport Hub.
2. L'âge minimal requis pour concourir dans les tournois de ligue officielle est fixé à 16 ans révolus au jour de l'inscription.
3. L'unicité du compte joueur est strictement obligatoire. Un joueur ne peut posséder qu'un seul pseudonyme actif dans la base de données. Tout joueur détecté avec des comptes multiples (smurfing) s'expose à la disqualification immédiate et à un bannissement définitif.

### 1.2 Attribution du Niveau et Score Initial
1. Lors de l'inscription via l'interface ou par l'intermédiaire de l'Oracle (commande d'ajout de joueur), le joueur doit renseigner un pseudonyme unique (alphanumérique, sans caractères illicites) et son niveau initial.
2. Si le niveau n'est pas spécifié lors de la création, le niveau par défaut attribué est de **1**.
3. Le score initial de base d'un nouveau joueur est fixé à **1000 points** (ou au barème équivalent indexé sur son niveau déclaré par un administrateur).

### 1.3 Validation et Rôles d'Accès
1. Les utilisateurs authentifiés disposant du rôle `USER` ou `ADMIN` sont légitimes pour créer leur profil joueur ou enregistrer de nouveaux compétiteurs.
2. La modification directe du statut d'un joueur, la suppression ou le bannissement sont des prérogatives exclusives des comptes pourvus du rôle `ADMIN`.

---

## Article 2 — Système de Cotation, Algorithme Elo et Prédictions

### 2.1 Modèle de Cotation Probabiliste (Pattern Strategy Elo)
1. Le classement et les pronostics de la ligue reposent sur une implémentation native du **modèle logistique de cotation Elo** (standard international Bradley-Terry), substituée à l'ancienne heuristique linéaire simplifiée.
2. La probabilité théorique $E_A$ de victoire du Joueur A face au Joueur B est calculée selon l'écart de force (rating basé sur le score et le niveau) :
   $$E_A = \frac{1}{1 + 10^{(Rating_B - Rating_A) / 400}}$$
3. Symétriquement, l'espérance de victoire du Joueur B s'établit à $E_B = 1.0 - E_A$.

### 2.2 Facteur d'Ajustement K et Historique Direct (Head-to-Head / H2H)
1. Le facteur de volatilité standard est fixé à **$K = 32$**, assurant une actualisation réactive des scores après chaque confrontation sans instabilité excessive.
2. Lors de la prédiction d'un match entre deux compétiteurs, le système inspecte l'historique complet des confrontations directes passées (Head-to-Head) :
   - Chaque victoire historique directe confère un bonus d'ascendant psychologique et tactique au joueur victorieux.
   - Le calcul pondéré combine la probabilité brute issue du rating avec le ratio de victoire direct historique.
3. **Bornage de Sécurité Probabiliste** :
   Afin d'éviter tout pronostic absolu ou réductionniste, la probabilité finale retournée est obligatoirement bornée dans l'intervalle strict **[1.0%, 99.0%]**. Aucun match n'est considéré comme gagné ou perdu d'avance à 100%.

---

## Article 3 — Conditions des Matchs, Formats et Enregistrement des Scores

### 3.1 Formats de Compétition Officiels
1. **Match Standard / Phase de Poules** : Les matchs préliminaires se disputent au format *Best of 1* (BO1) ou *Best of 3* (BO3) selon le calendrier établi par la direction du tournoi.
2. **Phases Finales (Playoffs)** : Les demi-finales se déroulent obligatoirement au format *Best of 3* (BO3) et la Grande Finale au format *Best of 5* (BO5).
3. Tout match disputé doit comporter au moins un round ou point gagnant par joueur, sans possibilité de match nul (un vainqueur distinct est requis).

### 3.2 Saisie et Enregistrement Officiel des Résultats
1. La création et l'enregistrement officiel d'un match (avec attribution des scores finaux) sont **strictement réservés aux administrateurs accrédités (rôle ADMIN)**.
2. Toute tentative d'enregistrement de match émanant d'un rôle `USER` non autorisé est rejetée avec refus formel de l'Oracle ("Je n'ai pas l'autorité nécessaire pour cette opération").
3. Lors de la saisie d'un résultat (ex: "Match Sarah 2 contre Carla 0"), le système vérifie au préalable l'existence des deux joueurs en base de données avant toute insertion. Si un joueur est manquant, l'enregistrement est suspendu jusqu'à son inscription effective.

### 3.3 Critères de Départage (Tie-Break)
En cas d'égalité de points au classement général à l'issue de la saison régulière, les critères de départage successifs sont :
1. Résultat de la confrontation directe (Head-to-Head) entre les joueurs concernés.
2. Différence de score cumulée (rounds marqués moins rounds concédés).
3. Score de force Elo actualisé.
4. Match d'appui décisif au format BO1 en cas d'égalité persistante.

---

## Article 4 — Arbitrage, Gestion des Litiges et Réclamations

### 4.1 Souveraineté de l'Arbitrage
1. Le corps arbitral de la ligue, appuyé par les journaux d'audit de l'Oracle et la traçabilité Langfuse, dispose d'une autorité souveraine pour l'interprétation des règles en temps réel.
2. Les décisions arbitrales sont sans appel dès lors qu'elles sont confirmées par la commission d'organisation.

### 4.2 Procédure de Réclamation
1. Toute contestation portant sur le déroulement d'une manche ou la validité d'un score doit être déposée dans un délai strict de **15 minutes** suivant la fin de la rencontre.
2. La réclamation doit obligatoirement être accompagnée d'une preuve tangible : capture d'écran horodatée du tableau des scores de fin de partie, enregistrement vidéo (VOD) ou identifiant de match serveur.
3. Passé le délai de 15 minutes, le résultat enregistré dans la base Esport Hub devient définitif, inaltérable et opposable aux deux parties.

---

## Article 5 — Déconnexions, Pannes Matérielles et Forfaits

### 5.1 Déconnexion Précoce (Seuil des 3 Minutes / Remake)
1. Si un joueur subit une déconnexion involontaire ou un crash client avant 3 minutes de jeu effectif (ou avant la survenue du premier round / élimination), le match est immédiatement interrompu.
2. Un nouveau match complet (*Remake*) est relancé sans pénalité pour les deux participants.

### 5.2 Déconnexion en Cours de Partie (Pause Technique)
1. Si un incident technique intervient après le seuil initial des 3 minutes, le match est mis en pause technique.
2. Un délai maximal de reconnexion de **5 minutes** est octroyé au joueur déconnecté pour réintégrer la session.
3. Si le joueur ne parvient pas à se reconnecter à l'issue des 5 minutes de pause :
   - La manche en cours est adjugée par forfait à son adversaire.
   - Si le format de la rencontre est un BO3 ou BO5 et que le joueur ne peut reprendre la partie, la victoire globale est déclarée par forfait technique au bénéfice de l'adversaire présent.
   - Aucun dédommagement en points Elo ne pourra être réclamé pour cause de défaillance de la connexion Internet personnelle du participant.

### 5.3 Panne d'Infrastructure Majeure
En cas de défaillance générale avérée des serveurs de jeu ou de la plateforme Esport Hub, la manche en cours est déclarée nulle et non avenue (*Null and Void*). La rencontre est reprogrammée ultérieurement sans incidence sur le barème Elo.

---

## Article 6 — Code de Conduite, Éthique et Sanctions Disciplinaires

### 6.1 Intégrité Sportive et Anti-Triche
1. L'utilisation de tout logiciel tiers, script d'automatisation, exploit de bug non résolu, wallhack, aimbot ou modification de client conférant un avantage indu est formellement prohibée.
2. Le trucage de match (*match-fixing*), l'accord préalable entre adversaires sur l'issue d'une partie (*win-trading*) ou le refus délibéré de concourir entraînent l'exclusion immédiate de la ligue.

### 6.2 Comportement et Respect d'Autrui
1. Tout participant s'engage à faire preuve de courtoisie, de dignité et de fair-play envers ses adversaires, les organisateurs, l'Oracle et les spectateurs.
2. Sont formellement interdits et réprimés avec la plus grande sévérité :
   - Les insultes, invectives, provocations dégradantes et le harcèlement en ligne.
   - Tout propos à caractère raciste, sexiste, homophobe, xénophobe ou discriminatoire.
   - Les tentatives d'ingénierie sociale, de détournement du prompt de l'Oracle (*jailbreak / prompt injection*) ou de déstabilisation de l'infrastructure.

### 6.3 Échelle des Sanctions Disciplinaires
Toute infraction constatée entraîne l'application proportionnée des sanctions suivantes :
1. **Niveau 1 (Avertissement formel)** : Première infraction mineure (retard léger, comportement non coopératif).
2. **Niveau 2 (Forfait de match)** : Défaite technique 0-2 ou 0-3 infligée sur le match concerné avec actualisation négative du score Elo.
3. **Niveau 3 (Pénalité de points Elo)** : Retrait forfaitaire de **100 à 250 points Elo** sur le classement général de la ligue.
4. **Niveau 4 (Suspension temporaire)** : Interdiction de participation à tous les tournois de la plateforme pendant une durée de 30 à 180 jours.
5. **Niveau 5 (Bannissement définitif)** : Radiation irrévocable du compte, suppression des trophées acquis et inscription sur la liste noire de la ligue.
