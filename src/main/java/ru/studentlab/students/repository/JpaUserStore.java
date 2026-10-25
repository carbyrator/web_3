package ru.studentlab.students.repository;

import org.springframework.stereotype.Repository;
import ru.studentlab.students.model.User;

/** Адаптер Spring Data: прикладным сервисам не нужен весь JpaRepository. */
@Repository
public class JpaUserStore implements UserReader, UserWriter {
    private final UserRepository repository;

    public JpaUserStore(UserRepository repository) {
        this.repository = repository;
    }

    @Override public User findByUsername(String username) { return repository.findByUsername(username); }
    @Override public boolean existsByUsername(String username) { return repository.existsByUsername(username); }
    @Override public boolean existsByEmail(String email) { return repository.existsByEmail(email); }
    @Override public User saveAndFlush(User user) { return repository.saveAndFlush(user); }
}
