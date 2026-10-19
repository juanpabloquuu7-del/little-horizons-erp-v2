package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AssignmentDaoImpl;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectDaoImpl;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TeacherDaoImpl;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.CustomExceptions.NotFoundException;

import java.time.LocalTime;
import java.util.List;

/** Business logic for the teacher directory, academic load (assignments) and class schedules. */
public class TeacherService {

    public static final List<String> WEEKDAYS = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");
    public static final List<String> TEACHER_STATUSES = List.of("Activo", "Inactivo", "Licencia");

    private final TeacherDao teacherDao;
    private final SubjectDao subjectDao;
    private final AssignmentDao assignmentDao;
    private final int academicYear = AppConfig.getInstance().getAcademicYear();

    public TeacherService() {
        this(new TeacherDaoImpl(), new SubjectDaoImpl(), new AssignmentDaoImpl());
    }

    public TeacherService(TeacherDao teacherDao, SubjectDao subjectDao, AssignmentDao assignmentDao) {
        this.teacherDao = teacherDao;
        this.subjectDao = subjectDao;
        this.assignmentDao = assignmentDao;
    }

    public int createTeacher(Teacher draft) {
        validate(draft);
        if (teacherDao.findByDocument(draft.documentNumber()).isPresent()) {
            throw new BusinessException("Ya existe un profesor con el documento " + draft.documentNumber() + ".");
        }
        return teacherDao.insert(draft);
    }

    public void updateTeacher(Teacher teacher) {
        validate(teacher);
        if (!teacherDao.update(teacher)) {
            throw new NotFoundException("No existe un profesor con id " + teacher.id());
        }
    }

    public void changeStatus(int teacherId, String status) {
        Validation.requireOneOf(status, TEACHER_STATUSES, "El estado");
        getTeacher(teacherId);
        teacherDao.updateStatus(teacherId, status);
    }

    public void deleteTeacher(int teacherId) {
        if (!teacherDao.delete(teacherId)) {
            throw new NotFoundException("No existe un profesor con id " + teacherId);
        }
    }

    public Teacher getTeacher(int teacherId) {
        return teacherDao.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("No existe un profesor con id " + teacherId));
    }

    public List<Teacher> listTeachers() {
        return teacherDao.findAll();
    }

    // ------------------------------------------------------------------ academic load

    /** Assigns a subject (and section) to a teacher for the current academic year. */
    public int assignSubject(int teacherId, int subjectId, String section) {
        Teacher teacher = getTeacher(teacherId);
        if (!"Activo".equals(teacher.status())) {
            throw new BusinessException("El profesor no está activo (estado: " + teacher.status() + ").");
        }
        Subject subject = subjectDao.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("No existe una materia con id " + subjectId));
        if (!subject.active()) {
            throw new BusinessException("La materia " + subject.code() + " está inactiva.");
        }
        String normalizedSection = section == null || section.isBlank() ? "A" : section.trim().toUpperCase();
        if (assignmentDao.exists(teacherId, subjectId, academicYear, normalizedSection)) {
            throw new BusinessException("El profesor ya tiene asignada esa materia en la sección " + normalizedSection + ".");
        }
        return assignmentDao.insert(new TeachingAssignment(0, teacherId, subjectId, academicYear, normalizedSection));
    }

    public void removeAssignment(int assignmentId) {
        if (!assignmentDao.delete(assignmentId)) {
            throw new NotFoundException("No existe una asignación con id " + assignmentId);
        }
    }

    /** Rows: assignment id, subject code, subject, grade, section, year. */
    public List<String[]> workloadOf(int teacherId) {
        getTeacher(teacherId);
        return assignmentDao.findByTeacher(teacherId, academicYear);
    }

    // ------------------------------------------------------------------ schedules

    /** Registers a weekly slot for an assignment, rejecting overlaps with the teacher's other classes. */
    public int addSchedule(int assignmentId, String weekday, LocalTime start, LocalTime end) {
        TeachingAssignment assignment = assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("No existe una asignación con id " + assignmentId));
        Validation.requireOneOf(weekday, WEEKDAYS, "El día");
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        if (assignmentDao.hasConflict(assignment.teacherId(), assignment.academicYear(), weekday, start, end)) {
            throw new BusinessException("Conflicto de horario: el profesor ya tiene una clase el " + weekday
                    + " que se cruza con " + start + " - " + end + ".");
        }
        return assignmentDao.insertSchedule(new ClassSchedule(0, assignmentId, weekday, start, end));
    }

    /** Rows: assignment id, subject, grade, weekday, start, end. */
    public List<String[]> scheduleOf(int teacherId) {
        getTeacher(teacherId);
        return assignmentDao.findSchedulesByTeacher(teacherId, academicYear);
    }

    private static void validate(Teacher t) {
        Validation.requireText(t.documentNumber(), "El documento");
        Validation.requireText(t.firstName(), "El primer nombre");
        Validation.requireText(t.lastName(), "El primer apellido");
        Validation.requireText(t.email(), "El correo");
    }
}
