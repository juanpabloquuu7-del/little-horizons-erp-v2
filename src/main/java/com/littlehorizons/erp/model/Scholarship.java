package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Scholarship of a student for one academic year: a percentage discount on recurring concepts. */
public record Scholarship(int id, int studentId, int academicYear, BigDecimal percent, String reason,
                          LocalDate assignedOn) {
}
