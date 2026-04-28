package org.example.auth.service;

import org.example.auth.dto.AuthResponse;
import org.example.auth.dto.LoginRequest;
import org.example.auth.dto.RegisterRequest;
import org.example.auth.entity.Role;
import org.example.auth.entity.UserEntity;
import org.example.auth.exception.InvalidCredentialsException;
import org.example.auth.exception.UserAlreadyExistsException;
import org.example.auth.repository.UserRepository;
import org.example.auth.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * Service applicatif d'authentification.
 * <p>
 * Il gère l'inscription, l'authentification et l'émission des jetons JWT.
 */
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Inscrit un nouvel utilisateur avec le rôle {@code USER}.
     *
     * @param request charge utile d'inscription validée
     * @return réponse d'authentification contenant le jeton JWT et les informations utilisateur
     * @throws UserAlreadyExistsException si le nom d'utilisateur est déjà utilisé
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new UserAlreadyExistsException();
        }

        UserEntity user = new UserEntity(
                request.username().trim(),
                passwordEncoder.encode(request.password()),
                Role.USER
        );

        UserEntity savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);
        return buildResponse(savedUser, token);
    }

    /**
     * Authentifie un utilisateur existant et génère un nouveau jeton JWT.
     *
     * @param request charge utile de connexion
     * @return réponse d'authentification contenant le jeton JWT et les informations utilisateur
     * @throws InvalidCredentialsException si l'identifiant ou le mot de passe est invalide
     */
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }

        UserEntity user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(InvalidCredentialsException::new);
        String token = jwtService.generateToken(user);
        return buildResponse(user, token);
    }

    private AuthResponse buildResponse(UserEntity user, String token) {
        return new AuthResponse(
                token,
                "Bearer",
                user.getUsername(),
                user.getRole().name()
        );
    }
}
