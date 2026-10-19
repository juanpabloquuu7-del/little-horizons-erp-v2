package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** A resource loan to a student or a teacher (stored in separate tables). Status: Activo, Devuelto, Vencido. */
public record Loan(int id, int resourceId, int borrowerId, boolean teacherBorrower, LocalDate loanDate,
                   LocalDate expectedReturn, LocalDate actualReturn, String status, String notes) {

    public static final String ACTIVE = "Activo";
    public static final String RETURNED = "Devuelto";
    public static final String OVERDUE = "Vencido";
}
