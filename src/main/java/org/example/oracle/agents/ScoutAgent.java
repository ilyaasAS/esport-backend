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
 * Agent Scout spécialisé dans l'analyse de données, classements et cotations probabilistes Elo (MySQL).
 * Exploite les outils de lecture et de pronostic sous une température de 0.2.
 */
@Component
public class ScoutAgent implements EsportAgent {

    private static final Logger log = LoggerFactory.getLogger(ScoutAgent.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final ChatClient scoutChatClient;

    public ScoutAgent(@Qualifier("scoutChatClient") ChatClient scoutChatClient) {
        this.scoutChatClient = scoutChatClient;
    }

    @Override
    public boolean supports(OracleIntent intent) {
        return intent == OracleIntent.READ || intent == OracleIntent.PREDICT_MATCH;
    }

    @Override
    public Flux<String> process(String userMessage, OracleIntent intent, boolean isAdmin) {
        return Flux.defer(() -> {
            log.info("Agent Scout activé pour l'analyse de données (intention: {})", intent);
            String content = scoutChatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                return Flux.just("Le Scout n'a pas pu récupérer les données de la ligue.");
            }

            String[] words = content.split("(?<=\\s+)");
            return Flux.fromArray(words);
        }).timeout(TIMEOUT);
    }
}
