package com.littlehorizons.erp.model;

/** A subject of the curriculum, belonging to one grade level. */
public record Subject(int id, String code, String name, String description, int gradeId,
                      int weeklyHours, boolean active) {
}
