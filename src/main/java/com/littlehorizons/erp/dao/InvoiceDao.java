package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceDao {
    int insert(Invoice invoice);
    boolean updateFile(int invoiceId, String pdfName, String pdfPath);
    Optional<Invoice> findByPaymentId(int paymentId);

    /** Rows: number, date, receipt, student, file name. */
    List<String[]> listRows();

    /** Next consecutive invoice sequence number (1-based). */
    int nextSequence();
}
