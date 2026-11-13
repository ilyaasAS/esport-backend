package org.example.oracle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de recherche sémantique RAG interrogeant le VectorStore ChromaDB
 * pour extraire les articles et dispositions du règlement officiel de la ligue e-sport.
 */
@Service
public class TournamentRuleService {

    private static final Logger log = LoggerFactory.getLogger(TournamentRuleService.class);
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.45;
    private static final int DEFAULT_TOP_K = 3;

    private final VectorStore vectorStore;

    /**
     * Initialise le service avec le VectorStore ChromaDB injecté.
     *
     * @param vectorStore magasin vectoriel ChromaDB autoconfiguré
     */
    public TournamentRuleService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Recherche les articles réglementaires les plus pertinents pour une requête donnée.
     *
     * @param query question ou mot-clé posé par l'utilisateur
     * @return chaîne textuelle contenant les extraits des articles applicables ou un message clair
     */
    public String findRelevantRules(String query) {
        if (query == null || query.isBlank()) {
            return "Aucune question ou mot-clé réglementaire spécifié.";
        }

        log.info("Recherche RAG dans le règlement officiel pour la requête : '{}'", query);

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(DEFAULT_TOP_K)
                    .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                    .build();

            List<Document> documents = vectorStore.similaritySearch(request);

            // Fallback souple si le seuil strict à 0.45 ne retourne aucun chunk (tolérance sémantique)
            if (documents == null || documents.isEmpty()) {
                documents = vectorStore.similaritySearch(
                        SearchRequest.builder().query(query).topK(1).build()
                );
            }

            if (documents == null || documents.isEmpty()) {
                log.warn("Aucun extrait réglementaire pertinent trouvé pour : '{}'", query);
                return "Aucun article du règlement officiel ne correspond spécifiquement à cette requête.";
            }

            log.info("{} extraits réglementaires extraits pour la requête '{}'", documents.size(), query);

            StringBuilder sb = new StringBuilder("=== EXTRAITS DU RÈGLEMENT OFFICIEL DE LA LIGUE E-SPORT ===\n\n");
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                sb.append("[Extrait ").append(i + 1).append("]\n")
                  .append(doc.getText() != null ? doc.getText().trim() : "")
                  .append("\n\n");
            }
            return sb.toString().trim();

        } catch (Exception ex) {
            log.error("Erreur lors de l'interrogation du VectorStore ChromaDB : {}", ex.getMessage(), ex);
            return "Le service de consultation du règlement est temporairement indisponible.";
        }
    }
}
