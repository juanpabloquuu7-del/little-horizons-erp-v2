package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class AcademicRecordDaoImpl extends BaseJdbcDao implements AcademicRecordDao {

    private static final String SELECT = """
            SELECT r.id_registro, r.id_estudiante, r.id_materia, r.id_profesor, r.año_academico, r.periodo_academico,
                   r.valor_nota, r.observaciones, r.fecha_registro
            FROM registros_academicos r
            """;

    private static AcademicRecord map(ResultSet rs) throws SQLException {
        return new AcademicRecord(rs.getInt("id_registro"), rs.getInt("id_estudiante"), rs.getInt("id_materia"),
                rs.getInt("id_profesor"), rs.getInt("año_academico"), rs.getString("periodo_academico"),
                rs.getBigDecimal("valor_nota"), rs.getString("observaciones"), rs.getObject("fecha_registro", LocalDate.class));
    }

    @Override
    public void upsert(AcademicRecord r) {
        update("""
                INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico,
                    valor_nota, observaciones, fecha_registro)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE valor_nota = VALUES(valor_nota), id_profesor = VALUES(id_profesor),
                    observaciones = VALUES(observaciones), fecha_registro = VALUES(fecha_registro)
                """, r.studentId(), r.subjectId(), r.teacherId(), r.academicYear(), r.period(), r.score(),
                nullIfBlank(r.notes()), r.recordedOn());
    }

    @Override
    public List<AcademicRecord> findByStudent(int studentId) {
        return query(SELECT + " WHERE r.id_estudiante = ? ORDER BY r.año_academico, r.periodo_academico, r.id_materia",
                AcademicRecordDaoImpl::map, studentId);
    }

    @Override
    public List<AcademicRecord> findByGradeAndYear(int gradeId, int year) {
        return query(SELECT + " JOIN materias m ON m.id_materia = r.id_materia WHERE m.id_grado = ? AND r.año_academico = ?",
                AcademicRecordDaoImpl::map, gradeId, year);
    }

    @Override
    public List<String[]> historyRows(int studentId) {
        return query("SELECT r.año_academico, r.periodo_academico, m.codigo_materia, m.nombre_materia, r.valor_nota, "
                + "CONCAT_WS(' ', p.primer_nombre, p.primer_apellido) "
                + """
                FROM registros_academicos r
                JOIN materias m ON m.id_materia = r.id_materia
                JOIN profesores p ON p.id_profesor = r.id_profesor
                WHERE r.id_estudiante = ?
                ORDER BY r.año_academico, FIELD(r.periodo_academico, 'I', 'II', 'III', 'IV', 'Final'), m.nombre_materia
                """, columns(6), studentId);
    }
}
