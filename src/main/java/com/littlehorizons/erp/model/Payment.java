package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A payment received from a guardian on behalf of a student. */
public record Payment(int id, int studentId, int guardianId, int conceptId, BigDecimal amountPaid,
                      LocalDate paymentDate, String method, String receiptNumber,
                      int academicYear, String period, String notes) {

    public static final List<String> METHODS =
            List.of("Efectivo", "Transferencia", "Tarjeta Débito", "Tarjeta Crédito", "Cheque");
    public static final List<String> PERIODS = List.of("I", "II", "III", "IV", "Anual");
}
