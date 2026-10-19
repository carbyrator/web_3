package ru.studentlab.students.model;

import jakarta.persistence.*;
import java.time.LocalDate;

// Hibernate хранит объекты этого класса в таблице students.
@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "student_group", nullable = false, length = 5)
    private String group;

    @Column(nullable = false, unique = true)
    private Integer isu;

    @Column(nullable = false)
    private Integer dorm;

    @Column(nullable = false, length = 5)
    private String room;

    @Column(nullable = false)
    private LocalDate expDate;

    @Column(nullable = false)
    private Boolean foreigner;

    @Column(nullable = false, columnDefinition = "text")
    private String notes = "";

    // Внутренний id не меняется, даже если пользователь исправил ИСУ.
    // get... читает закрытое поле, set... меняет его. Это обычные методы класса.
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    public Integer getIsu() { return isu; }
    public void setIsu(Integer isu) { this.isu = isu; }
    public Integer getDorm() { return dorm; }
    public void setDorm(Integer dorm) { this.dorm = dorm; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public LocalDate getExpDate() { return expDate; }
    public void setExpDate(LocalDate expDate) { this.expDate = expDate; }
    public Boolean getForeigner() { return foreigner; }
    public void setForeigner(Boolean foreigner) { this.foreigner = foreigner; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
