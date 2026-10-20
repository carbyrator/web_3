package ru.studentlab.students.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.studentlab.students.model.Student;
import java.time.LocalDate;

// Данные студента для JSON, внутренний номер записи в ответ не передаём
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

    // Копируем поля из записи БД в объект ответа
    public StudentDto(Student student) {
        name = student.getName();
        group = student.getGroup();
        isu = student.getIsu();
        dorm = student.getDorm();
        room = student.getRoom();
        expDate = student.getExpDate();
        foreigner = student.getForeigner();
        notes = student.getNotes();
    }
}
