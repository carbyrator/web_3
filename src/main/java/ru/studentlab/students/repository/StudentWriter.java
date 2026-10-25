package ru.studentlab.students.repository;

import ru.studentlab.students.model.Student;

/** Сохраняет новую или существующую запись; возвращает сохранённую сущность с id. */
public interface StudentWriter {
    Student saveAndFlush(Student student);
    void delete(Student student);
}
