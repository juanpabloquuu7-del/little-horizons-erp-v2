package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;

import java.util.List;

public interface AcademicRecordDao {

    /** Inserts the score, or updates it when the student/subject/year/period record already exists. */
    void upsert(AcademicRecord record);

    List<AcademicRecord> findByStudent(int studentId);

    /** All records of the subjects of a grade level in an academic year (course statistics). */
    List<AcademicRecord> findByGradeAndYear(int gradeId, int year);

    /** Rows: year, period, subject code, subject, score, teacher (full academic history of a student). */
    List<String[]> historyRows(int studentId);
}
