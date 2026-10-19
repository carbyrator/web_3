package ru.studentlab.students.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.studentlab.students.model.User;

// Spring Data сам создаёт простые запросы по названиям методов.
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
