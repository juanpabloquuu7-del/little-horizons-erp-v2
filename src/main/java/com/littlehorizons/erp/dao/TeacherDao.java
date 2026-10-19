package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Teacher;

import java.util.List;
import java.util.Optional;

public interface TeacherDao {
    int insert(Teacher teacher);
    boolean update(Teacher teacher);
    boolean updateStatus(int teacherId, String status);
    boolean delete(int teacherId);
    Optional<Teacher> findById(int teacherId);
    Optional<Teacher> findByDocument(String documentNumber);
    List<Teacher> findAll();
}
