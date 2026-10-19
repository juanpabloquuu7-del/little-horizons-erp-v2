package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Teacher;

import java.util.List;
import java.util.Scanner;

/** Menus and listings for teachers, academic load and schedules. */
public class TeacherView extends BaseView {

    public TeacherView(Scanner scanner) {
        super(scanner);
    }

    public int mainMenu() {
        return showMenu("GESTIÓN DE PROFESORES", "Volver al menú principal",
                "Registrar profesor", "Listar profesores", "Actualizar datos del profesor",
                "Cambiar estado (Activo / Inactivo / Licencia)", "Asignar materia (carga académica)",
                "Ver carga académica de un profesor", "Quitar una asignación", "Registrar horario de clase",
                "Ver horario de un profesor", "Eliminar profesor");
    }

    public void showTeachers(List<Teacher> teachers) {
        table(new String[]{"ID", "Documento", "Nombre completo", "Correo", "Especialización", "Contratación", "Estado"},
                teachers.stream().map(t -> cells(t.id(), t.documentType() + " " + t.documentNumber(), t.fullName(),
                        t.email(), t.specialization(), t.hireDate(), t.status())).toList());
    }

    public void showWorkload(List<String[]> rows) {
        table(new String[]{"Asignación", "Código", "Materia", "Grado", "Sección", "Año"}, rows);
    }

    public void showSchedule(List<String[]> rows) {
        table(new String[]{"Asignación", "Materia", "Grado", "Día", "Inicio", "Fin"}, rows);
    }
}
