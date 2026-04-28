package org.example.oracle;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/oracle")
public class OracleController {

    private static final Logger log = LoggerFactory.getLogger(OracleController.class);
    private final OracleService oracleService;

    public OracleController(OracleService oracleService) {
        this.oracleService = oracleService;
        log.info("Oracle Endpoint Initialized at /api/oracle/chat");
    }

    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @Valid @RequestBody OracleChatRequest request,
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Flux.just("Accès refusé : authentification requise.");
        }
        // Le badge est déterminé au point d'entrée servlet, avant toute exécution asynchrone.
        final boolean isAdmin = oracleService.isAdmin(authentication.getAuthorities());
        return oracleService.streamChat(request.message(), isAdmin);
    }
}
