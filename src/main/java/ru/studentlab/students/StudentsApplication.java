package ru.studentlab.students;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Используем пользователей из своей базы вместо тестового пользователя Spring
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@OpenAPIDefinition(info = @Info(title = "API студентов", version = "1.0",
        description = "Студенты, пользователи, JWT и роли USER/ADMIN."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class StudentsApplication {
    // Метод main запускает приложение Spring Boot
    public static void main(String[] args) {
        SpringApplication.run(StudentsApplication.class, args);
    }
}
