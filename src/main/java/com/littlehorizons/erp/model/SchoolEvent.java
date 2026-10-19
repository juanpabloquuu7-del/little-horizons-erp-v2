package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** A calendar event; organizerId 0 means none. */
public record SchoolEvent(int id, String name, String description, String type, LocalDate date,
                          String location, boolean hasCompetition, int organizerId, boolean active) {

    public static final java.util.List<String> TYPES =
            java.util.List.of("Académico", "Cultural", "Deportivo", "Social", "Científico");
}
