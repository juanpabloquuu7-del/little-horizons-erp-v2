package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;

import java.util.List;
import java.util.Optional;

public interface GuardianDao {
    int insert(Guardian guardian);
    boolean update(Guardian guardian);
    boolean delete(int guardianId);
    Optional<Guardian> findById(int guardianId);
    Optional<Guardian> findByDocument(String documentNumber);
    List<Guardian> findAll();

    /** Guardians of a student, principal and financial responsible first. */
    List<Guardian> findByStudent(int studentId);

    /** Links a guardian to a student (or updates the flags of an existing link). */
    void link(int studentId, int guardianId, boolean principal, boolean emergencyContact);

    void clearPrincipal(int studentId);
    boolean unlink(int studentId, int guardianId);
    int countStudents(int guardianId);
}
