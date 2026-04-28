package org.example.auth.security;

import org.example.auth.entity.UserEntity;
import org.example.auth.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
/**
 * Adaptateur Spring Security chargé de charger un utilisateur applicatif en {@link UserDetails}.
 */
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    /**
     * Charge un utilisateur par son nom pour l'authentification Spring Security.
     *
     * @param username identifiant utilisateur recherché
     * @return représentation {@link UserDetails} utilisable par Spring Security
     * @throws UsernameNotFoundException si l'utilisateur n'existe pas
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable."));

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(buildAuthorities(user))
                .build();
    }

    private Collection<? extends GrantedAuthority> buildAuthorities(UserEntity user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
}
