package com.littlehorizons.erp.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

/** Menu and tables of the statistics module. */
public class ReportView extends BaseView {

    public ReportView(Scanner scanner) {
        super(scanner);
    }

    public int mainMenu() {
        return showMenu("REPORTES Y ESTADÍSTICAS", "Volver al menú principal",
                "Estudiantes matriculados por grado", "Mejores promedios del año", "Participación en clubes",
                "Participación en eventos", "Ingresos por concepto", "Ingresos por mes", "Estado de los recursos");
    }

    public void showStudentsPerGrade(List<String[]> rows) {
        table(new String[]{"Grado", "Estudiantes activos"}, rows);
    }

    public void showTopStudents(List<String[]> rows) {
        table(new String[]{"ID", "Estudiante", "Grado", "Promedio", "Notas"}, rows);
    }

    public void showClubParticipation(List<String[]> rows) {
        table(new String[]{"Club", "Inscritos", "Cupo", "Ocupación"}, rows);
    }

    public void showEventParticipation(List<String[]> rows) {
        table(new String[]{"Evento", "Tipo", "Fecha", "Participantes", "Competidores"}, rows);
    }

    public void showIncome(String firstColumn, List<String[]> rows) {
        table(new String[]{firstColumn, "Pagos", "Total recaudado"},
                rows.stream().map(r -> cells(r[0], r[1], FinanceView.money(new BigDecimal(r[2])))).toList());
        BigDecimal total = rows.stream().map(r -> new BigDecimal(r[2])).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!rows.isEmpty()) {
            System.out.println("  Total: " + FinanceView.money(total));
        }
    }

    public void showResourceStatus(List<String[]> rows) {
        table(new String[]{"Categoría", "Disponible", "Prestado", "En mantenimiento", "Dado de baja"}, rows);
    }
}
