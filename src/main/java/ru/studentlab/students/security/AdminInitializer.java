package ru.studentlab.students.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.studentlab.students.service.port.AdministratorAccounts;

// При запуске создаём администратора или обновляем его пароль из .env
@Component
public class AdminInitializer implements CommandLineRunner {
    @Value("${app.admin.username:}") private String username;
    @Value("${app.admin.email:}") private String email;
    @Value("${app.admin.password:}") private String password;
    private final AdministratorAccounts administrators;

    public AdminInitializer(AdministratorAccounts administrators) {
        this.administrators = administrators;
    }

    @Override
    public void run(String... args) {
        administrators.initialize(username, email, password);
    }
}
