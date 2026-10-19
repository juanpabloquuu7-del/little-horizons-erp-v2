package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A score (0.0 - 5.0) of a student in a subject for a period (I, II, III, IV or Final). */
public record AcademicRecord(int id, int studentId, int subjectId, int teacherId, int academicYear,
                             String period, BigDecimal score, String notes, LocalDate recordedOn) {

    public static final String FINAL_PERIOD = "Final";
}
