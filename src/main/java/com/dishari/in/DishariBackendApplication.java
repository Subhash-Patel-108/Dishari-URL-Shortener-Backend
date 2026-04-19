package com.dishari.in;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.dishari.in.domain.repository")
@EntityScan(basePackages = "com.dishari.in.domain.entity")
public class DishariBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DishariBackendApplication.class, args);
	}

}
