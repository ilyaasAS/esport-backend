package org.example.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Hooks;

@Configuration
public class SecurityContextConfig {

    static {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        Hooks.enableAutomaticContextPropagation();
    }
}
