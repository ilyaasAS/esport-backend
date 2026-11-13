package org.example.oracle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Service d'ingestion ETL (Extract-Transform-Load) pour les règlements officiels de la ligue e-sport.
 * Découpe le corpus markdown en chunks sémantiques via TokenTextSplitter et les indexe
 * dans le VectorStore ChromaDB au démarrage si la collection est vide.
 */
@Component
public class TournamentRuleLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TournamentRuleLoader.class);

    private final VectorStore vectorStore;
    private final ChromaApi chromaApi;
    private final String collectionName;
    private final Resource rulebookResource;

    /**
     * Initialise le composant de chargement avec les dépendances vectorielles requises.
     *
     * @param vectorStore magasin vectoriel ChromaDB autoconfiguré
     * @param chromaApi client API natif ChromaDB (optionnel pour les sondes directes)
     * @param collectionName nom de la collection vectorielle ciblée
     * @param rulebookResource ressource classpath contenant le corpus de règles markdown
     */
    public TournamentRuleLoader(
            VectorStore vectorStore,
            @Autowired(required = false) ChromaApi chromaApi,
            @Value("${spring.ai.vectorstore.chroma.collection-name:esport-rules}") String collectionName,
            @Value("classpath:documents/esport-league-rulebook.md") Resource rulebookResource
    ) {
        this.vectorStore = vectorStore;
        this.chromaApi = chromaApi;
        this.collectionName = collectionName;
        this.rulebookResource = rulebookResource;
    }

    @Override
    public void run(String... args) {
        try {
            loadRulesIfEmpty();
        } catch (Exception ex) {
            log.error("Erreur critique lors de l'ingestion des règles dans ChromaDB : {}", ex.getMessage(), ex);
        }
    }

    /**
     * Vérifie la vacuité de la base vectorielle et orchestre le pipeline ETL d'ingestion.
     */
    public void loadRulesIfEmpty() {
        if (!rulebookResource.exists()) {
            log.warn("Corpus de règlement introuvable dans les ressources : {}", rulebookResource.getDescription());
            return;
        }

        if (!isCollectionEmpty()) {
            log.info("La collection ChromaDB '{}' contient déjà des documents. Ingestion ETL ignorée.", collectionName);
            return;
        }

        try {
            String rawMarkdown = rulebookResource.getContentAsString(StandardCharsets.UTF_8);
            if (rawMarkdown.isBlank()) {
                log.warn("Le fichier de règlement '{}' est vide. Aucune indexation effectuée.", rulebookResource.getFilename());
                return;
            }

            // 1. Encapsulation dans un Document Spring AI avec métadonnées d'audit
            Map<String, Object> metadata = Map.of(
                    "source", "esport-league-rulebook.md",
                    "category", "tournament_rules",
                    "title", "Règlement Officiel de la Ligue E-Sport Esport Hub"
            );
            Document rootDocument = new Document(rawMarkdown, metadata);

            // 2. Découpage sémantique (Chunking) via TokenTextSplitter
            // Paramètres optimisés : taille de chunk 400 tokens, min 100 caractères, préservation des séparateurs
            TokenTextSplitter textSplitter = new TokenTextSplitter(400, 100, 10, 5000, true);
            List<Document> chunks = textSplitter.split(List.of(rootDocument));

            log.info("Découpage sémantique terminé : {} chunks générés pour '{}'.", chunks.size(), rulebookResource.getFilename());

            // 3. Indexation vectorielle dense dans ChromaDB via VectorStore
            log.info("Indexation en cours de {} chunks dans la collection ChromaDB '{}'...", chunks.size(), collectionName);
            vectorStore.accept(chunks);

            log.info("Ingestion ETL RAG achevée avec succès : {} chunks indexés et persistés dans ChromaDB.", chunks.size());

        } catch (Exception ex) {
            log.error("Échec du pipeline d'ingestion documentaire RAG : {}", ex.getMessage(), ex);
            throw new IllegalStateException("Erreur lors de l'indexation des règles dans ChromaDB", ex);
        }
    }

    /**
     * Détermine si la collection ChromaDB est vide via la sonde native count ou par recherche de similarité.
     *
     * @return true si la collection ne contient aucun vecteur
     */
    private boolean isCollectionEmpty() {
        try {
            if (chromaApi != null) {
                ChromaApi.Collection collection = chromaApi.getCollection(collectionName);
                if (collection != null) {
                    Long count = chromaApi.countEmbeddings(collection.id());
                    return count == null || count == 0;
                }
            }
            List<Document> probe = vectorStore.similaritySearch(
                    SearchRequest.builder().query("règlement").topK(1).similarityThreshold(0.0).build()
            );
            return probe.isEmpty();
        } catch (Exception ex) {
            log.warn("Sonde de vacuité ChromaDB non concluante (application du fallback vide) : {}", ex.getMessage());
            return true;
        }
    }
}
