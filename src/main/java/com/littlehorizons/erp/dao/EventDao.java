package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SchoolEvent;

import java.util.List;
import java.util.Optional;

public interface EventDao {
    int insert(SchoolEvent event);
    boolean setActive(int eventId, boolean active);
    Optional<SchoolEvent> findById(int eventId);
    List<SchoolEvent> findAll();
    List<SchoolEvent> findByType(String type);

    /** Registers a participant (or updates the role/position of an existing one). */
    void saveParticipant(int eventId, int studentId, String role, String position);

    /** Rows: student id, full name, grade, role, position. */
    List<String[]> findParticipants(int eventId);
}
