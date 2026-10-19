package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.PaymentConcept;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/** Menus and listings of the financial module. */
public class FinanceView extends BaseView {

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    public FinanceView(Scanner scanner) {
        super(scanner);
    }

    public int mainMenu() {
        return showMenu("GESTIÓN FINANCIERA", "Volver al menú principal",
                "Ver conceptos de pago", "Registrar pago y generar factura (PDF)",
                "Historial de pagos de un estudiante", "Historial de pagos de un acudiente",
                "Generar / reimprimir factura de un pago existente", "Listar facturas emitidas",
                "Reporte de morosos", "Asignar / cambiar beca de un estudiante", "Quitar beca de un estudiante",
                "Listar becas del año");
    }

    public void showConcepts(List<PaymentConcept> concepts) {
        table(new String[]{"ID", "Código", "Concepto", "Valor base", "Recurrente"},
                concepts.stream().map(c -> cells(c.id(), c.code(), c.name(), money(c.baseAmount()),
                        c.recurring() ? "Sí" : "No")).toList());
    }

    public void showStudentPayments(List<String[]> rows) {
        table(new String[]{"Pago", "Recibo", "Fecha", "Concepto", "Monto", "Método", "Período / año"},
                rows.stream().map(r -> cells(r[0], r[1], r[2], r[3], money(new BigDecimal(r[4])), r[5], r[6])).toList());
    }

    public void showGuardianPayments(List<String[]> rows) {
        table(new String[]{"Pago", "Recibo", "Fecha", "Estudiante", "Concepto", "Monto", "Método"},
                rows.stream().map(r -> cells(r[0], r[1], r[2], r[3], r[4], money(new BigDecimal(r[5])), r[6])).toList());
        BigDecimal total = rows.stream().map(r -> new BigDecimal(r[5])).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!rows.isEmpty()) {
            System.out.println("  Total pagado: " + money(total));
        }
    }

    public void showInvoices(List<String[]> rows) {
        table(new String[]{"Factura", "Fecha", "Recibo", "Estudiante", "Archivo"}, rows);
    }

    public void showScholarships(List<String[]> rows) {
        table(new String[]{"ID", "Estudiante", "Descuento %", "Motivo", "Asignada"}, rows);
    }

    public void showDelinquents(List<String[]> rows, int days, String conceptCode) {
        title("Sin pago de " + conceptCode + " en más de " + days + " días");
        table(new String[]{"ID", "Estudiante", "Grado", "Último pago", "Días", "Tel. acudiente"}, rows);
    }

    public static String money(BigDecimal value) {
        return MONEY.format(value);
    }
}
