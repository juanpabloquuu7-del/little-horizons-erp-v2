package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Scholarship;

import java.util.List;
import java.util.Optional;

public interface ScholarshipDao {
    Optional<Scholarship> findByStudentAndYear(int studentId, int academicYear);

    /** Creates the scholarship or replaces the one the student already has that year. */
    void save(Scholarship scholarship);

    /** @return number of deleted rows (0 when the student had no scholarship that year) */
    int delete(int studentId, int academicYear);

    /** Rows: student id, student, percent, reason, assigned on. */
    List<String[]> listRows(int academicYear);
}
