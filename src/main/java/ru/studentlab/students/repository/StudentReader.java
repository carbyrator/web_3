package ru.studentlab.students.repository;

import ru.studentlab.students.model.Student;
import ru.studentlab.students.query.StudentSearch;
import java.util.List;

// Чтение не меняет хранилище; отсутствующий ИСУ возвращает null, список — никогда null
public interface StudentReader {
    Student findByIsu(Integer isu);
    List<Student> findAll(StudentSearch search);
    long count(StudentSearch search);
}
