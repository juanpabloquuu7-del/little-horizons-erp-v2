package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Attendance;

import java.util.List;

public class AttendanceDaoImpl extends BaseJdbcDao implements AttendanceDao {

    @Override
    public void upsert(Attendance a) {
        update("""
                INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, estado_asistencia, observaciones, registrado_por)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE estado_asistencia = VALUES(estado_asistencia),
                    observaciones = VALUES(observaciones), registrado_por = VALUES(registrado_por)
                """, a.studentId(), a.subjectId(), a.date(), a.status(), nullIfBlank(a.notes()), nullableId(a.recordedBy()));
    }

    @Override
    public List<String[]> historyRows(int studentId) {
        return query("""
                SELECT a.fecha_asistencia, m.nombre_materia, a.estado_asistencia, COALESCE(a.observaciones, '')
                FROM asistencias a JOIN materias m ON m.id_materia = a.id_materia
                WHERE a.id_estudiante = ? ORDER BY a.fecha_asistencia DESC, m.nombre_materia
                """, columns(4), studentId);
    }

    @Override
    public List<String[]> findWeeklyAbsences(int threshold) {
        return query("SELECT e.id_estudiante, " + STUDENT_NAME + ", MIN(a.fecha_asistencia) AS semana_desde, COUNT(*) AS ausencias "
                + """
                FROM asistencias a JOIN estudiantes e ON e.id_estudiante = a.id_estudiante
                WHERE a.estado_asistencia = 'Ausente'
                GROUP BY e.id_estudiante, YEARWEEK(a.fecha_asistencia, 1)
                HAVING COUNT(*) > ?
                ORDER BY semana_desde DESC, ausencias DESC
                """, columns(4), threshold);
    }

    @Override
    public List<String[]> findAccumulated(int minAbsences) {
        return query("SELECT e.id_estudiante, " + STUDENT_NAME + ", "
                + "SUM(a.estado_asistencia = 'Ausente') AS ausencias, SUM(a.estado_asistencia = 'Tardanza') AS tardanzas, "
                + "SUM(a.estado_asistencia = 'Excusa Médica') AS excusas "
                + """
                FROM asistencias a JOIN estudiantes e ON e.id_estudiante = a.id_estudiante
                GROUP BY e.id_estudiante
                HAVING ausencias >= ?
                ORDER BY ausencias DESC
                """, columns(5), minAbsences);
    }
}
