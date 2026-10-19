package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Attendance mark of a student in a subject on a date. */
public record Attendance(int id, int studentId, int subjectId, LocalDate date, String status,
                         String notes, int recordedBy) {

    public static final String PRESENT = "Presente";
    public static final String ABSENT = "Ausente";
    public static final String LATE = "Tardanza";
    public static final String EXCUSED = "Excusa Médica";
}
