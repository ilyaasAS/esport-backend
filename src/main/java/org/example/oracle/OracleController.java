package org.example.oracle;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

/**
 * Contrôleur REST d'entrée du chat Oracle en streaming SSE.
 */
@RestController
@RequestMapping("/api/oracle")
public class OracleController {

    private static final Logger log = LoggerFactory.getLogger(OracleController.class);
    private final OracleService oracleService;

    /**
     * Construit le contrôleur Oracle.
     *
     * @param oracleService service d'orchestration Oracle
     */
    public OracleController(OracleService oracleService) {
        this.oracleService = oracleService;
        log.info("Point de terminaison Oracle initialisé sur /api/oracle/chat.");
    }

    /**
     * Exécute une requête Oracle et retourne une réponse en flux SSE.
     *
     * @param request charge utile contenant le message utilisateur
     * @param authentication contexte d'authentification courant
     * @return flux texte SSE contenant la réponse Oracle
     */
    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @Valid @RequestBody OracleChatRequest request,
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise.");
        }
        // Le badge est déterminé au point d'entrée servlet, avant toute exécution asynchrone.
        final boolean isAdmin = oracleService.isAdmin(authentication.getAuthorities());
        return oracleService.streamChat(request.message(), isAdmin);
    }
}
