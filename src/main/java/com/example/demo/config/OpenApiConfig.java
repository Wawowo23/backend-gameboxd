package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                .title("API de reseñas de videojuegos Gameboxd")
                        .version("1.0")
                                .description("Aquí se encuentra la documentación de todos los " +
                                        "endpoints del backend de reseñas de videojuegos 'Gameboxd'")
                                .contact(new Contact().name("Miguel Ángel Cámara Casado").email("miguelangelcamaracasado@gmail.com"))
                );
    }
}
