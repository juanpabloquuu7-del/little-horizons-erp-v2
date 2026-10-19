package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.GradeLevel;

import java.util.List;
import java.util.Optional;

public interface GradeLevelDao {
    List<GradeLevel> findAll();
    Optional<GradeLevel> findById(int id);
}
