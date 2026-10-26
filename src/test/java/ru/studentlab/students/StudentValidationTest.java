package ru.studentlab.students;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.studentlab.students.dto.StudentDto;
import ru.studentlab.students.exception.ApiException;
import ru.studentlab.students.service.StudentValidator;
import ru.studentlab.students.validation.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class StudentValidationTest {
    private final ObjectMapper json = new ObjectMapper();
    private final List<StudentValidationRule> rules = List.of(new StudentRules());

    @Test
    void additionalRuleWorksWithoutChangingParserOrService() throws Exception {
        List<StudentValidationRule> extended = new ArrayList<>(rules);
        extended.add((student, errors) -> {
            if (student.notes.length() > 5) {
                errors.putIfAbsent("notes", "Не более пяти символов");
            }
        });
        StudentValidator validator = new StudentValidator(extended);
        ApiException error = assertThrows(ApiException.class, () -> validator.read(
                json.readTree("{\"notes\":\"too long\"}"), validStudent()));
        assertEquals(422, error.getStatus());
        assertEquals("Не более пяти символов", error.getDetails().get("notes"));
    }

    @Test
    void patchNeverMutatesTheBaseEvenIfValidationFails() throws Exception {
        StudentValidator validator = new StudentValidator(rules);
        StudentDto base = validStudent();
        StudentDto patched = validator.read(json.readTree("{\"room\":\"202\"}"), base);
        assertEquals("202", patched.room);
        assertEquals("101", base.room);
        assertThrows(ApiException.class, () -> validator.read(
                json.readTree("{\"room\":\"303\",\"isu\":1}"), base));
        assertEquals("101", base.room);
        assertEquals(505301, base.isu);
    }

    @Test
    void everyBuiltInRuleAcceptsMissingFieldsAndPreservesOtherErrors() {
        for (StudentValidationRule rule : rules) {
            StudentDto empty = new StudentDto();
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("existing", "Старая ошибка");
            assertDoesNotThrow(() -> rule.validate(empty, errors));
            assertEquals("Старая ошибка", errors.get("existing"));
            assertNull(empty.name);
            assertNull(empty.dorm);
            assertNull(empty.expDate);
            errors.clear();
            rule.validate(validStudent(), errors);
            assertTrue(errors.isEmpty());
        }
    }

    @Test
    void wrongTypesNullUnknownFieldsAndImpossibleDatesAreRejected() {
        StudentValidator validator = new StudentValidator(rules);
        for (String input : List.of("{\"room\":null}", "{\"room\":3}",
                "{\"unknown\":1}", "{\"expDate\":\"2027-02-30\"}", "{\"foreigner\":\"false\"}")) {
            ApiException error = assertThrows(ApiException.class,
                    () -> validator.read(json.readTree(input), validStudent()));
            assertEquals(422, error.getStatus());
        }
        ApiException error = assertThrows(ApiException.class,
                () -> validator.read(json.readTree("{}"), validStudent()));
        assertEquals(400, error.getStatus());
    }

    static StudentDto validStudent() {
        StudentDto data = new StudentDto();
        data.name = "Иван Иванов";
        data.group = "P3220";
        data.isu = 505301;
        data.dorm = 8;
        data.room = "101";
        data.expDate = LocalDate.of(2027, 6, 1);
        data.foreigner = false;
        return data;
    }
}
