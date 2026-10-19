package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;

import java.util.List;
import java.util.Optional;

public interface SubjectDao {
    int insert(Subject subject);
    boolean setActive(int subjectId, boolean active);
    Optional<Subject> findById(int subjectId);
    Optional<Subject> findByCode(String code);
    List<Subject> findByGrade(int gradeId);

    /** Rows: id, code, name, grade, weekly hours, status. Optionally filtered by grade (0 = all). */
    List<String[]> listRows(int gradeId);
}
