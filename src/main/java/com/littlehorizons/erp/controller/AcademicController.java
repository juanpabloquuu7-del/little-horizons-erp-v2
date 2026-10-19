package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.view.AcademicView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Orchestrates subjects, grades/averages and attendance. */
public class AcademicController extends BaseController {

    private static final Map<String, String> ATTENDANCE_CODES = Map.of(
            "A", "Ausente", "T", "Tardanza", "E", "Excusa Médica", "P", "Presente");

    private final AcademicView view;
    private final AcademicService service;
    private final StudentService studentService;

    public AcademicController(AcademicView view, AcademicService service, StudentService studentService) {
        super(view);
        this.view = view;
        this.service = service;
        this.studentService = studentService;
    }

    public void run() {
        int option;
        do {
            option = view.mainMenu();
            switch (option) {
                case 1 -> safely(() -> view.showSubjects(service.listSubjects(askOptionalGradeId())));
                case 2 -> safely(this::createSubject);
                case 3 -> safely(this::recordGrade);
                case 4 -> safely(() -> view.showHistory(service.academicHistory(view.readInt("ID del estudiante: "))));
                case 5 -> safely(this::studentAverages);
                case 6 -> safely(this::courseAverages);
                case 7 -> safely(this::markIndividualAttendance);
                case 8 -> safely(this::markClassAttendance);
                case 9 -> safely(() -> view.showAttendance(service.attendanceHistory(view.readInt("ID del estudiante: "))));
                case 10 -> safely(() -> view.showWeeklyAlerts(service.weeklyAbsenceAlerts(), service.weeklyAbsenceThreshold()));
                case 11 -> safely(() -> view.showAccumulated(
                        service.accumulatedAbsences(view.readIntInRange("Mínimo de ausencias a reportar: ", 1, 500))));
                default -> { }
            }
        } while (option != 0);
    }

    private void createSubject() {
        String code = view.readText("Código (ej. MAT-5): ");
        String name = view.readText("Nombre de la materia: ");
        String description = view.readOptionalText("Descripción");
        int gradeId = askGradeId();
        int hours = view.readIntInRange("Horas semanales (1-40): ", 1, 40);
        int id = service.createSubject(code, name, description, gradeId, hours);
        view.success("Materia creada con ID " + id + ".");
    }

    private void recordGrade() {
        int teacherId = view.readInt("ID del profesor que registra: ");
        int studentId = view.readInt("ID del estudiante: ");
        int subjectId = view.readInt("ID de la materia: ");
        String period = view.chooseOption("Período", AcademicService.GRADE_PERIODS);
        BigDecimal score = view.readDecimal("Nota (0.0 a 5.0): ");
        String notes = view.readOptionalText("Observaciones");
        service.recordGrade(teacherId, studentId, subjectId, period, score, notes);
        view.success("Nota registrada.");
    }

    private void studentAverages() {
        int id = view.readInt("ID del estudiante: ");
        int year = service.academicYear();
        view.showAverages("Promedio por materia (" + year + ")", service.studentAveragesBySubject(id, year));
        view.showAverages("Promedio general por período (" + year + ")", service.studentAveragesByPeriod(id, year));
    }

    private void courseAverages() {
        int gradeId = askGradeId();
        int year = service.academicYear();
        view.showAverages("Promedio del curso por materia (" + year + ")", service.courseAveragesBySubject(gradeId, year));
        view.showAverages("Promedio del curso por período (" + year + ")", service.courseAveragesByPeriod(gradeId, year));
    }

    private void markIndividualAttendance() {
        int studentId = view.readInt("ID del estudiante: ");
        int subjectId = view.readInt("ID de la materia: ");
        LocalDate date = view.readDate("Fecha");
        String status = view.chooseOption("Estado de asistencia", AcademicService.ATTENDANCE_STATUSES);
        String notes = view.readOptionalText("Observaciones");
        int teacherId = view.readInt("ID del profesor que registra (0 = no indicar): ");
        service.markAttendance(studentId, subjectId, date, status, notes, teacherId);
        view.success("Asistencia registrada.");
    }

    private void markClassAttendance() {
        int subjectId = view.readInt("ID de la materia: ");
        LocalDate date = view.readDate("Fecha");
        int teacherId = view.readInt("ID del profesor que registra (0 = no indicar): ");
        view.showEnrolledStudents(service.enrolledStudents(subjectId));
        view.info("Todos quedan 'Presente' salvo los que indique. Formato: id:código, separados por coma.\n"
                + "  Códigos: A = Ausente, T = Tardanza, E = Excusa Médica. Ejemplo: 5:A, 7:T");
        Map<Integer, String> exceptions = parseExceptions(view.readOptionalText("Excepciones"));
        int total = service.markClassAttendance(subjectId, date, exceptions, teacherId);
        view.success("Asistencia registrada para " + total + " estudiantes (" + exceptions.size() + " con novedad).");
    }

    private static Map<Integer, String> parseExceptions(String input) {
        Map<Integer, String> result = new HashMap<>();
        if (input.isBlank()) {
            return result;
        }
        Arrays.stream(input.split(",")).map(String::trim).filter(t -> !t.isEmpty()).forEach(token -> {
            String[] parts = token.split(":");
            String status = parts.length == 2 ? ATTENDANCE_CODES.get(parts[1].trim().toUpperCase()) : null;
            try {
                if (status == null) {
                    throw new NumberFormatException();
                }
                result.put(Integer.parseInt(parts[0].trim()), status);
            } catch (NumberFormatException e) {
                throw new BusinessException("Excepción inválida: '" + token + "'. Use id:código (A, T o E).");
            }
        });
        return result;
    }

    private int askGradeId() {
        view.showGrades(studentService.gradeLevels());
        return view.readInt("ID del grado: ");
    }

    /** Grade filter for the subject list (0 = every grade). */
    private int askOptionalGradeId() {
        view.showGrades(studentService.gradeLevels());
        return view.readInt("ID del grado (0 = todos): ");
    }
}
