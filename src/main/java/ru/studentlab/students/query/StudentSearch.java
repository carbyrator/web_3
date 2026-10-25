package ru.studentlab.students.query;

import ru.studentlab.students.validation.StudentFields;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Проверенные неизменяемые критерии, общие для сервиса и хранилища. */
public record StudentSearch(Map<String, String> filters, String sortField,
                            String sortDirection, Integer page, int size) {
    public StudentSearch {
        if (filters == null || !StudentFields.ALL.containsAll(filters.keySet())
                || filters.values().stream().anyMatch(value -> value == null || value.isBlank())
                || !("id".equals(sortField) || StudentFields.ALL.contains(sortField))
                || !("asc".equals(sortDirection) || "desc".equals(sortDirection))
                || size < 1 || size > 100
                || (page != null && (page < 0 || (long) page * size > Integer.MAX_VALUE))) {
            throw new IllegalArgumentException("Некорректные критерии поиска");
        }
        if (filters.containsKey("foreigner") && !filters.get("foreigner").matches("true|false")) {
            throw new IllegalArgumentException("Некорректный статус студента");
        }
        filters = Collections.unmodifiableMap(new LinkedHashMap<>(filters));
    }
}
