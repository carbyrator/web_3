package ru.studentlab.students.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

// Данные студента для JSON
@Schema(requiredProperties = {"name", "group", "isu", "dorm", "room", "expDate", "foreigner"})
public class StudentDto {
    @Schema(example = "Иван Иванов")
    public String name;
    @Schema(example = "P3220")
    public String group;
    @Schema(example = "505301")
    public Integer isu;
    public Integer dorm;
    public String room;
    @Schema(example = "2027-06-01")
    public LocalDate expDate;
    public Boolean foreigner;
    public String notes = "";

    public StudentDto() {
    }

    // Независимая копия для проверки PATCH
    public StudentDto(StudentDto student) {
        name = student.name;
        group = student.group;
        isu = student.isu;
        dorm = student.dorm;
        room = student.room;
        expDate = student.expDate;
        foreigner = student.foreigner;
        notes = student.notes;
    }
}
