package com.littlehorizons.erp.model;

/** A guardian (acudiente) who can be responsible for several students (siblings). */
public record Guardian(int id, String documentType, String documentNumber, String firstName, String middleName,
                       String lastName, String secondLastName, String relationship, String phone,
                       String email, String address, boolean financialResponsible) {

    public String fullName() {
        return Names.join(firstName, middleName, lastName, secondLastName);
    }
}
