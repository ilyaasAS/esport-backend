package org.example.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.web.dto.StandardApiErrorBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
/**
 * Point d'entrée JSON pour les accès non authentifiés (HTTP 401).
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    /**
     * Retourne une réponse JSON standardisée lorsque l'authentification est requise.
     *
     * @param request requête HTTP d'origine
     * @param response réponse HTTP à écrire
     * @param authException exception d'authentification déclenchée
     * @throws IOException en cas d'erreur d'entrée/sortie
     * @throws ServletException en cas d'erreur servlet
     */
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = StandardApiErrorBody.toMap(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Accès refusé : authentification requise.",
                request.getRequestURI(),
                null
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
