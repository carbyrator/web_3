package ru.studentlab.students.dto;

import java.util.List;

public record StudentSearchResult(List<StudentDto> students, long total, Integer page, int size) {
    public StudentSearchResult {
        students = List.copyOf(students);
    }
}
