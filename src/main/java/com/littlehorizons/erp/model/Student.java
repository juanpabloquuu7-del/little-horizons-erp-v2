package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** A student. Status is one of Activo, Graduado, Retirado, Suspendido. */
public record Student(int id, String documentType, String documentNumber, String firstName, String middleName,
                      String lastName, String secondLastName, LocalDate birthDate, String gender,
                      String address, String phone, String email, LocalDate admissionDate, String status) {

    public static final String ACTIVE = "Activo";
    public static final String GRADUATED = "Graduado";
    public static final String WITHDRAWN = "Retirado";
    public static final String SUSPENDED = "Suspendido";

    public String fullName() {
        return Names.join(firstName, middleName, lastName, secondLastName);
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }
}
