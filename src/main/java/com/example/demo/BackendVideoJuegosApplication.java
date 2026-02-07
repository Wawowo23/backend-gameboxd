package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.annotation.Order;

@SpringBootApplication(scanBasePackages = "com.example.demo")
@Order(value = 1)
@EnableCaching
public class BackendVideoJuegosApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendVideoJuegosApplication.class, args);
	}
}
