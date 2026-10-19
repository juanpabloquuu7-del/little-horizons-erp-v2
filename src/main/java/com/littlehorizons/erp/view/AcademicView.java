package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.AverageLine;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/** Menus and listings of grades, averages and attendance. */
public class AcademicView extends BaseView {

    public AcademicView(Scanner scanner) {
        super(scanner);
    }

    public int mainMenu() {
        return showMenu("GESTIÓN ACADÉMICA", "Volver al menú principal",
                "Listar materias", "Crear materia", "Registrar / corregir nota", "Historial académico de un estudiante",
                "Promedios de un estudiante", "Promedios de un curso",
                "Registrar asistencia individual", "Registrar asistencia de una clase",
                "Historial de asistencia de un estudiante", "Alertas de inasistencia semanal",
                "Inasistencias acumuladas");
    }

    public void showGrades(List<com.littlehorizons.erp.model.GradeLevel> grades) {
        table(new String[]{"ID", "Grado"}, grades.stream().map(g -> cells(g.id(), g.name())).toList());
    }

    public void showSubjects(List<String[]> rows) {
        table(new String[]{"ID", "Código", "Materia", "Grado", "Horas/sem", "Estado"}, rows);
    }

    public void showHistory(List<String[]> rows) {
        table(new String[]{"Año", "Período", "Código", "Materia", "Nota", "Profesor"}, rows);
    }

    public void showAverages(String heading, List<AverageLine> lines) {
        title(heading);
        table(new String[]{"Concepto", "Promedio", "Notas"},
                lines.stream().map(l -> cells(l.label(), String.format(Locale.US, "%.2f", l.average()), l.count())).toList());
        if (!lines.isEmpty()) {
            double total = lines.stream().mapToDouble(l -> l.average() * l.count()).sum();
            long count = lines.stream().mapToLong(AverageLine::count).sum();
            System.out.printf(Locale.US, "  Promedio general: %.2f%n", total / count);
        }
    }

    public void showEnrolledStudents(List<String[]> rows) {
        table(new String[]{"ID", "Estudiante"}, rows);
    }

    public void showAttendance(List<String[]> rows) {
        table(new String[]{"Fecha", "Materia", "Estado", "Observaciones"}, rows);
        long absences = rows.stream().filter(r -> "Ausente".equals(r[2])).count();
        System.out.println("  Ausencias: " + absences + " de " + rows.size() + " registros");
    }

    public void showWeeklyAlerts(List<String[]> rows, int threshold) {
        title("Estudiantes con más de " + threshold + " ausencias en una semana");
        table(new String[]{"ID", "Estudiante", "Semana desde", "Ausencias"}, rows);
    }

    public void showAccumulated(List<String[]> rows) {
        table(new String[]{"ID", "Estudiante", "Ausencias", "Tardanzas", "Excusas médicas"}, rows);
    }
}
