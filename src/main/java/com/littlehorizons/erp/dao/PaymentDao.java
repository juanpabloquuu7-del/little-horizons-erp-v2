package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;

import java.util.List;
import java.util.Optional;

public interface PaymentDao {
    List<PaymentConcept> findActiveConcepts();
    Optional<PaymentConcept> findConceptById(int conceptId);
    int insert(Payment payment);
    Optional<Payment> findById(int paymentId);

    /** Next consecutive sequence used to build receipt numbers. */
    int nextSequence();

    /** Rows: id, receipt, date, concept, amount, method, period. */
    List<String[]> historyByStudent(int studentId);

    /** Payments of every student linked to the guardian. Rows: id, receipt, date, student, concept, amount, method. */
    List<String[]> historyByGuardian(int guardianId);

    /**
     * Active students whose last payment of the recurring concept is older than {@code days} days
     * (or who never paid it). Rows: id, student, grade, last payment, days elapsed, guardian phone.
     */
    List<String[]> findDelinquent(String conceptCode, int year, int days);
}
