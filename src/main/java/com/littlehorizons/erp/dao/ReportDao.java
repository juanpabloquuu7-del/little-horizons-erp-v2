package com.littlehorizons.erp.dao;

import java.util.List;

/** Read-only aggregate queries used by the "Reportes y Estadísticas" menu. All results are display rows. */
public interface ReportDao {

    /** Rows: grade, active students enrolled in the year. */
    List<String[]> studentsPerGrade(int year);

    /** Best students by average of periods I-IV. Rows: id, student, grade, average, scores. */
    List<String[]> topStudents(int year, int limit);

    /** Rows: club, active members, capacity, occupancy %. */
    List<String[]> clubParticipation();

    /** Rows: event, type, date, participants, competitors. */
    List<String[]> eventParticipation();

    /** Rows: concept, payments, total collected (year). */
    List<String[]> incomeByConcept(int year);

    /** Rows: month, payments, total collected (year). */
    List<String[]> incomeByMonth(int year);

    /** Rows: category, available, loaned, maintenance, retired. */
    List<String[]> resourceStatus();
}
