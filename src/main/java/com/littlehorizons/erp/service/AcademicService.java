package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AcademicRecordDaoImpl;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AssignmentDaoImpl;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.AttendanceDaoImpl;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentDaoImpl;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectDaoImpl;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDaoImpl;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TeacherDaoImpl;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.AverageLine;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.CustomExceptions.NotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Business logic of the academic module: subjects, grades with automatic averages, and attendance. */
public class AcademicService {

    public static final List<String> GRADE_PERIODS = List.of("I", "II", "III", "IV", "Final");
    public static final List<String> ATTENDANCE_STATUSES =
            List.of(Attendance.PRESENT, Attendance.ABSENT, Attendance.LATE, Attendance.EXCUSED);

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = new BigDecimal("5.0");

    private final SubjectDao subjectDao;
    private final StudentDao studentDao;
    private final AssignmentDao assignmentDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final AcademicRecordDao recordDao;
    private final AttendanceDao attendanceDao;
    private final TeacherDao teacherDao;
    private final AppConfig config = AppConfig.getInstance();
    private final int academicYear = config.getAcademicYear();

    public AcademicService() {
        this(new SubjectDaoImpl(), new StudentDaoImpl(), new AssignmentDaoImpl(), new SubjectEnrollmentDaoImpl(),
                new AcademicRecordDaoImpl(), new AttendanceDaoImpl(), new TeacherDaoImpl());
    }

    public AcademicService(SubjectDao subjectDao, StudentDao studentDao, AssignmentDao assignmentDao,
                           SubjectEnrollmentDao subjectEnrollmentDao, AcademicRecordDao recordDao, AttendanceDao attendanceDao,
                           TeacherDao teacherDao) {
        this.subjectDao = subjectDao;
        this.studentDao = studentDao;
        this.assignmentDao = assignmentDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.recordDao = recordDao;
        this.attendanceDao = attendanceDao;
        this.teacherDao = teacherDao;
    }

    public int academicYear() {
        return academicYear;
    }

    // ------------------------------------------------------------------ subjects

    public int createSubject(String code, String name, String description, int gradeId, int weeklyHours) {
        Validation.requireText(code, "El código");
        Validation.requireText(name, "El nombre de la materia");
        if (weeklyHours < 1 || weeklyHours > 40) {
            throw new BusinessException("Las horas semanales deben estar entre 1 y 40.");
        }
        String normalizedCode = code.trim().toUpperCase();
        if (subjectDao.findByCode(normalizedCode).isPresent()) {
            throw new BusinessException("Ya existe una materia con el código " + normalizedCode + ".");
        }
        return subjectDao.insert(new Subject(0, normalizedCode, name.trim(), description, gradeId, weeklyHours, true));
    }

    public Subject getSubject(int subjectId) {
        return subjectDao.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("No existe una materia con id " + subjectId));
    }

    /** Rows: id, code, name, grade, weekly hours, status (gradeId 0 = every grade). */
    public List<String[]> listSubjects(int gradeId) {
        return subjectDao.listRows(gradeId);
    }

    // ------------------------------------------------------------------ grades

    /** Registers (or corrects) a score; the teacher must be assigned to the subject and the student enrolled in it. */
    public void recordGrade(int teacherId, int studentId, int subjectId, String period, BigDecimal score, String notes) {
        Validation.requireOneOf(period, GRADE_PERIODS, "El período");
        if (score == null || score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new BusinessException("La nota debe estar entre 0.0 y 5.0.");
        }
        studentDao.findById(studentId).orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + studentId));
        getSubject(subjectId);
        if (!assignmentDao.isAssigned(teacherId, subjectId, academicYear)) {
            throw new BusinessException("El profesor " + teacherId + " no tiene asignada esa materia en " + academicYear + ".");
        }
        if (!subjectEnrollmentDao.isEnrolled(studentId, subjectId, academicYear)) {
            throw new BusinessException("El estudiante no está inscrito en esa materia en " + academicYear + ".");
        }
        recordDao.upsert(new AcademicRecord(0, studentId, subjectId, teacherId, academicYear, period,
                score.setScale(1, RoundingMode.HALF_UP), notes, LocalDate.now()));
    }

    /** Rows: year, period, subject code, subject, score, teacher (complete history since admission). */
    public List<String[]> academicHistory(int studentId) {
        studentDao.findById(studentId).orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + studentId));
        return recordDao.historyRows(studentId);
    }

    /** Average per subject for one student (periods I-IV of the academic year). */
    public List<AverageLine> studentAveragesBySubject(int studentId, int year) {
        Map<Integer, String> names = subjectNames();
        return averages(studentRecords(studentId, year), r -> names.getOrDefault(r.subjectId(), "Materia " + r.subjectId()));
    }

    /** General average of one student for each period (all subjects). */
    public List<AverageLine> studentAveragesByPeriod(int studentId, int year) {
        return averages(studentRecords(studentId, year), r -> "Período " + r.period());
    }

    /** Average of the whole course (grade level) in each subject. */
    public List<AverageLine> courseAveragesBySubject(int gradeId, int year) {
        Map<Integer, String> names = subjectNames();
        return averages(courseRecords(gradeId, year), r -> names.getOrDefault(r.subjectId(), "Materia " + r.subjectId()));
    }

    /** General average of the whole course in each period. */
    public List<AverageLine> courseAveragesByPeriod(int gradeId, int year) {
        return averages(courseRecords(gradeId, year), r -> "Período " + r.period());
    }

    private List<AcademicRecord> studentRecords(int studentId, int year) {
        studentDao.findById(studentId).orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + studentId));
        return recordDao.findByStudent(studentId).stream().filter(r -> r.academicYear() == year).toList();
    }

    private List<AcademicRecord> courseRecords(int gradeId, int year) {
        return recordDao.findByGradeAndYear(gradeId, year);
    }

    /** Groups the scores (excluding the manual 'Final' record) and averages each group with the Stream API. */
    private List<AverageLine> averages(List<AcademicRecord> records, Function<AcademicRecord, String> classifier) {
        return records.stream()
                .filter(r -> !AcademicRecord.FINAL_PERIOD.equals(r.period()))
                .collect(Collectors.groupingBy(classifier, Collectors.summarizingDouble(r -> r.score().doubleValue())))
                .entrySet().stream()
                .map(e -> new AverageLine(e.getKey(), e.getValue().getAverage(), e.getValue().getCount()))
                .sorted(Comparator.comparing(AverageLine::label))
                .toList();
    }

    private Map<Integer, String> subjectNames() {
        return subjectDao.listRows(0).stream().collect(Collectors.toMap(r -> Integer.parseInt(r[0]), r -> r[2] + " (" + r[3] + ")"));
    }

    // ------------------------------------------------------------------ attendance

    /** Marks one student in one subject; teacherId may be 0 when unknown. */
    public void markAttendance(int studentId, int subjectId, LocalDate date, String status, String notes, int teacherId) {
        Validation.requireOneOf(status, ATTENDANCE_STATUSES, "El estado de asistencia");
        requireValidAttendanceDate(date);
        requireTeacherIfGiven(teacherId);
        studentDao.findById(studentId).orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + studentId));
        getSubject(subjectId);
        if (!subjectEnrollmentDao.isEnrolled(studentId, subjectId, date.getYear())) {
            throw new BusinessException("El estudiante no está inscrito en esa materia en " + date.getYear() + ".");
        }
        attendanceDao.upsert(new Attendance(0, studentId, subjectId, date, status, notes, teacherId));
    }

    /** Rows: student id, full name (students enrolled in the subject in the current year). */
    public List<String[]> enrolledStudents(int subjectId) {
        getSubject(subjectId);
        return subjectEnrollmentDao.findEnrolledStudents(subjectId, academicYear);
    }

    /**
     * Marks the whole class: everyone is 'Presente' except the students listed in {@code exceptions}
     * (student id to status).
     *
     * @return number of students marked
     */
    public int markClassAttendance(int subjectId, LocalDate date, Map<Integer, String> exceptions, int teacherId) {
        // Validate everything before the first insert so the class is never left half marked.
        requireValidAttendanceDate(date);
        if (date.getYear() != academicYear) {
            throw new BusinessException("La fecha debe pertenecer al año académico " + academicYear + ".");
        }
        requireTeacherIfGiven(teacherId);
        exceptions.values().forEach(status -> Validation.requireOneOf(status, ATTENDANCE_STATUSES, "El estado de asistencia"));
        List<String[]> students = enrolledStudents(subjectId);
        if (students.isEmpty()) {
            throw new BusinessException("La materia no tiene estudiantes inscritos en " + academicYear + ".");
        }
        List<Integer> enrolledIds = students.stream().map(r -> Integer.parseInt(r[0])).toList();
        exceptions.keySet().stream().filter(id -> !enrolledIds.contains(id)).findFirst().ifPresent(id -> {
            throw new BusinessException("El estudiante " + id + " no está inscrito en la materia.");
        });
        enrolledIds.forEach(id -> markAttendance(id, subjectId, date, exceptions.getOrDefault(id, Attendance.PRESENT), null, teacherId));
        return enrolledIds.size();
    }

    private static void requireValidAttendanceDate(LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new BusinessException("No se puede registrar asistencia de una fecha futura.");
        }
    }

    /** teacherId 0 means "not given"; any other value must be an existing teacher. */
    private void requireTeacherIfGiven(int teacherId) {
        if (teacherId != 0 && teacherDao.findById(teacherId).isEmpty()) {
            throw new NotFoundException("No existe un profesor con id " + teacherId);
        }
    }

    /** Rows: date, subject, status, notes. */
    public List<String[]> attendanceHistory(int studentId) {
        studentDao.findById(studentId).orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + studentId));
        return attendanceDao.historyRows(studentId);
    }

    /** Students with more than N absences in a week (N from configuration). Rows: id, name, week start, absences. */
    public List<String[]> weeklyAbsenceAlerts() {
        return attendanceDao.findWeeklyAbsences(weeklyAbsenceThreshold());
    }

    public int weeklyAbsenceThreshold() {
        return config.getInt("business.weeklyAbsenceThreshold", 3);
    }

    /** Rows: id, name, absences, late arrivals, excuses. */
    public List<String[]> accumulatedAbsences(int minAbsences) {
        return attendanceDao.findAccumulated(Math.max(1, minAbsences));
    }
}
