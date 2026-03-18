package com.billpoint.backend;

import com.billpoint.backend.model.User;
import com.billpoint.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public static CommandLineRunner seedDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			userRepository.findByUsername("admin").ifPresentOrElse(admin -> {
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setIsActive(true);
				userRepository.save(admin);
				System.out.println("Admin user updated with password: admin123");
			}, () -> {
				User admin = new User();
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setRole("ADMIN");
				admin.setEmail("admin@billpoint.com");
				admin.setIsActive(true);
				admin.setCreatedAt(LocalDateTime.now());
				userRepository.save(admin);
				System.out.println("Default Admin user successfully seeded: admin / admin123");
			});
		};
	}
}
