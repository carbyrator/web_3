package ru.studentlab.students.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.studentlab.students.dto.RegisterRequest;
import ru.studentlab.students.model.Role;
import ru.studentlab.students.model.User;
import ru.studentlab.students.repository.UserRepository;
import ru.studentlab.students.service.AuthService;

// Первого администратора создаём из .env, а не через публичную регистрацию.
@Component
public class AdminInitializer implements CommandLineRunner {
    @Value("${app.admin.username:}") private String username;
    @Value("${app.admin.email:}") private String email;
    @Value("${app.admin.password:}") private String password;
    private final AuthService auth;
    private final UserRepository users;

    public AdminInitializer(AuthService auth, UserRepository users) {
        this.auth = auth;
        this.users = users;
    }

    @Override
    public void run(String... args) {
        if (username.isBlank() && email.isBlank() && password.isBlank()) {
            return;
        }
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Для администратора нужны ADMIN_USERNAME, ADMIN_EMAIL и ADMIN_PASSWORD");
        }
        User existing = users.findByUsername(username.trim());
        if (existing != null) {
            if (existing.getRole() != Role.ADMIN) {
                throw new IllegalArgumentException("Имя администратора занято обычным пользователем");
            }
            return;
        }
        RegisterRequest input = new RegisterRequest();
        input.username = username;
        input.email = email;
        input.password = password;
        auth.createAdmin(input);
    }
}
