package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ClubDaoImpl;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.EventDaoImpl;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentDaoImpl;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TeacherDaoImpl;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.CustomExceptions.NotFoundException;

import java.time.LocalDate;
import java.util.List;

/** Business logic of clubs (strict capacity) and the events calendar with competition results. */
public class ExtracurricularService {

    public static final List<String> PARTICIPATION_ROLES = List.of("Participante", "Competidor", "Organizador", "Asistente");
    public static final List<String> POSITIONS = List.of("1er Lugar", "2do Lugar", "3er Lugar", "Participante");

    private final ClubDao clubDao;
    private final EventDao eventDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public ExtracurricularService() {
        this(new ClubDaoImpl(), new EventDaoImpl(), new StudentDaoImpl(), new TeacherDaoImpl());
    }

    public ExtracurricularService(ClubDao clubDao, EventDao eventDao, StudentDao studentDao, TeacherDao teacherDao) {
        this.clubDao = clubDao;
        this.eventDao = eventDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    // ------------------------------------------------------------------ clubs

    public int createClub(String name, String description, int supervisorId, int maxCapacity, String schedule) {
        Validation.requireText(name, "El nombre del club");
        if (maxCapacity <= 0) {
            throw new BusinessException("El cupo máximo debe ser mayor que cero.");
        }
        if (supervisorId != 0 && teacherDao.findById(supervisorId).isEmpty()) {
            throw new NotFoundException("No existe un profesor con id " + supervisorId);
        }
        return clubDao.insert(new Club(0, name.trim(), description, supervisorId, maxCapacity, schedule, true));
    }

    public void setClubActive(int clubId, boolean active) {
        getClub(clubId);
        clubDao.setActive(clubId, active);
    }

    public Club getClub(int clubId) {
        return clubDao.findById(clubId).orElseThrow(() -> new NotFoundException("No existe un club con id " + clubId));
    }

    /** Rows: id, club, active members, capacity, supervisor, status, schedule. */
    public List<String[]> clubRows() {
        return clubDao.listRows();
    }

    public int countMembers(int clubId) {
        return clubDao.countActiveMembers(clubId);
    }

    /** Enrolls a student in a club, refusing when the maximum capacity has been reached. */
    public void enrollStudent(int clubId, int studentId) {
        Club club = getClub(clubId);
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + studentId));
        if (!club.active()) {
            throw new BusinessException("El club " + club.name() + " no está activo.");
        }
        if (!student.isActive()) {
            throw new BusinessException("El estudiante no está activo (estado: " + student.status() + ").");
        }
        if (clubDao.isActiveMember(clubId, studentId)) {
            throw new BusinessException("El estudiante ya pertenece a " + club.name() + ".");
        }
        if (!clubDao.enrollIfRoom(clubId, studentId, LocalDate.now())) {
            throw new BusinessException("El club " + club.name() + " alcanzó su cupo máximo (" + club.maxCapacity() + ").");
        }
    }

    public void withdrawStudent(int clubId, int studentId, String reason) {
        getClub(clubId);
        if (!clubDao.withdraw(clubId, studentId, LocalDate.now(), reason)) {
            throw new BusinessException("El estudiante no es miembro activo de ese club.");
        }
    }

    /** Rows: student id, full name, join date (active members). */
    public List<String[]> members(int clubId) {
        getClub(clubId);
        return clubDao.findMembers(clubId);
    }

    // ------------------------------------------------------------------ events

    public int createEvent(String name, String description, String type, LocalDate date, String location,
                           boolean hasCompetition, int organizerId) {
        Validation.requireText(name, "El nombre del evento");
        Validation.requireOneOf(type, SchoolEvent.TYPES, "El tipo de evento");
        if (date == null) {
            throw new BusinessException("La fecha del evento es obligatoria.");
        }
        if (organizerId != 0 && teacherDao.findById(organizerId).isEmpty()) {
            throw new NotFoundException("No existe un profesor con id " + organizerId);
        }
        return eventDao.insert(new SchoolEvent(0, name.trim(), description, type, date, location, hasCompetition, organizerId, true));
    }

    public void setEventActive(int eventId, boolean active) {
        getEvent(eventId);
        eventDao.setActive(eventId, active);
    }

    public SchoolEvent getEvent(int eventId) {
        return eventDao.findById(eventId).orElseThrow(() -> new NotFoundException("No existe un evento con id " + eventId));
    }

    public List<SchoolEvent> listEvents() {
        return eventDao.findAll();
    }

    public List<SchoolEvent> listEventsByType(String type) {
        Validation.requireOneOf(type, SchoolEvent.TYPES, "El tipo de evento");
        return eventDao.findByType(type);
    }

    /** Registers a participant; a position is only accepted for events with competition. */
    public void registerParticipant(int eventId, int studentId, String role, String position) {
        SchoolEvent event = getEvent(eventId);
        studentDao.findById(studentId).orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + studentId));
        Validation.requireOneOf(role, PARTICIPATION_ROLES, "El rol");
        String finalPosition = position == null || position.isBlank() ? null : position;
        if (finalPosition != null) {
            Validation.requireOneOf(finalPosition, POSITIONS, "La posición");
            if (!event.hasCompetition()) {
                throw new BusinessException("El evento '" + event.name() + "' no tiene competencia: no admite posiciones.");
            }
        }
        eventDao.saveParticipant(eventId, studentId, role, finalPosition);
    }

    /** Rows: student id, full name, grade, role, position. */
    public List<String[]> participants(int eventId) {
        getEvent(eventId);
        return eventDao.findParticipants(eventId);
    }
}
