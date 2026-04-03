package com.example.demo;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

	public static void main(String[] args) {
		// Load .env file if it exists (for local development)
		Dotenv dotenv = Dotenv.configure()
				.directory(".")
				.ignoreIfMissing()
				.load();
		
		// Set environment variables as system properties for Spring to use
		dotenv.entries().forEach(e -> 
			System.setProperty(e.getKey(), e.getValue())
		);
		
		SpringApplication.run(DemoApplication.class, args);
	}

}
