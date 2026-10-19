package com.littlehorizons.erp.dao;

import java.util.List;

public class ReportDaoImpl extends BaseJdbcDao implements ReportDao {

    @Override
    public List<String[]> studentsPerGrade(int year) {
        return query("""
                SELECT g.nombre_grado, COUNT(m.id_matricula)
                FROM grados g
                LEFT JOIN matriculas m ON m.id_grado = g.id_grado AND m.año_academico = ? AND m.estado_matricula = 'Activa'
                GROUP BY g.id_grado, g.nombre_grado, g.nivel_numerico
                ORDER BY g.nivel_numerico
                """, columns(2), year);
    }

    @Override
    public List<String[]> topStudents(int year, int limit) {
        return query("SELECT e.id_estudiante, " + STUDENT_NAME + ", COALESCE(g.nombre_grado, '-'), "
                + "ROUND(AVG(r.valor_nota), 2) AS promedio, COUNT(*) "
                + """
                FROM registros_academicos r
                JOIN estudiantes e ON e.id_estudiante = r.id_estudiante
                LEFT JOIN matriculas m ON m.id_estudiante = e.id_estudiante AND m.año_academico = r.año_academico
                LEFT JOIN grados g ON g.id_grado = m.id_grado
                WHERE r.año_academico = ? AND r.periodo_academico <> 'Final'
                GROUP BY e.id_estudiante, g.nombre_grado
                ORDER BY promedio DESC, e.primer_apellido
                LIMIT ?
                """, columns(5), year, limit);
    }

    @Override
    public List<String[]> clubParticipation() {
        return query("""
                SELECT c.nombre_club,
                       (SELECT COUNT(*) FROM membresias_club m WHERE m.id_club = c.id_club AND m.estado_membresia = 'Activa') AS inscritos,
                       c.capacidad_maxima,
                       CONCAT(ROUND(100 * (SELECT COUNT(*) FROM membresias_club m
                                           WHERE m.id_club = c.id_club AND m.estado_membresia = 'Activa') / c.capacidad_maxima), '%')
                FROM clubes c WHERE c.esta_activo = TRUE
                ORDER BY inscritos DESC, c.nombre_club
                """, columns(4));
    }

    @Override
    public List<String[]> eventParticipation() {
        return query("""
                SELECT ev.nombre_evento, ev.tipo_evento, ev.fecha_evento, COUNT(pe.id_participacion),
                       COALESCE(SUM(pe.rol_participacion = 'Competidor'), 0)
                FROM eventos ev LEFT JOIN participantes_evento pe ON pe.id_evento = ev.id_evento
                GROUP BY ev.id_evento
                ORDER BY ev.fecha_evento
                """, columns(5));
    }

    @Override
    public List<String[]> incomeByConcept(int year) {
        return query("""
                SELECT c.nombre_concepto, COUNT(p.id_pago), COALESCE(SUM(p.monto_pagado), 0)
                FROM conceptos_pago c
                LEFT JOIN pagos p ON p.id_concepto = c.id_concepto AND YEAR(p.fecha_pago) = ?
                GROUP BY c.id_concepto
                ORDER BY SUM(p.monto_pagado) DESC
                """, columns(3), year);
    }

    @Override
    public List<String[]> incomeByMonth(int year) {
        return query("""
                SELECT DATE_FORMAT(fecha_pago, '%Y-%m') AS mes, COUNT(*), SUM(monto_pagado)
                FROM pagos WHERE YEAR(fecha_pago) = ?
                GROUP BY mes ORDER BY mes
                """, columns(3), year);
    }

    @Override
    public List<String[]> resourceStatus() {
        return query("""
                SELECT c.nombre_categoria,
                       COALESCE(SUM(r.estado_recurso = 'Disponible'), 0), COALESCE(SUM(r.estado_recurso = 'Prestado'), 0),
                       COALESCE(SUM(r.estado_recurso = 'En Mantenimiento'), 0), COALESCE(SUM(r.estado_recurso = 'Dado de Baja'), 0)
                FROM categorias_recurso c LEFT JOIN recursos r ON r.id_categoria = c.id_categoria
                GROUP BY c.id_categoria
                ORDER BY c.id_categoria
                """, columns(5));
    }
}
