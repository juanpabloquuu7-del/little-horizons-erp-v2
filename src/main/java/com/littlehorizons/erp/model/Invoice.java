package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Simulated DIAN electronic invoice issued for a payment. */
public record Invoice(int id, String number, int paymentId, LocalDate issueDate, String cufe,
                      String pdfName, String pdfPath) {
}
