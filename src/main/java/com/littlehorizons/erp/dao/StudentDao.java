package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;

import java.util.List;
import java.util.Optional;

public interface StudentDao {
    int insert(Student student);
    boolean update(Student student);
    boolean updateStatus(int studentId, String status);
    boolean delete(int studentId);
    Optional<Student> findById(int studentId);
    Optional<Student> findByDocument(String documentNumber);
    List<Student> findAll();

    /** Active enrollments of a grade in an academic year. */
    List<Student> findByGrade(int gradeId, int year);

    /** Students sharing at least one guardian with the given student. */
    List<Student> findSiblings(int studentId);

    /** Rows: id, document, full name, grade of the given year, status. */
    List<String[]> listWithGrade(int year);
}
