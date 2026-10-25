package ru.studentlab.students.validation;

import java.util.Set;

public final class StudentFields {
    public static final Set<String> ALL = Set.of(
            "name", "group", "isu", "dorm", "room", "expDate", "foreigner", "notes");

    private StudentFields() { }
}
