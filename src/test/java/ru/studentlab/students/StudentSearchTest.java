package ru.studentlab.students;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.studentlab.students.exception.ApiException;
import ru.studentlab.students.query.StudentSearch;
import ru.studentlab.students.query.StudentSearchParser;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class StudentSearchTest {
    private final ObjectMapper json = new ObjectMapper();
    private final StudentSearchParser parser = new StudentSearchParser();

    @Test
    void supportsLegacyAliasAndSizeWithoutPage() throws Exception {
        StudentSearch search = parser.parse(json.readTree(
                "{\"dormitory\":8,\"foreigner\":\"FALSE\",\"size\":2,\"sort\":\"isu,DESC\"}"));
        assertEquals(Map.of("dorm", "8", "foreigner", "false"), search.filters());
        assertEquals(0, search.page());
        assertEquals(2, search.size());
        assertEquals("desc", search.sortDirection());
        assertNull(parser.parse(json.readTree("{}")).page());
    }

    @Test
    void rejectsConflictingAliasesOverflowAndInvalidSort() {
        for (String input : new String[]{"{\"dorm\":8,\"dormitory\":9}",
                "{\"page\":2147483647,\"size\":100}", "{\"sort\":\"name desc;drop table students\"}",
                "{\"sort\":\"id\"}", "{\"size\":0}", "{\"foreigner\":\"maybe\"}"}) {
            ApiException error = assertThrows(ApiException.class, () -> parser.parse(json.readTree(input)));
            assertEquals(422, error.getStatus());
        }
    }

    @Test
    void searchCannotBeChangedAfterValidation() {
        Map<String, String> filters = new LinkedHashMap<>(Map.of("room", "101"));
        StudentSearch search = new StudentSearch(filters, "id", "asc", null, 20);
        filters.put("injected", "bad");
        assertEquals(Map.of("room", "101"), search.filters());
        assertThrows(UnsupportedOperationException.class, () -> search.filters().put("room", "202"));
        assertThrows(IllegalArgumentException.class,
                () -> new StudentSearch(Map.of(), "bad field", "asc", null, 20));
    }
}
