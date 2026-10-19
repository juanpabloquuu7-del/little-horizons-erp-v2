package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;

import java.util.List;
import java.util.Scanner;

/** Menus and listings for students, guardians and enrollment. */
public class StudentView extends BaseView {

    public StudentView(Scanner scanner) {
        super(scanner);
    }

    public int mainMenu() {
        return showMenu("GESTIÓN DE ESTUDIANTES", "Volver al menú principal",
                "Registrar estudiante", "Listar estudiantes", "Buscar estudiante por documento",
                "Consultar ficha (acudientes, hermanos, matrículas)", "Actualizar datos del estudiante",
                "Cambiar estado (Activo / Graduado / Retirado / Suspendido)", "Matricular en un grado",
                "Cambiar de grado", "Inscribir en materias", "Ver inscripciones en materias",
                "Gestión de acudientes", "Eliminar estudiante");
    }

    public int guardianMenu() {
        return showMenu("GESTIÓN DE ACUDIENTES", "Volver",
                "Registrar acudiente", "Listar acudientes", "Consultar acudiente (con sus estudiantes)",
                "Actualizar acudiente", "Vincular acudiente a un estudiante", "Desvincular acudiente de un estudiante",
                "Eliminar acudiente");
    }

    public void showStudents(List<String[]> rows) {
        table(new String[]{"ID", "Documento", "Nombre completo", "Grado", "Estado"}, rows);
    }

    public void showStudent(Student s) {
        title("Ficha del estudiante #" + s.id());
        table(new String[]{"Campo", "Valor"}, List.of(
                cells("Nombre", s.fullName()), cells("Documento", s.documentType() + " " + s.documentNumber()),
                cells("Nacimiento", s.birthDate()), cells("Género", s.gender()),
                cells("Dirección", s.address()), cells("Teléfono", s.phone()), cells("Correo", s.email()),
                cells("Ingreso al colegio", s.admissionDate()), cells("Estado", s.status())));
    }

    public void showGuardians(List<Guardian> guardians) {
        table(new String[]{"ID", "Documento", "Nombre completo", "Parentesco", "Teléfono", "Correo", "Resp. financiero"},
                guardians.stream().map(g -> cells(g.id(), g.documentType() + " " + g.documentNumber(), g.fullName(),
                        g.relationship(), g.phone(), g.email(), g.financialResponsible() ? "Sí" : "No")).toList());
    }

    public void showGrades(List<GradeLevel> grades) {
        table(new String[]{"ID", "Grado", "Nivel"},
                grades.stream().map(g -> cells(g.id(), g.name(), g.level())).toList());
    }

    public void showEnrollmentHistory(List<String[]> rows) {
        table(new String[]{"Año", "Grado", "Estado", "Matrícula", "Retiro"}, rows);
    }

    public void showSubjectEnrollments(List<String[]> rows) {
        table(new String[]{"Período", "Código", "Materia", "Sección", "Estado"}, rows);
    }
}
