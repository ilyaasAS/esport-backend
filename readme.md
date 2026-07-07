# Moteur métier de l'Esport Hub - Le Cerveau & l'Oracle

Le dépôt `esport-backend` est le noyau métier de Esport Hub.  
Il expose une API REST/SSE sécurisée, orchestre l'agent Oracle IA et garantit un comportement déterministe sur les flux sensibles.

## Socle technique

| Domaine | Technologie | Usage |
|---|---|---|
| Exécution Java | Java 21 | Exécution applicative |
| Cadre applicatif | Spring Boot 3.4 | API, injection, sécurité, observabilité |
| IA applicative | Spring AI | Appel d'outils et orchestration LLM |
| Persistance | Spring Data JPA / Hibernate | Accès et mappage des données |
| Base de données | MySQL 8 | Stockage transactionnel |
| Sécurité | Spring Security + JWT | Authentification sans état et RBAC |
| Construction | Maven + Docker multi-étapes | Compilation reproductible |

## Positionnement architectural

L'architecture suit une séparation claire des responsabilités:

- `oracle`: orchestration IA et politiques de conversation.
- `services`: logique métier de tournoi.
- `repositories` / `infrastructure.persistence`: ports métier et adaptateurs techniques.
- `web`: contrôleurs, DTO et gestion d'erreurs HTTP.
- `auth`: authentification, autorisation et sécurité JWT.

Cette conception soutient une trajectoire **Prêt pour la production**: testabilité, lisibilité, isolation des couches et maîtrise des risques.

## Oracle impérial: orchestrateur centralisé (Façade)

`OracleService` joue le rôle d'**Orchestrateur Centralisé** selon le patron **Façade**:

1. classification déterministe de l'intention utilisateur,
2. sélection stricte des outils autorisés selon le contexte et le rôle,
3. exécution Java prioritaire des branches critiques,
4. appel LLM encadré uniquement lorsque pertinent,
5. assainissement systématique de la réponse finale.

Cette stratégie évite de déléguer les décisions critiques au modèle et maintient la gouvernance côté code applicatif.

## Appel d'outils stratégiques: IA connectée à des fonctions Java réelles

L'agent Oracle n'invente pas de mutations métier: il appelle des outils Java typés exposés par `OracleToolsConfiguration`.

| Outil | Fonction métier |
|---|---|
| `getPlayersTool` | Lecture de la liste des joueurs |
| `getPlayersRankingTool` | Classement dynamique des joueurs |
| `getMatchesTool` | Lecture de l'historique des matchs |
| `getMatchesRankingTool` | Classement des matchs les plus intenses |
| `addPlayerTool` | Création d'un joueur |
| `createMatchTool` | Enregistrement d'un match |
| `predictMatchWinnerTool` | Pronostic déterministe de vainqueur |

## Innovation: module de pronostic mathématique déterministe (PowerScore)

Le moteur métier intègre un moteur de prédiction déterministe pour les pronostics:

- score de puissance calculé à partir des attributs joueurs (niveau + historique de score),
- comparaison normalisée des puissances pour dériver un favori,
- résultat explicable, reproductible et non aléatoire.

Ce module fournit un comportement stable et auditable, indépendant des fluctuations du modèle LLM.

## Sécurité critique (garde-fous déterministes)

Le système est **Sécurisé dès la conception**.  
Les cas sensibles sont interceptés en Java avant toute génération libre:

- blocage des tentatives d'injection d'invite et d'exfiltration technique,
- refus explicite des opérations non autorisées selon les rôles,
- traitement déterministe des situations vitales (orientation vers le **3114**),
- protection du "Secret Impérial" (non-divulgation d'informations internes sur le code source et l'architecture).

## Gestion des erreurs et standards

- Erreurs métier structurées via `BusinessException` et exceptions spécialisées.
- Mappage HTTP centralisé via `GlobalExceptionHandler` avec schéma d'erreur unifié.
- Règles de nommage technique standardisées en anglais pour les identifiants de code.
- Documentation technique en français pour les Javadocs et guides d'exploitation.

## Guide de démarrage rapide (Docker)

Depuis la racine de l'écosystème:

```bash
docker compose up -d --build
```

## Exécution locale du moteur métier (hors conteneur Docker)

Depuis `backend/`:

```bash
mvn spring-boot:run
```

## Tests

```bash
mvn test
```

Exemple de ciblage:

```bash
mvn -Dtest=OracleServiceSecurityTest test
```

## Variables d'environnement principales

| Variable | Rôle |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Connexion MySQL |
| `JWT_SECRET`, `JWT_EXPIRATION_MS` | Signature et durée de vie des tokens |
| `ADMIN_PASSWORD` | Initialisation du compte administrateur |
| `OLLAMA_BASE_URL`, `OLLAMA_MODEL` | Connexion au moteur IA local |

Le fichier `backend/.env.example` sert de référence de configuration.