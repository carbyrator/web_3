package ru.studentlab.students.service.port;

import com.fasterxml.jackson.databind.JsonNode;
import ru.studentlab.students.dto.StudentDto;

public interface StudentCommands {
    StudentDto create(JsonNode input);
    StudentDto update(Integer isu, JsonNode input, boolean partial);
    void delete(Integer isu);
}
