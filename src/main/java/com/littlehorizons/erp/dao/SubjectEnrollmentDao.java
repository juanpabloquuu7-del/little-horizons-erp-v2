package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SubjectEnrollment;

import java.util.List;

public interface SubjectEnrollmentDao {
    int insert(SubjectEnrollment enrollment);
    boolean exists(int studentId, int subjectId, int year, String period);

    /** True when the student is enrolled (not withdrawn) in the subject during the year, any period. */
    boolean isEnrolled(int studentId, int subjectId, int year);

    /** Rows: period, subject code, subject, section, status. */
    List<String[]> findByStudent(int studentId, int year);

    /** Rows: student id, full name (students enrolled in the subject during the year). */
    List<String[]> findEnrolledStudents(int subjectId, int year);
}
