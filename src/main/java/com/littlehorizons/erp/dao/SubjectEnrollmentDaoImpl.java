package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SubjectEnrollment;

import java.util.List;

public class SubjectEnrollmentDaoImpl extends BaseJdbcDao implements SubjectEnrollmentDao {

    @Override
    public int insert(SubjectEnrollment e) {
        return insert("""
                INSERT INTO inscripciones_materia (id_estudiante, id_materia, año_academico, periodo_academico, seccion, fecha_inscripcion, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, e.studentId(), e.subjectId(), e.academicYear(), e.period(), nullIfBlank(e.section()),
                e.enrollmentDate(), e.status());
    }

    @Override
    public boolean exists(int studentId, int subjectId, int year, String period) {
        return count("SELECT COUNT(*) FROM inscripciones_materia WHERE id_estudiante = ? AND id_materia = ? AND año_academico = ? AND periodo_academico = ?",
                studentId, subjectId, year, period) > 0;
    }

    @Override
    public boolean isEnrolled(int studentId, int subjectId, int year) {
        return count("SELECT COUNT(*) FROM inscripciones_materia WHERE id_estudiante = ? AND id_materia = ? AND año_academico = ? AND estado <> 'Retirada'",
                studentId, subjectId, year) > 0;
    }

    @Override
    public List<String[]> findByStudent(int studentId, int year) {
        return query("""
                SELECT i.periodo_academico, m.codigo_materia, m.nombre_materia, COALESCE(i.seccion, '-'), i.estado
                FROM inscripciones_materia i JOIN materias m ON m.id_materia = i.id_materia
                WHERE i.id_estudiante = ? AND i.año_academico = ?
                ORDER BY FIELD(i.periodo_academico, 'I', 'II', 'III', 'IV'), m.nombre_materia
                """, columns(5), studentId, year);
    }

    @Override
    public List<String[]> findEnrolledStudents(int subjectId, int year) {
        return query("SELECT e.id_estudiante, " + STUDENT_NAME + """
                 FROM inscripciones_materia i JOIN estudiantes e ON e.id_estudiante = i.id_estudiante
                WHERE i.id_materia = ? AND i.año_academico = ? AND i.estado <> 'Retirada'
                GROUP BY e.id_estudiante
                ORDER BY e.primer_apellido, e.primer_nombre
                """, columns(2), subjectId, year);
    }
}
