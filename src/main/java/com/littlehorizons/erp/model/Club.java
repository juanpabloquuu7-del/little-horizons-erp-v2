package com.littlehorizons.erp.model;

/** An extracurricular club with a strict maximum capacity; supervisorId 0 means none. */
public record Club(int id, String name, String description, int supervisorId, int maxCapacity,
                   String meetingSchedule, boolean active) {
}
