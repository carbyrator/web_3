package ru.studentlab.students.model;

import jakarta.persistence.*;

// Храним учетные записи, роли и хеши паролей в таблице app_users
@Entity
@Table(name = "app_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Имя пользователя (уникальное не пустое)
    @Column(nullable = false, unique = true, length = 40)
    private String username;

    // Почта (уникальная не пустая)
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    // Хеш пароля BCrypt, исходный пароль не сохраняем
    @Column(nullable = false)
    private String password;

    // Роль
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
