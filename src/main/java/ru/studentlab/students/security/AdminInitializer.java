package ru.studentlab.students.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import ru.studentlab.students.dto.RegisterRequest;
import ru.studentlab.students.model.Role;
import ru.studentlab.students.model.User;
import ru.studentlab.students.repository.UserRepository;
import ru.studentlab.students.service.AuthService;
import java.nio.charset.StandardCharsets;

// Администратора создаём из .env. При смене пароля обновляем его при запуске.
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
        if (password.length() < 6 || password.length() > 40
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Пароль администратора: от 6 до 40 символов и не более 72 байт UTF-8");
        }
        User existing = users.findByUsername(username.trim());
        if (existing != null) {
            if (existing.getRole() != Role.ADMIN) {
                throw new IllegalArgumentException("Имя администратора занято обычным пользователем");
            }
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            if (!encoder.matches(password, existing.getPassword())) {
                existing.setPassword(encoder.encode(password));
                users.saveAndFlush(existing);
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
