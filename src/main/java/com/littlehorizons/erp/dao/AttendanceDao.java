package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Attendance;

import java.util.List;

public interface AttendanceDao {

    /** Inserts the mark, or updates it when the student already has a mark for that subject and date. */
    void upsert(Attendance attendance);

    /** Rows: date, subject, status, notes. */
    List<String[]> historyRows(int studentId);

    /** Students with more than {@code threshold} absences in one week. Rows: id, name, week start, absences. */
    List<String[]> findWeeklyAbsences(int threshold);

    /** Rows: id, name, absences, late arrivals, medical excuses (absences at least minAbsences). */
    List<String[]> findAccumulated(int minAbsences);
}
