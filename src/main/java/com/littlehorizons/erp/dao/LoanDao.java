package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Loan;

import java.time.LocalDate;
import java.util.List;

public interface LoanDao {

    /** Registers the loan and marks the resource as loaned, atomically. */
    int lend(Loan loan);

    /**
     * Registers the return and frees the resource, atomically.
     *
     * @return false when there is no unreturned loan with that id
     */
    boolean giveBack(int loanId, boolean teacherBorrower, LocalDate returnDate);

    /** Flags unreturned loans past their due date as 'Vencido'; returns how many were updated. */
    int markOverdue();

    /** Rows: loan id, borrower type, borrower, resource code, resource, loan date, due date, status. */
    List<String[]> findActive();

    /** Unreturned loans past due. Rows: same columns as {@link #findActive()} plus days late. */
    List<String[]> findOverdue();
}
