package com.example.demo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct; // Spring Boot 3 usa jakarta en lugar de javax
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() throws IOException {
        String firebaseJson = System.getenv("FIREBASE_CONFIG");

        if (firebaseJson != null && !firebaseJson.isEmpty()) {
            InputStream serviceAccount = new ByteArrayInputStream(firebaseJson.getBytes(StandardCharsets.UTF_8));

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        } else {
            // Si no existe (Local), podrías dejar tu código antiguo o un aviso
            System.out.println("ADVERTENCIA: Variable FIREBASE_CONFIG no encontrada. Ignorando si estás en Local.");
        }
    }
}