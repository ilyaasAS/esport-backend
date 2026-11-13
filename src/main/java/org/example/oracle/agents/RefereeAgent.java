package org.example.oracle.agents;

import org.example.oracle.OracleIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Agent Arbitre spécialisé dans les règles officielles de la ligue e-sport (ChromaDB RAG).
 * Exécute l'outil queryTournamentRulesTool sous une température de 0.1 pour un grounding absolu.
 */
@Component
public class RefereeAgent implements EsportAgent {

    private static final Logger log = LoggerFactory.getLogger(RefereeAgent.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final ChatClient refereeChatClient;

    public RefereeAgent(@Qualifier("refereeChatClient") ChatClient refereeChatClient) {
        this.refereeChatClient = refereeChatClient;
    }

    @Override
    public boolean supports(OracleIntent intent) {
        return intent == OracleIntent.RULES_INQUIRY;
    }

    @Override
    public Flux<String> process(String userMessage, OracleIntent intent, boolean isAdmin) {
        return Flux.defer(() -> {
            log.info("Agent Arbitre activé pour la requête réglementaire : '{}'", userMessage);
            String content = refereeChatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                return Flux.just("L'Arbitre n'a pas pu identifier d'article officiel applicable.");
            }

            String[] words = content.split("(?<=\\s+)");
            return Flux.fromArray(words);
        }).timeout(TIMEOUT);
    }
}
