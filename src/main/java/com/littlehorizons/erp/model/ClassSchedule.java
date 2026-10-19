package com.littlehorizons.erp.model;

import java.time.LocalTime;

/** Weekly class slot of a teaching assignment. */
public record ClassSchedule(int id, int assignmentId, String weekday, LocalTime startTime, LocalTime endTime) {
}
