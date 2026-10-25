package ru.studentlab.students.repository;

import ru.studentlab.students.model.User;

/** Отсутствующее имя возвращает null; проверки уникальности не меняют хранилище. */
public interface UserReader {
    User findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
