package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.service.FinanceService;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.InputValidator;
import com.littlehorizons.erp.view.FinanceView;

import java.math.BigDecimal;

/** Orchestrates payments, invoices and the delinquency report. */
public class FinanceController extends BaseController {

    private final FinanceView view;
    private final FinanceService service;

    public FinanceController(FinanceView view, FinanceService service) {
        super(view);
        this.view = view;
        this.service = service;
    }

    public void run() {
        int option;
        do {
            option = view.mainMenu();
            switch (option) {
                case 1 -> safely(() -> view.showConcepts(service.listConcepts()));
                case 2 -> safely(this::registerPayment);
                case 3 -> safely(() -> view.showStudentPayments(service.paymentsOfStudent(view.readInt("ID del estudiante: "))));
                case 4 -> safely(() -> view.showGuardianPayments(service.paymentsOfGuardian(view.readInt("ID del acudiente: "))));
                case 5 -> safely(() -> {
                    var file = service.issueInvoice(view.readInt("ID del pago a facturar: "));
                    view.success("Factura generada: " + file);
                });
                case 6 -> safely(() -> view.showInvoices(service.invoiceRows()));
                case 7 -> safely(() -> view.showDelinquents(service.delinquencyReport(), service.delinquencyDays(),
                        service.delinquencyConceptCode()));
                case 8 -> safely(this::assignScholarship);
                case 9 -> safely(() -> {
                    service.removeScholarship(view.readInt("ID del estudiante: "));
                    view.success("Beca eliminada.");
                });
                case 10 -> safely(() -> view.showScholarships(service.scholarshipRows()));
                default -> { }
            }
        } while (option != 0);
    }

    private void registerPayment() {
        int studentId = view.readInt("ID del estudiante: ");
        view.showConcepts(service.listConcepts());
        int conceptId = view.readInt("ID del concepto: ");
        BigDecimal suggested = service.suggestedAmount(studentId, conceptId);
        String discount = service.discountDescription(studentId, conceptId);
        if (!discount.isEmpty()) {
            view.info("Se aplicó " + discount + ": " + FinanceView.money(suggested));
        }
        String typed = view.readOptionalText("Monto pagado [Enter = " + FinanceView.money(suggested) + "]");
        BigDecimal amount = typed.isBlank() ? suggested
                : InputValidator.parseDecimal(typed).orElseThrow(() -> new BusinessException("Monto inválido."));
        String method = view.chooseOption("Método de pago", Payment.METHODS);
        String period = view.chooseOption("Período al que aplica", Payment.PERIODS);
        String notes = view.readOptionalText("Notas");
        var invoice = service.registerPaymentAndInvoice(studentId, conceptId, amount, method,
                AppConfig.getInstance().getAcademicYear(), period, notes);
        view.success("Pago registrado y factura electrónica generada:\n  " + invoice);
    }

    private void assignScholarship() {
        int studentId = view.readInt("ID del estudiante: ");
        BigDecimal percent = view.readDecimal("Porcentaje de descuento (1-100): ");
        String reason = view.readOptionalText("Motivo");
        service.assignScholarship(studentId, percent, reason);
        view.success("Beca asignada para " + AppConfig.getInstance().getAcademicYear() + ".");
    }
}
