package de.paf.tarifvergleich.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails admin = User.withUsername("admin")
                // simple (später ersetzen)
                .password("{noop}admin1")
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // Für dein Single-Page-UI (fetch POST) ist CSRF sonst oft im Weg.
                // Später sauber lösen (Token), jetzt pragmatisch aus.
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // Static / UI
                        .requestMatchers(
                                "/", "/index.html",
                                "/favicon.ico",
                                "/error",
                                "/**.css", "/**.js", "/**.map",
                                "/assets/**", "/static/**"
                        ).permitAll()

                        // User-APIs (frei)
                        .requestMatchers(HttpMethod.GET, "/api/kapitalanlagen").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tarife/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/berechnung").permitAll()

                        // Admin UI + Admin APIs (geschützt)
                        .requestMatchers("/admin.html").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Alles andere: erlauben (oder optional: authenticated())
                        .anyRequest().permitAll()
                )

                // Login Formular (Standard)
                .formLogin(Customizer.withDefaults())
                .logout(Customizer.withDefaults());

        return http.build();
    }
}