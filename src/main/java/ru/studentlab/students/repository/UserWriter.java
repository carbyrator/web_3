package ru.studentlab.students.repository;

import ru.studentlab.students.model.User;

public interface UserWriter {
    User saveAndFlush(User user);
}
