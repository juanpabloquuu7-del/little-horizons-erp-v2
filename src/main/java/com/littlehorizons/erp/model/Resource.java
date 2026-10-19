package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.List;

/** An inventory item identified by a unique code. */
public record Resource(int id, String code, String name, int categoryId, String categoryName,
                       String description, LocalDate acquisitionDate, String location, String status) {

    public static final String AVAILABLE = "Disponible";
    public static final String LOANED = "Prestado";
    public static final String MAINTENANCE = "En Mantenimiento";
    public static final String RETIRED = "Dado de Baja";
    public static final List<String> STATUSES = List.of(AVAILABLE, LOANED, MAINTENANCE, RETIRED);

    public boolean isAvailable() {
        return AVAILABLE.equals(status);
    }
}
