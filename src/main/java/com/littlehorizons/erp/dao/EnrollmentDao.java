package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EnrollmentDao {
    int insert(Enrollment enrollment);
    Optional<Enrollment> findByStudentAndYear(int studentId, int year);
    boolean changeGrade(int enrollmentId, int gradeId);
    boolean updateStatus(int enrollmentId, String status, LocalDate withdrawalDate);

    /** Rows: year, grade, status, enrollment date, withdrawal date (all years of a student). */
    List<String[]> historyRows(int studentId);
}
