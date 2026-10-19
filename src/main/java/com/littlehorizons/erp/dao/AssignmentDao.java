package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.TeachingAssignment;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AssignmentDao {
    int insert(TeachingAssignment assignment);
    boolean delete(int assignmentId);
    Optional<TeachingAssignment> findById(int assignmentId);
    boolean exists(int teacherId, int subjectId, int year, String section);
    boolean isAssigned(int teacherId, int subjectId, int year);

    /** Rows: assignment id, subject code, subject, grade, section, year. */
    List<String[]> findByTeacher(int teacherId, int year);

    int insertSchedule(ClassSchedule schedule);

    /** True when the teacher already has a class overlapping the given slot. */
    boolean hasConflict(int teacherId, int year, String weekday, LocalTime start, LocalTime end);

    /** Rows: assignment id, subject, grade, weekday, start, end. */
    List<String[]> findSchedulesByTeacher(int teacherId, int year);
}
