package org.example.oracle.agents;

import org.example.oracle.OracleIntent;
import reactor.core.publisher.Flux;

/**
 * Contrat standard d'un agent spécialisé au sein du système Multi-Agents de la ligue e-sport.
 */
public interface EsportAgent {

    /**
     * Détermine si l'agent est compétent pour prendre en charge l'intention spécifiée.
     *
     * @param intent intention utilisateur classifiée
     * @return true si l'agent prend en charge cette intention
     */
    boolean supports(OracleIntent intent);

    /**
     * Traite la requête utilisateur et produit une réponse progressive réactive (SSE).
     *
     * @param userMessage message utilisateur brut
     * @param intent      intention classifiée
     * @param isAdmin     indicateur de privilège administrateur
     * @return flux réactif de fragments textuels
     */
    Flux<String> process(String userMessage, OracleIntent intent, boolean isAdmin);
}
