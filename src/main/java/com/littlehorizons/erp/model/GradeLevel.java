package com.littlehorizons.erp.model;

/** A school grade level (Preescolar = level 0, Primero = 1 ... Once = 11). */
public record GradeLevel(int id, String name, int level) {
}
