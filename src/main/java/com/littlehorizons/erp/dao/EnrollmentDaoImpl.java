package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class EnrollmentDaoImpl extends BaseJdbcDao implements EnrollmentDao {

    @Override
    public int insert(Enrollment e) {
        return insert("INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula) VALUES (?, ?, ?, ?, ?)",
                e.studentId(), e.gradeId(), e.academicYear(), e.status(), e.enrollmentDate());
    }

    @Override
    public Optional<Enrollment> findByStudentAndYear(int studentId, int year) {
        return queryOne("""
                SELECT id_matricula, id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, fecha_retiro
                FROM matriculas WHERE id_estudiante = ? AND año_academico = ?
                """, rs -> new Enrollment(rs.getInt("id_matricula"), rs.getInt("id_estudiante"), rs.getInt("id_grado"),
                        rs.getInt("año_academico"), rs.getString("estado_matricula"),
                        rs.getObject("fecha_matricula", LocalDate.class), rs.getObject("fecha_retiro", LocalDate.class)),
                studentId, year);
    }

    @Override
    public boolean changeGrade(int enrollmentId, int gradeId) {
        return update("UPDATE matriculas SET id_grado = ? WHERE id_matricula = ?", gradeId, enrollmentId) > 0;
    }

    @Override
    public boolean updateStatus(int enrollmentId, String status, LocalDate withdrawalDate) {
        return update("UPDATE matriculas SET estado_matricula = ?, fecha_retiro = ? WHERE id_matricula = ?",
                status, withdrawalDate, enrollmentId) > 0;
    }

    @Override
    public List<String[]> historyRows(int studentId) {
        return query("""
                SELECT m.año_academico, g.nombre_grado, m.estado_matricula, m.fecha_matricula, COALESCE(m.fecha_retiro, '-')
                FROM matriculas m JOIN grados g ON g.id_grado = m.id_grado
                WHERE m.id_estudiante = ? ORDER BY m.año_academico DESC
                """, columns(5), studentId);
    }
}
