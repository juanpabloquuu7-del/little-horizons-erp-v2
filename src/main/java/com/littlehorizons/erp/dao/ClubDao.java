package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClubDao {
    int insert(Club club);
    boolean setActive(int clubId, boolean active);
    Optional<Club> findById(int clubId);
    int countActiveMembers(int clubId);
    boolean isActiveMember(int clubId, int studentId);

    /**
     * Enrolls the student only if the club is active and still has room, in a single atomic statement.
     *
     * @return true if enrolled, false if the club is full (or inactive)
     */
    boolean enrollIfRoom(int clubId, int studentId, LocalDate date);

    boolean withdraw(int clubId, int studentId, LocalDate date, String reason);

    /** Rows: id, name, active members, capacity, supervisor, status, schedule. */
    List<String[]> listRows();

    /** Rows: student id, full name, join date. Active members only. */
    List<String[]> findMembers(int clubId);
}
