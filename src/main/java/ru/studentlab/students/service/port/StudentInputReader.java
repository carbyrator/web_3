package ru.studentlab.students.service.port;

import com.fasterxml.jackson.databind.JsonNode;
import ru.studentlab.students.dto.StudentDto;

/** Возвращает проверенные данные. base=null означает полную запись, иначе PATCH.
 * Не изменяет base; ошибки структуры дают 400, ошибки полей — 422 (ApiException).
 */
public interface StudentInputReader {
    StudentDto read(JsonNode input, StudentDto base);
}
