package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
public class SecurityConfig {

    // 1. INYECTAR EL FILTRO: Esto es lo que te faltaba
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. EXCEPCIONES PÚBLICAS (Sin token)
                        // Permitimos el registro para que los nuevos usuarios puedan crear su cuenta
                        .requestMatchers("/api/v1/usuarios/new", "/api/v1/usuarios/").permitAll()

                        // Permitimos ver (GET) la información sin estar logueado
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/juegos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/empresas/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/colecciones/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()

                        // 2. TODO LO DEMÁS (POST, PUT, DELETE, etc.) REQUIERE TOKEN
                        // Al no haber listado los POST de juegos o reviews arriba,
                        // caen aquí automáticamente y pedirán el JWT.
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        // El filtro que procesa el JWT antes de llegar a los controladores
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}