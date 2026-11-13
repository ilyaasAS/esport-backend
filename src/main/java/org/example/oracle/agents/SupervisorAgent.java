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
 * Agent Superviseur & Coordinateur général de la ligue e-sport.
 * Prend en charge les interactions conversationnelles générales, le recadrage poli
 * et délègue les opérations de mutation administrative (joueurs, matchs).
 */
@Component
public class SupervisorAgent implements EsportAgent {

    private static final Logger log = LoggerFactory.getLogger(SupervisorAgent.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final ChatClient supervisorChatClient;

    public SupervisorAgent(@Qualifier("supervisorChatClient") ChatClient supervisorChatClient) {
        this.supervisorChatClient = supervisorChatClient;
    }

    @Override
    public boolean supports(OracleIntent intent) {
        return intent == OracleIntent.GREETING
                || intent == OracleIntent.SMALL_TALK
                || intent == OracleIntent.UNKNOWN_ACTION
                || intent == OracleIntent.CREATE_PLAYER
                || intent == OracleIntent.CREATE_MATCH;
    }

    @Override
    public Flux<String> process(String userMessage, OracleIntent intent, boolean isAdmin) {
        log.info("Agent Superviseur activé pour l'intention: {}", intent);

        if (intent == OracleIntent.CREATE_MATCH && !isAdmin) {
            return Flux.just("Je n'ai pas l'autorité nécessaire pour cette opération.");
        }

        // Pour les opérations de mutation (écriture administrative)
        if (intent == OracleIntent.CREATE_PLAYER || intent == OracleIntent.CREATE_MATCH) {
            return Flux.defer(() -> {
                String[] tools = intent == OracleIntent.CREATE_PLAYER
                        ? new String[]{"addPlayerTool"}
                        : (isAdmin ? new String[]{"createMatchTool"} : new String[]{});

                String content = supervisorChatClient.prompt()
                        .user(userMessage)
                        .functions(tools)
                        .call()
                        .content();

                if (content == null || content.isBlank()) {
                    return Flux.just("L'opération administrative n'a pas pu aboutir.");
                }

                String[] words = content.split("(?<=\\s+)");
                return Flux.fromArray(words);
            }).timeout(TIMEOUT);
        }

        // Pour les messages conversationnels (accueil, small-talk) en streaming direct
        return supervisorChatClient.prompt()
                .user(userMessage)
                .stream()
                .content()
                .timeout(TIMEOUT);
    }
}
