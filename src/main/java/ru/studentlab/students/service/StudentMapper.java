package ru.studentlab.students.service;

import org.springframework.stereotype.Component;
import ru.studentlab.students.dto.StudentDto;
import ru.studentlab.students.model.Student;

// Только преобразование данных без поиска, проверок и сохранения
@Component
public class StudentMapper {
    public StudentDto toDto(Student student) {
        StudentDto data = new StudentDto();
        data.name = student.getName();
        data.group = student.getGroup();
        data.isu = student.getIsu();
        data.dorm = student.getDorm();
        data.room = student.getRoom();
        data.expDate = student.getExpDate();
        data.foreigner = student.getForeigner();
        data.notes = student.getNotes();
        return data;
    }

    public void copyToEntity(StudentDto data, Student student) {
        student.setName(data.name);
        student.setGroup(data.group);
        student.setIsu(data.isu);
        student.setDorm(data.dorm);
        student.setRoom(data.room);
        student.setExpDate(data.expDate);
        student.setForeigner(data.foreigner);
        student.setNotes(data.notes);
    }
}
