package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SchoolEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class EventDaoImpl extends BaseJdbcDao implements EventDao {

    private static final String SELECT = """
            SELECT id_evento, nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, tiene_competencia,
                   id_organizador, esta_activo
            FROM eventos
            """;

    private static SchoolEvent map(ResultSet rs) throws SQLException {
        return new SchoolEvent(rs.getInt("id_evento"), rs.getString("nombre_evento"), rs.getString("descripcion"),
                rs.getString("tipo_evento"), rs.getObject("fecha_evento", LocalDate.class), rs.getString("lugar"),
                rs.getBoolean("tiene_competencia"), rs.getInt("id_organizador"), rs.getBoolean("esta_activo"));
    }

    @Override
    public int insert(SchoolEvent e) {
        return insert("""
                INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, tiene_competencia, id_organizador, esta_activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, e.name(), nullIfBlank(e.description()), e.type(), e.date(), nullIfBlank(e.location()),
                e.hasCompetition(), nullableId(e.organizerId()), e.active());
    }

    @Override
    public boolean setActive(int eventId, boolean active) {
        return update("UPDATE eventos SET esta_activo = ? WHERE id_evento = ?", active, eventId) > 0;
    }

    @Override
    public Optional<SchoolEvent> findById(int eventId) {
        return queryOne(SELECT + " WHERE id_evento = ?", EventDaoImpl::map, eventId);
    }

    @Override
    public List<SchoolEvent> findAll() {
        return query(SELECT + " ORDER BY fecha_evento", EventDaoImpl::map);
    }

    @Override
    public List<SchoolEvent> findByType(String type) {
        return query(SELECT + " WHERE tipo_evento = ? ORDER BY fecha_evento", EventDaoImpl::map, type);
    }

    @Override
    public void saveParticipant(int eventId, int studentId, String role, String position) {
        update("""
                INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, posicion_obtenida)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE rol_participacion = VALUES(rol_participacion), posicion_obtenida = VALUES(posicion_obtenida)
                """, eventId, studentId, role, nullIfBlank(position));
    }

    @Override
    public List<String[]> findParticipants(int eventId) {
        return query("SELECT e.id_estudiante, " + STUDENT_NAME + ", COALESCE(g.nombre_grado, '-'), pe.rol_participacion, COALESCE(pe.posicion_obtenida, '-') "
                + """
                FROM participantes_evento pe
                JOIN estudiantes e ON e.id_estudiante = pe.id_estudiante
                LEFT JOIN matriculas m ON m.id_estudiante = e.id_estudiante AND m.estado_matricula = 'Activa'
                LEFT JOIN grados g ON g.id_grado = m.id_grado
                WHERE pe.id_evento = ?
                ORDER BY pe.posicion_obtenida IS NULL, pe.posicion_obtenida, e.primer_apellido
                """, columns(5), eventId);
    }
}
