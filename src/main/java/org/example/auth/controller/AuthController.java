package org.example.auth.controller;

import jakarta.validation.Valid;
import org.example.auth.dto.AuthResponse;
import org.example.auth.dto.LoginRequest;
import org.example.auth.dto.RegisterRequest;
import org.example.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
/**
 * Contrôleur REST dédié aux opérations d'authentification.
 */
public class AuthController {

    private final AuthService authService;

    /**
     * Construit le contrôleur d'authentification.
     *
     * @param authService service applicatif d'authentification
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Crée un nouveau compte utilisateur.
     *
     * @param request données d'inscription validées
     * @return réponse HTTP 201 contenant le jeton d'authentification
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Authentifie un utilisateur existant.
     *
     * @param request identifiants de connexion validés
     * @return réponse HTTP 200 contenant le jeton d'authentification
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
