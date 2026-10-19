package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Yearly enrollment of a student in a grade. Status: Activa, Completada, Retirada. */
public record Enrollment(int id, int studentId, int gradeId, int academicYear, String status,
                         LocalDate enrollmentDate, LocalDate withdrawalDate) {

    public static final String ACTIVE = "Activa";
    public static final String COMPLETED = "Completada";
    public static final String WITHDRAWN = "Retirada";
}
