package com.littlehorizons.erp.model;

/** A teacher assigned to a subject (and section) for an academic year. */
public record TeachingAssignment(int id, int teacherId, int subjectId, int academicYear, String section) {
}
