package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.service.ExtracurricularService;
import com.littlehorizons.erp.view.ExtracurricularView;

import java.util.ArrayList;
import java.util.List;

/** Orchestrates the clubs and events menus. */
public class ExtracurricularController extends BaseController {

    private final ExtracurricularView view;
    private final ExtracurricularService service;

    public ExtracurricularController(ExtracurricularView view, ExtracurricularService service) {
        super(view);
        this.view = view;
        this.service = service;
    }

    public void run() {
        int option;
        do {
            option = view.mainMenu();
            switch (option) {
                case 1 -> safely(this::createClub);
                case 2 -> safely(() -> view.showClubs(service.clubRows()));
                case 3 -> safely(() -> {
                    int clubId = view.readInt("ID del club: ");
                    service.enrollStudent(clubId, view.readInt("ID del estudiante: "));
                    view.success("Estudiante inscrito en el club.");
                });
                case 4 -> safely(() -> {
                    int clubId = view.readInt("ID del club: ");
                    int studentId = view.readInt("ID del estudiante: ");
                    service.withdrawStudent(clubId, studentId, view.readOptionalText("Motivo del retiro"));
                    view.success("Estudiante retirado del club.");
                });
                case 5 -> safely(() -> {
                    int clubId = view.readInt("ID del club: ");
                    var club = service.getClub(clubId);
                    view.title(club.name() + " (" + service.countMembers(clubId) + "/" + club.maxCapacity() + ")");
                    view.showMembers(service.members(clubId));
                });
                case 6 -> safely(() -> {
                    int clubId = view.readInt("ID del club: ");
                    boolean active = view.confirm("¿Dejar el club ACTIVO? (n = desactivar)");
                    service.setClubActive(clubId, active);
                    view.success(active ? "Club activado." : "Club desactivado.");
                });
                case 7 -> safely(this::createEvent);
                case 8 -> safely(() -> view.showEvents(service.listEvents()));
                case 9 -> safely(() -> view.showEvents(service.listEventsByType(typeChoice())));
                case 10 -> safely(this::registerParticipant);
                case 11 -> safely(() -> {
                    int eventId = view.readInt("ID del evento: ");
                    view.title(service.getEvent(eventId).name());
                    view.showParticipants(service.participants(eventId));
                });
                case 12 -> safely(() -> {
                    int eventId = view.readInt("ID del evento: ");
                    boolean active = view.confirm("¿Dejar el evento ACTIVO? (n = desactivar)");
                    service.setEventActive(eventId, active);
                    view.success(active ? "Evento activado." : "Evento desactivado.");
                });
                default -> { }
            }
        } while (option != 0);
    }

    private void createClub() {
        String name = view.readText("Nombre del club: ");
        String description = view.readOptionalText("Descripción");
        int capacity = view.readIntInRange("Cupo máximo: ", 1, 500);
        int supervisor = view.readInt("ID del profesor encargado (0 = ninguno): ");
        String schedule = view.readOptionalText("Horario de reunión (ej. Martes 3-5 PM)");
        view.success("Club creado con ID " + service.createClub(name, description, supervisor, capacity, schedule) + ".");
    }

    private void createEvent() {
        String name = view.readText("Nombre del evento: ");
        String description = view.readOptionalText("Descripción");
        String type = typeChoice();
        var date = view.readDate("Fecha del evento");
        String location = view.readOptionalText("Lugar");
        boolean competition = view.confirm("¿El evento tiene competencia con posiciones?");
        int organizer = view.readInt("ID del profesor organizador (0 = ninguno): ");
        view.success("Evento creado con ID "
                + service.createEvent(name, description, type, date, location, competition, organizer) + ".");
    }

    private void registerParticipant() {
        int eventId = view.readInt("ID del evento: ");
        SchoolEvent event = service.getEvent(eventId);
        int studentId = view.readInt("ID del estudiante: ");
        String role = view.chooseOption("Rol de participación", ExtracurricularService.PARTICIPATION_ROLES);
        String position = null;
        if (event.hasCompetition() && view.confirm("¿Registrar la posición obtenida?")) {
            position = view.chooseOption("Posición", ExtracurricularService.POSITIONS);
        }
        service.registerParticipant(eventId, studentId, role, position);
        view.success("Participación registrada.");
    }

    private String typeChoice() {
        List<String> types = new ArrayList<>(SchoolEvent.TYPES);
        return view.chooseOption("Tipo de evento", types);
    }
}
