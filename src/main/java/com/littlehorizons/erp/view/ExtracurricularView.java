package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.SchoolEvent;

import java.util.List;
import java.util.Scanner;

/** Menus and listings of clubs and events. */
public class ExtracurricularView extends BaseView {

    public ExtracurricularView(Scanner scanner) {
        super(scanner);
    }

    public int mainMenu() {
        return showMenu("ACTIVIDADES EXTRACURRICULARES", "Volver al menú principal",
                "Crear club", "Listar clubes y cupos", "Inscribir estudiante en un club", "Retirar estudiante de un club",
                "Ver miembros de un club", "Activar / desactivar club",
                "Crear evento", "Calendario de eventos", "Filtrar eventos por tipo",
                "Registrar participante / posición en un evento", "Ver participantes de un evento",
                "Activar / desactivar evento");
    }

    public void showClubs(List<String[]> rows) {
        table(new String[]{"ID", "Club", "Inscritos", "Cupo", "Profesor", "Estado", "Reunión"}, rows);
    }

    public void showMembers(List<String[]> rows) {
        table(new String[]{"ID", "Estudiante", "Ingreso"}, rows);
    }

    public void showEvents(List<SchoolEvent> events) {
        table(new String[]{"ID", "Evento", "Tipo", "Fecha", "Lugar", "Competencia", "Activo"},
                events.stream().map(e -> cells(e.id(), e.name(), e.type(), e.date(), e.location(),
                        e.hasCompetition() ? "Sí" : "No", e.active() ? "Sí" : "No")).toList());
    }

    public void showParticipants(List<String[]> rows) {
        table(new String[]{"ID", "Estudiante", "Grado", "Rol", "Posición"}, rows);
    }
}
