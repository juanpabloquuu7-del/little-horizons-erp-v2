package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Enrollment of a student in a subject for an academic period (I, II, III, IV). */
public record SubjectEnrollment(int id, int studentId, int subjectId, int academicYear, String period,
                                String section, LocalDate enrollmentDate, String status) {
}
