package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Scholarship;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ScholarshipDaoImpl extends BaseJdbcDao implements ScholarshipDao {

    private static Scholarship map(ResultSet rs) throws SQLException {
        return new Scholarship(rs.getInt("id_beca"), rs.getInt("id_estudiante"), rs.getInt("año_academico"),
                rs.getBigDecimal("porcentaje_descuento"), rs.getString("motivo"),
                rs.getObject("fecha_asignacion", LocalDate.class));
    }

    @Override
    public Optional<Scholarship> findByStudentAndYear(int studentId, int academicYear) {
        return queryOne("""
                SELECT id_beca, id_estudiante, año_academico, porcentaje_descuento, motivo, fecha_asignacion
                FROM becas WHERE id_estudiante = ? AND año_academico = ?
                """, ScholarshipDaoImpl::map, studentId, academicYear);
    }

    @Override
    public void save(Scholarship s) {
        update("""
                INSERT INTO becas (id_estudiante, año_academico, porcentaje_descuento, motivo, fecha_asignacion)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE porcentaje_descuento = VALUES(porcentaje_descuento),
                    motivo = VALUES(motivo), fecha_asignacion = VALUES(fecha_asignacion)
                """, s.studentId(), s.academicYear(), s.percent(), nullIfBlank(s.reason()), s.assignedOn());
    }

    @Override
    public int delete(int studentId, int academicYear) {
        return update("DELETE FROM becas WHERE id_estudiante = ? AND año_academico = ?", studentId, academicYear);
    }

    @Override
    public List<String[]> listRows(int academicYear) {
        return query("SELECT e.id_estudiante, " + STUDENT_NAME + ", b.porcentaje_descuento, b.motivo, b.fecha_asignacion "
                + "FROM becas b JOIN estudiantes e ON e.id_estudiante = b.id_estudiante "
                + "WHERE b.año_academico = ? ORDER BY e.primer_apellido, e.primer_nombre", columns(5), academicYear);
    }
}
