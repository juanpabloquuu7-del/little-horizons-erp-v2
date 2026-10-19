package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.InputValidator;
import com.littlehorizons.erp.view.StudentView;

import java.time.LocalDate;
import java.util.List;

/** Orchestrates the students menu: registration, enrollment, guardians. */
public class StudentController extends BaseController {

    private static final List<String> STUDENT_DOCUMENTS = List.of("TI", "CC", "CE", "Pasaporte");
    private static final List<String> ADULT_DOCUMENTS = List.of("CC", "CE", "Pasaporte");
    private static final List<String> GENDERS = List.of("M", "F", "Otro");

    private final StudentView view;
    private final StudentService service;

    public StudentController(StudentView view, StudentService service) {
        super(view);
        this.view = view;
        this.service = service;
    }

    public void run() {
        int option;
        do {
            option = view.mainMenu();
            switch (option) {
                case 1 -> safely(this::registerStudent);
                case 2 -> safely(() -> view.showStudents(service.listStudents()));
                case 3 -> safely(() -> view.showStudent(service.getStudentByDocument(view.readText("Número de documento: "))));
                case 4 -> safely(this::showProfile);
                case 5 -> safely(this::updateStudent);
                case 6 -> safely(() -> {
                    int id = view.readInt("ID del estudiante: ");
                    service.changeStatus(id, view.chooseOption("Nuevo estado", StudentService.STUDENT_STATUSES));
                    view.success("Estado actualizado.");
                });
                case 7 -> safely(() -> {
                    int id = view.readInt("ID del estudiante: ");
                    service.enrollInGrade(id, askGradeId());
                    view.success("Matrícula " + service.academicYear() + " registrada.");
                });
                case 8 -> safely(() -> {
                    int id = view.readInt("ID del estudiante: ");
                    service.changeGrade(id, askGradeId());
                    view.success("Grado actualizado.");
                });
                case 9 -> safely(this::enrollInSubjects);
                case 10 -> safely(() -> view.showSubjectEnrollments(service.subjectEnrollments(view.readInt("ID del estudiante: "))));
                case 11 -> guardianMenu();
                case 12 -> safely(() -> {
                    int id = view.readInt("ID del estudiante a eliminar: ");
                    if (view.confirm("Se borrarán también sus notas, asistencia y pagos. ¿Eliminar a "
                            + service.getStudent(id).fullName() + "?")) {
                        service.deleteStudent(id);
                        view.success("Estudiante eliminado.");
                    }
                });
                default -> { }
            }
        } while (option != 0);
    }

    // ------------------------------------------------------------------ students

    private void registerStudent() {
        String documentType = view.chooseOption("Tipo de documento", STUDENT_DOCUMENTS);
        String documentNumber = view.readText("Número de documento: ");
        String firstName = view.readText("Primer nombre: ");
        String middleName = view.readOptionalText("Segundo nombre");
        String lastName = view.readText("Primer apellido: ");
        String secondLastName = view.readOptionalText("Segundo apellido");
        LocalDate birthDate = view.readDate("Fecha de nacimiento");
        String gender = view.chooseOption("Género", GENDERS);
        String address = view.readOptionalText("Dirección");
        String phone = view.readOptionalText("Teléfono");
        String email = optionalEmail("Correo electrónico");
        int guardianId = view.readInt("ID del acudiente responsable (0 = registrar uno nuevo): ");
        if (guardianId == 0) {
            guardianId = registerGuardian();
        }
        int id = service.registerStudent(new Student(0, documentType, documentNumber, firstName, middleName, lastName,
                secondLastName, birthDate, gender, address, phone, email, LocalDate.now(), Student.ACTIVE), guardianId);
        view.success("Estudiante registrado con ID " + id + ".");
        if (view.confirm("¿Matricularlo ahora en un grado para " + service.academicYear() + "?")) {
            service.enrollInGrade(id, askGradeId());
            view.success("Matrícula registrada.");
            if (view.confirm("¿Inscribirlo en todas las materias de su grado?")) {
                String period = view.chooseOption("Período académico", StudentService.PERIODS);
                view.success("Inscrito en " + service.enrollInAllGradeSubjects(id, period) + " materias.");
            }
        }
    }

    private void showProfile() {
        Student student = service.getStudent(view.readInt("ID del estudiante: "));
        view.showStudent(student);
        view.title("Acudientes");
        view.showGuardians(service.guardiansOf(student.id()));
        view.title("Hermanos (comparten acudiente)");
        view.showStudents(service.findSiblings(student.id()).stream()
                .map(s -> new String[]{String.valueOf(s.id()), s.documentNumber(), s.fullName(), "-", s.status()}).toList());
        view.title("Historial de matrículas");
        view.showEnrollmentHistory(service.enrollmentHistory(student.id()));
    }

    private void updateStudent() {
        Student s = service.getStudent(view.readInt("ID del estudiante: "));
        view.info("Deje en blanco para conservar el valor actual.");
        service.updateStudent(new Student(s.id(), s.documentType(), s.documentNumber(),
                keep(view.readOptionalText("Primer nombre [" + s.firstName() + "]"), s.firstName()),
                keep(view.readOptionalText("Segundo nombre [" + nvl(s.middleName()) + "]"), s.middleName()),
                keep(view.readOptionalText("Primer apellido [" + s.lastName() + "]"), s.lastName()),
                keep(view.readOptionalText("Segundo apellido [" + nvl(s.secondLastName()) + "]"), s.secondLastName()),
                s.birthDate(), s.gender(),
                keep(view.readOptionalText("Dirección [" + nvl(s.address()) + "]"), s.address()),
                keep(view.readOptionalText("Teléfono [" + nvl(s.phone()) + "]"), s.phone()),
                keep(optionalEmail("Correo [" + nvl(s.email()) + "]"), s.email()), s.admissionDate(), s.status()));
        view.success("Estudiante actualizado.");
    }

    private void enrollInSubjects() {
        int id = view.readInt("ID del estudiante: ");
        String period = view.chooseOption("Período académico", StudentService.PERIODS);
        if (view.confirm("¿Inscribir en TODAS las materias de su grado? (n = elegir una)")) {
            view.success("Se inscribió en " + service.enrollInAllGradeSubjects(id, period) + " materia(s) nueva(s).");
        } else {
            service.enrollInSubject(id, view.readInt("ID de la materia: "), period);
            view.success("Inscripción registrada.");
        }
    }

    private int askGradeId() {
        view.showGrades(service.gradeLevels());
        return view.readInt("ID del grado: ");
    }

    // ------------------------------------------------------------------ guardians

    private void guardianMenu() {
        int option;
        do {
            option = view.guardianMenu();
            switch (option) {
                case 1 -> safely(() -> view.success("Acudiente registrado con ID " + registerGuardian() + "."));
                case 2 -> safely(() -> view.showGuardians(service.listGuardians()));
                case 3 -> safely(() -> {
                    Guardian g = service.getGuardian(view.readInt("ID del acudiente: "));
                    view.showGuardians(List.of(g));
                    view.title("Estudiantes a cargo");
                    view.showStudents(service.studentsOfGuardian(g.id()));
                });
                case 4 -> safely(this::updateGuardian);
                case 5 -> safely(() -> {
                    int studentId = view.readInt("ID del estudiante: ");
                    int guardianId = view.readInt("ID del acudiente: ");
                    boolean principal = view.confirm("¿Será el acudiente principal?");
                    service.linkGuardian(studentId, guardianId, principal, view.confirm("¿Es contacto de emergencia?"));
                    view.success("Acudiente vinculado.");
                });
                case 6 -> safely(() -> {
                    int studentId = view.readInt("ID del estudiante: ");
                    service.unlinkGuardian(studentId, view.readInt("ID del acudiente: "));
                    view.success("Acudiente desvinculado.");
                });
                case 7 -> safely(() -> {
                    int id = view.readInt("ID del acudiente a eliminar: ");
                    if (view.confirm("¿Eliminar a " + service.getGuardian(id).fullName() + "?")) {
                        service.deleteGuardian(id);
                        view.success("Acudiente eliminado.");
                    }
                });
                default -> { }
            }
        } while (option != 0);
    }

    private int registerGuardian() {
        String documentType = view.chooseOption("Tipo de documento del acudiente", ADULT_DOCUMENTS);
        String documentNumber = view.readText("Número de documento: ");
        String firstName = view.readText("Primer nombre: ");
        String middleName = view.readOptionalText("Segundo nombre");
        String lastName = view.readText("Primer apellido: ");
        String secondLastName = view.readOptionalText("Segundo apellido");
        String relationship = view.readText("Parentesco (Padre, Madre, Abuelo, Tío, Tutor Legal...): ");
        String phone = view.readText("Teléfono principal: ");
        String email = optionalEmail("Correo electrónico");
        String address = view.readOptionalText("Dirección");
        boolean financial = view.confirm("¿Es el responsable de los pagos?");
        int id = service.createGuardian(new Guardian(0, documentType, documentNumber, firstName, middleName, lastName,
                secondLastName, relationship, phone, email, address, financial));
        if (id > 0) {
            view.info("Acudiente creado con ID " + id + ".");
        }
        return id;
    }

    private void updateGuardian() {
        Guardian g = service.getGuardian(view.readInt("ID del acudiente: "));
        view.info("Deje en blanco para conservar el valor actual.");
        service.updateGuardian(new Guardian(g.id(), g.documentType(), g.documentNumber(),
                keep(view.readOptionalText("Primer nombre [" + g.firstName() + "]"), g.firstName()),
                keep(view.readOptionalText("Segundo nombre [" + nvl(g.middleName()) + "]"), g.middleName()),
                keep(view.readOptionalText("Primer apellido [" + g.lastName() + "]"), g.lastName()),
                keep(view.readOptionalText("Segundo apellido [" + nvl(g.secondLastName()) + "]"), g.secondLastName()),
                keep(view.readOptionalText("Parentesco [" + g.relationship() + "]"), g.relationship()),
                keep(view.readOptionalText("Teléfono [" + g.phone() + "]"), g.phone()),
                keep(optionalEmail("Correo [" + nvl(g.email()) + "]"), g.email()),
                keep(view.readOptionalText("Dirección [" + nvl(g.address()) + "]"), g.address()),
                g.financialResponsible()));
        view.success("Acudiente actualizado.");
    }

    // ------------------------------------------------------------------ helpers

    private String optionalEmail(String prompt) {
        String email = view.readOptionalText(prompt);
        if (!email.isBlank() && !InputValidator.isValidEmail(email)) {
            throw new BusinessException("Correo electrónico inválido (ejemplo: nombre@dominio.com).");
        }
        return email;
    }

    private static String keep(String input, String current) {
        return input == null || input.isBlank() ? current : input;
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
