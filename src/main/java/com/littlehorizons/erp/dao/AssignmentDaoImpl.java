package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.TeachingAssignment;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class AssignmentDaoImpl extends BaseJdbcDao implements AssignmentDao {

    @Override
    public int insert(TeachingAssignment a) {
        return insert("INSERT INTO asignaciones_profesor (id_profesor, id_materia, año_academico, seccion) VALUES (?, ?, ?, ?)",
                a.teacherId(), a.subjectId(), a.academicYear(), nullIfBlank(a.section()));
    }

    @Override
    public boolean delete(int assignmentId) {
        return update("DELETE FROM asignaciones_profesor WHERE id_asignacion = ?", assignmentId) > 0;
    }

    @Override
    public Optional<TeachingAssignment> findById(int assignmentId) {
        return queryOne("SELECT id_asignacion, id_profesor, id_materia, año_academico, seccion FROM asignaciones_profesor WHERE id_asignacion = ?",
                rs -> new TeachingAssignment(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getString(5)), assignmentId);
    }

    @Override
    public boolean exists(int teacherId, int subjectId, int year, String section) {
        return count("SELECT COUNT(*) FROM asignaciones_profesor WHERE id_profesor = ? AND id_materia = ? AND año_academico = ? AND seccion <=> ?",
                teacherId, subjectId, year, nullIfBlank(section)) > 0;
    }

    @Override
    public boolean isAssigned(int teacherId, int subjectId, int year) {
        return count("SELECT COUNT(*) FROM asignaciones_profesor WHERE id_profesor = ? AND id_materia = ? AND año_academico = ?",
                teacherId, subjectId, year) > 0;
    }

    @Override
    public List<String[]> findByTeacher(int teacherId, int year) {
        return query("""
                SELECT a.id_asignacion, m.codigo_materia, m.nombre_materia, g.nombre_grado, COALESCE(a.seccion, '-'), a.año_academico
                FROM asignaciones_profesor a
                JOIN materias m ON m.id_materia = a.id_materia
                JOIN grados g ON g.id_grado = m.id_grado
                WHERE a.id_profesor = ? AND a.año_academico = ?
                ORDER BY g.nivel_numerico, m.nombre_materia
                """, columns(6), teacherId, year);
    }

    @Override
    public int insertSchedule(ClassSchedule s) {
        return insert("INSERT INTO horarios_clase (id_asignacion, dia_semana, hora_inicio, hora_fin) VALUES (?, ?, ?, ?)",
                s.assignmentId(), s.weekday(), s.startTime(), s.endTime());
    }

    @Override
    public boolean hasConflict(int teacherId, int year, String weekday, LocalTime start, LocalTime end) {
        return count("""
                SELECT COUNT(*) FROM horarios_clase h
                JOIN asignaciones_profesor a ON a.id_asignacion = h.id_asignacion
                WHERE a.id_profesor = ? AND a.año_academico = ? AND h.dia_semana = ?
                  AND h.hora_inicio < ? AND h.hora_fin > ?
                """, teacherId, year, weekday, end, start) > 0;
    }

    @Override
    public List<String[]> findSchedulesByTeacher(int teacherId, int year) {
        return query("""
                SELECT a.id_asignacion, m.nombre_materia, g.nombre_grado, h.dia_semana,
                       TIME_FORMAT(h.hora_inicio, '%H:%i'), TIME_FORMAT(h.hora_fin, '%H:%i')
                FROM horarios_clase h
                JOIN asignaciones_profesor a ON a.id_asignacion = h.id_asignacion
                JOIN materias m ON m.id_materia = a.id_materia
                JOIN grados g ON g.id_grado = m.id_grado
                WHERE a.id_profesor = ? AND a.año_academico = ?
                ORDER BY FIELD(h.dia_semana, 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'), h.hora_inicio
                """, columns(6), teacherId, year);
    }
}
