package ru.studentlab.students.repository;

import org.springframework.data.repository.Repository;
import ru.studentlab.students.model.User;

// Spring Data реализует узкие контракты без промежуточного адаптера.
public interface UserRepository extends Repository<User, Long>, UserReader, UserWriter { }
