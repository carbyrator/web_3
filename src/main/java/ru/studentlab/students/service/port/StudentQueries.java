package ru.studentlab.students.service.port;

import ru.studentlab.students.dto.StudentDto;
import ru.studentlab.students.dto.StudentSearchResult;
import ru.studentlab.students.query.StudentSearch;

public interface StudentQueries {
    StudentSearchResult findAll(StudentSearch search);
    StudentDto findByIsu(Integer isu);
}
