package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.EnrollmentDaoImpl;
import com.littlehorizons.erp.dao.GradeLevelDao;
import com.littlehorizons.erp.dao.GradeLevelDaoImpl;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.GuardianDaoImpl;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentDaoImpl;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectDaoImpl;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDaoImpl;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.CustomExceptions.NotFoundException;

import java.time.LocalDate;
import java.util.List;

/** Business logic for students, guardians, grade enrollment (matrícula) and subject enrollment. */
public class StudentService {

    public static final List<String> PERIODS = List.of("I", "II", "III", "IV");
    public static final List<String> STUDENT_STATUSES =
            List.of(Student.ACTIVE, Student.GRADUATED, Student.WITHDRAWN, Student.SUSPENDED);

    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final EnrollmentDao enrollmentDao;
    private final GradeLevelDao gradeLevelDao;
    private final SubjectDao subjectDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final int academicYear = AppConfig.getInstance().getAcademicYear();

    public StudentService() {
        this(new StudentDaoImpl(), new GuardianDaoImpl(), new EnrollmentDaoImpl(), new GradeLevelDaoImpl(),
                new SubjectDaoImpl(), new SubjectEnrollmentDaoImpl());
    }

    public StudentService(StudentDao studentDao, GuardianDao guardianDao, EnrollmentDao enrollmentDao,
                          GradeLevelDao gradeLevelDao, SubjectDao subjectDao, SubjectEnrollmentDao subjectEnrollmentDao) {
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.enrollmentDao = enrollmentDao;
        this.gradeLevelDao = gradeLevelDao;
        this.subjectDao = subjectDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
    }

    public int academicYear() {
        return academicYear;
    }

    // ------------------------------------------------------------------ students

    /** Registers the student and links the guardian as principal guardian and emergency contact. */
    public int registerStudent(Student draft, int guardianId) {
        validateStudent(draft);
        if (studentDao.findByDocument(draft.documentNumber()).isPresent()) {
            throw new BusinessException("Ya existe un estudiante con el documento " + draft.documentNumber() + ".");
        }
        getGuardian(guardianId);
        int id = studentDao.insert(draft);
        guardianDao.link(id, guardianId, true, true);
        return id;
    }

    public void updateStudent(Student student) {
        validateStudent(student);
        studentDao.findByDocument(student.documentNumber()).filter(other -> other.id() != student.id()).ifPresent(other -> {
            throw new BusinessException("Otro estudiante ya usa el documento " + student.documentNumber() + ".");
        });
        if (!studentDao.update(student)) {
            throw new NotFoundException("No existe un estudiante con id " + student.id());
        }
    }

    /** Changes the student status; withdrawing or graduating also closes the current enrollment. */
    public void changeStatus(int studentId, String status) {
        Validation.requireOneOf(status, STUDENT_STATUSES, "El estado");
        getStudent(studentId);
        studentDao.updateStatus(studentId, status);
        enrollmentDao.findByStudentAndYear(studentId, academicYear)
                .filter(e -> Enrollment.ACTIVE.equals(e.status()))
                .ifPresent(e -> {
                    if (Student.WITHDRAWN.equals(status)) {
                        enrollmentDao.updateStatus(e.id(), Enrollment.WITHDRAWN, LocalDate.now());
                    } else if (Student.GRADUATED.equals(status)) {
                        enrollmentDao.updateStatus(e.id(), Enrollment.COMPLETED, null);
                    }
                });
    }

    public void deleteStudent(int studentId) {
        if (!studentDao.delete(studentId)) {
            throw new NotFoundException("No existe un estudiante con id " + studentId);
        }
    }

    public Student getStudent(int studentId) {
        return studentDao.findById(studentId)
                .orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + studentId));
    }

    public Student getStudentByDocument(String documentNumber) {
        return studentDao.findByDocument(documentNumber.trim())
                .orElseThrow(() -> new NotFoundException("No existe un estudiante con el documento " + documentNumber));
    }

    /** Rows: id, document, full name, grade of the current year, status. */
    public List<String[]> listStudents() {
        return studentDao.listWithGrade(academicYear);
    }

    public List<Student> listStudentsByGrade(int gradeId) {
        getGrade(gradeId);
        return studentDao.findByGrade(gradeId, academicYear);
    }

    public List<Student> findSiblings(int studentId) {
        getStudent(studentId);
        return studentDao.findSiblings(studentId);
    }

    // ------------------------------------------------------------------ guardians

    public int createGuardian(Guardian draft) {
        Validation.requireText(draft.documentNumber(), "El documento");
        Validation.requireText(draft.firstName(), "El primer nombre");
        Validation.requireText(draft.lastName(), "El primer apellido");
        Validation.requireText(draft.relationship(), "El parentesco");
        Validation.requireText(draft.phone(), "El teléfono");
        if (guardianDao.findByDocument(draft.documentNumber()).isPresent()) {
            throw new BusinessException("Ya existe un acudiente con el documento " + draft.documentNumber() + ".");
        }
        return guardianDao.insert(draft);
    }

    public void updateGuardian(Guardian guardian) {
        if (!guardianDao.update(guardian)) {
            throw new NotFoundException("No existe un acudiente con id " + guardian.id());
        }
    }

    public void deleteGuardian(int guardianId) {
        getGuardian(guardianId);
        if (guardianDao.countStudents(guardianId) > 0) {
            throw new BusinessException("No se puede eliminar: el acudiente tiene estudiantes a cargo.");
        }
        guardianDao.delete(guardianId);
    }

    public Guardian getGuardian(int guardianId) {
        return guardianDao.findById(guardianId)
                .orElseThrow(() -> new NotFoundException("No existe un acudiente con id " + guardianId));
    }

    public List<Guardian> listGuardians() {
        return guardianDao.findAll();
    }

    public List<Guardian> guardiansOf(int studentId) {
        getStudent(studentId);
        return guardianDao.findByStudent(studentId);
    }

    /** Links an existing guardian to a student (several students per guardian are allowed: siblings). */
    public void linkGuardian(int studentId, int guardianId, boolean principal, boolean emergencyContact) {
        getStudent(studentId);
        getGuardian(guardianId);
        if (principal) {
            guardianDao.clearPrincipal(studentId);
        }
        guardianDao.link(studentId, guardianId, principal, emergencyContact);
    }

    public void unlinkGuardian(int studentId, int guardianId) {
        if (guardianDao.findByStudent(studentId).size() <= 1) {
            throw new BusinessException("Cada estudiante debe conservar al menos un acudiente.");
        }
        if (!guardianDao.unlink(studentId, guardianId)) {
            throw new BusinessException("El acudiente no está vinculado a ese estudiante.");
        }
    }

    /** Students (with grade) linked to a guardian: siblings. Rows as {@link #listStudents()}. */
    public List<String[]> studentsOfGuardian(int guardianId) {
        getGuardian(guardianId);
        return studentDao.listWithGrade(academicYear).stream()
                .filter(row -> guardianDao.findByStudent(Integer.parseInt(row[0])).stream().anyMatch(g -> g.id() == guardianId))
                .toList();
    }

    // ------------------------------------------------------------------ grade enrollment (matrícula)

    public List<GradeLevel> gradeLevels() {
        return gradeLevelDao.findAll();
    }

    public GradeLevel getGrade(int gradeId) {
        return gradeLevelDao.findById(gradeId).orElseThrow(() -> new NotFoundException("No existe el grado " + gradeId));
    }

    public int enrollInGrade(int studentId, int gradeId) {
        Student student = getStudent(studentId);
        getGrade(gradeId);
        if (!student.isActive()) {
            throw new BusinessException("Sólo se matriculan estudiantes activos (estado actual: " + student.status() + ").");
        }
        if (enrollmentDao.findByStudentAndYear(studentId, academicYear).isPresent()) {
            throw new BusinessException("El estudiante ya está matriculado en " + academicYear + ". Use 'Cambiar de grado'.");
        }
        return enrollmentDao.insert(new Enrollment(0, studentId, gradeId, academicYear, Enrollment.ACTIVE, LocalDate.now(), null));
    }

    public void changeGrade(int studentId, int gradeId) {
        getGrade(gradeId);
        Enrollment enrollment = currentEnrollment(studentId);
        enrollmentDao.changeGrade(enrollment.id(), gradeId);
    }

    /** Rows: year, grade, status, enrollment date, withdrawal date. */
    public List<String[]> enrollmentHistory(int studentId) {
        getStudent(studentId);
        return enrollmentDao.historyRows(studentId);
    }

    // ------------------------------------------------------------------ subject enrollment

    public void enrollInSubject(int studentId, int subjectId, String period) {
        Validation.requireOneOf(period, PERIODS, "El período");
        Enrollment enrollment = currentEnrollment(studentId);
        Subject subject = subjectDao.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("No existe una materia con id " + subjectId));
        checkSubjectMatchesGrade(subject, enrollment);
        if (subjectEnrollmentDao.exists(studentId, subjectId, academicYear, period)) {
            throw new BusinessException("El estudiante ya está inscrito en " + subject.name() + " para el período " + period + ".");
        }
        subjectEnrollmentDao.insert(new SubjectEnrollment(0, studentId, subjectId, academicYear, period, "A", LocalDate.now(), "Activa"));
    }

    /** Enrolls the student in every active subject of the grade of the current year; returns how many were new. */
    public int enrollInAllGradeSubjects(int studentId, String period) {
        Validation.requireOneOf(period, PERIODS, "El período");
        Enrollment enrollment = currentEnrollment(studentId);
        List<Subject> pending = subjectDao.findByGrade(enrollment.gradeId()).stream()
                .filter(Subject::active)
                .filter(s -> !subjectEnrollmentDao.exists(studentId, s.id(), academicYear, period))
                .toList();
        pending.forEach(s -> subjectEnrollmentDao.insert(
                new SubjectEnrollment(0, studentId, s.id(), academicYear, period, "A", LocalDate.now(), "Activa")));
        return pending.size();
    }

    /** Rows: period, subject code, subject, section, status. */
    public List<String[]> subjectEnrollments(int studentId) {
        getStudent(studentId);
        return subjectEnrollmentDao.findByStudent(studentId, academicYear);
    }

    // ------------------------------------------------------------------ helpers

    private Enrollment currentEnrollment(int studentId) {
        getStudent(studentId);
        return enrollmentDao.findByStudentAndYear(studentId, academicYear)
                .filter(e -> Enrollment.ACTIVE.equals(e.status()))
                .orElseThrow(() -> new BusinessException("El estudiante no tiene una matrícula activa en " + academicYear + "."));
    }

    private void checkSubjectMatchesGrade(Subject subject, Enrollment enrollment) {
        if (!subject.active()) {
            throw new BusinessException("La materia " + subject.code() + " está inactiva.");
        }
        if (subject.gradeId() != enrollment.gradeId()) {
            throw new BusinessException("La materia " + subject.code() + " no pertenece al grado de la matrícula del estudiante.");
        }
    }

    private static void validateStudent(Student s) {
        Validation.requireText(s.documentNumber(), "El documento");
        Validation.requireText(s.firstName(), "El primer nombre");
        Validation.requireText(s.lastName(), "El primer apellido");
        Validation.requireText(s.gender(), "El género");
        if (s.birthDate() == null || !s.birthDate().isBefore(LocalDate.now())) {
            throw new BusinessException("La fecha de nacimiento debe ser anterior a hoy.");
        }
    }
}
