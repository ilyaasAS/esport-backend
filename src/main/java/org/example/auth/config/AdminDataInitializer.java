package org.example.auth.config;

import org.example.auth.entity.Role;
import org.example.auth.entity.UserEntity;
import org.example.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;

    public AdminDataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_PASSWORD:admin123}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        logger.info("🔎 Diagnostic : Recherche de l'utilisateur admin...");

        if (userRepository.existsByUsernameIgnoreCase("admin")) {
            logger.info("ℹ️ Diagnostic : Compte admin déjà présent, aucune création nécessaire.");
            return;
        }

        logger.info("🚀 Action : Création du compte admin car inexistant.");
        UserEntity admin = new UserEntity(
                "admin",
                passwordEncoder.encode(adminPassword),
                Role.ADMIN
        );
        userRepository.save(admin);
        logger.info("✅ Succès : Admin créé avec le rôle ADMIN.");
    }
}
