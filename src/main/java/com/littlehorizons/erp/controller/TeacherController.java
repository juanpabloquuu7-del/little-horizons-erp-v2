package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.InputValidator;
import com.littlehorizons.erp.view.TeacherView;

import java.time.LocalDate;
import java.util.List;

/** Orchestrates the teachers menu: directory, academic load and schedules. */
public class TeacherController extends BaseController {

    private static final List<String> DOCUMENT_TYPES = List.of("CC", "CE", "Pasaporte");

    private final TeacherView view;
    private final TeacherService service;

    public TeacherController(TeacherView view, TeacherService service) {
        super(view);
        this.view = view;
        this.service = service;
    }

    public void run() {
        int option;
        do {
            option = view.mainMenu();
            switch (option) {
                case 1 -> safely(this::registerTeacher);
                case 2 -> safely(() -> view.showTeachers(service.listTeachers()));
                case 3 -> safely(this::updateTeacher);
                case 4 -> safely(() -> {
                    int id = view.readInt("ID del profesor: ");
                    service.changeStatus(id, view.chooseOption("Nuevo estado", TeacherService.TEACHER_STATUSES));
                    view.success("Estado actualizado.");
                });
                case 5 -> safely(() -> {
                    int teacherId = view.readInt("ID del profesor: ");
                    int subjectId = view.readInt("ID de la materia: ");
                    String section = view.readOptionalText("Sección (A por defecto)");
                    int id = service.assignSubject(teacherId, subjectId, section);
                    view.success("Materia asignada (asignación " + id + ").");
                });
                case 6 -> safely(() -> view.showWorkload(service.workloadOf(view.readInt("ID del profesor: "))));
                case 7 -> safely(() -> {
                    int id = view.readInt("ID de la asignación a quitar: ");
                    if (view.confirm("¿Quitar la asignación " + id + " y sus horarios?")) {
                        service.removeAssignment(id);
                        view.success("Asignación eliminada.");
                    }
                });
                case 8 -> safely(() -> {
                    int assignmentId = view.readInt("ID de la asignación: ");
                    String day = view.chooseOption("Día de la semana", TeacherService.WEEKDAYS);
                    var start = view.readTime("Hora de inicio");
                    var end = view.readTime("Hora de fin");
                    service.addSchedule(assignmentId, day, start, end);
                    view.success("Horario registrado sin conflictos.");
                });
                case 9 -> safely(() -> view.showSchedule(service.scheduleOf(view.readInt("ID del profesor: "))));
                case 10 -> safely(() -> {
                    int id = view.readInt("ID del profesor a eliminar: ");
                    if (view.confirm("¿Eliminar a " + service.getTeacher(id).fullName() + "?")) {
                        service.deleteTeacher(id);
                        view.success("Profesor eliminado.");
                    }
                });
                default -> { }
            }
        } while (option != 0);
    }

    private void registerTeacher() {
        String documentType = view.chooseOption("Tipo de documento", DOCUMENT_TYPES);
        String documentNumber = view.readText("Número de documento: ");
        String firstName = view.readText("Primer nombre: ");
        String middleName = view.readOptionalText("Segundo nombre");
        String lastName = view.readText("Primer apellido: ");
        String secondLastName = view.readOptionalText("Segundo apellido");
        String email = view.readEmail("Correo electrónico: ");
        String phone = view.readOptionalText("Teléfono");
        String specialization = view.readText("Especialización (Matemáticas, Español...): ");
        int id = service.createTeacher(new Teacher(0, documentType, documentNumber, firstName, middleName, lastName,
                secondLastName, email, phone, specialization, LocalDate.now(), "Activo"));
        view.success("Profesor registrado con ID " + id + ".");
    }

    private void updateTeacher() {
        Teacher t = service.getTeacher(view.readInt("ID del profesor: "));
        view.info("Deje en blanco para conservar el valor actual.");
        String email = keep(view.readOptionalText("Correo [" + t.email() + "]"), t.email());
        if (!InputValidator.isValidEmail(email)) {
            throw new BusinessException("Correo electrónico inválido.");
        }
        service.updateTeacher(new Teacher(t.id(), t.documentType(), t.documentNumber(),
                keep(view.readOptionalText("Primer nombre [" + t.firstName() + "]"), t.firstName()), t.middleName(),
                keep(view.readOptionalText("Primer apellido [" + t.lastName() + "]"), t.lastName()), t.secondLastName(),
                email, keep(view.readOptionalText("Teléfono [" + nvl(t.phone()) + "]"), t.phone()),
                keep(view.readOptionalText("Especialización [" + nvl(t.specialization()) + "]"), t.specialization()),
                t.hireDate(), t.status()));
        view.success("Profesor actualizado.");
    }

    private static String keep(String input, String current) {
        return input == null || input.isBlank() ? current : input;
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
