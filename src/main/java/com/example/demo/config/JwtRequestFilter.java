package com.example.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {


        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                String uid = jwtUtil.extractUid(jwt);
                if (uid != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Es vital que el tercer parámetro no sea null (ArrayList vacío está bien)
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            uid, null, new ArrayList<>());

                    // Añadimos detalles de la petición (opcional pero recomendado)
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                System.out.println("ERROR EN JWT FILTER: " + e.getMessage());
                e.printStackTrace();
                // Token inválido, expirado o corrupto
            }
        }
        chain.doFilter(request, response);
    }
}
