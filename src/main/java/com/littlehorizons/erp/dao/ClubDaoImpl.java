package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ClubDaoImpl extends BaseJdbcDao implements ClubDao {

    @Override
    public int insert(Club c) {
        return insert("INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, horario_reunion, esta_activo) VALUES (?, ?, ?, ?, ?, ?)",
                c.name(), nullIfBlank(c.description()), nullableId(c.supervisorId()), c.maxCapacity(),
                nullIfBlank(c.meetingSchedule()), c.active());
    }

    @Override
    public boolean setActive(int clubId, boolean active) {
        return update("UPDATE clubes SET esta_activo = ? WHERE id_club = ?", active, clubId) > 0;
    }

    @Override
    public Optional<Club> findById(int clubId) {
        return queryOne("""
                SELECT id_club, nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, horario_reunion, esta_activo
                FROM clubes WHERE id_club = ?
                """, rs -> new Club(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getInt(5),
                        rs.getString(6), rs.getBoolean(7)), clubId);
    }

    @Override
    public int countActiveMembers(int clubId) {
        return count("SELECT COUNT(*) FROM membresias_club WHERE id_club = ? AND estado_membresia = 'Activa'", clubId);
    }

    @Override
    public boolean isActiveMember(int clubId, int studentId) {
        return count("SELECT COUNT(*) FROM membresias_club WHERE id_club = ? AND id_estudiante = ? AND estado_membresia = 'Activa'",
                clubId, studentId) > 0;
    }

    @Override
    public boolean enrollIfRoom(int clubId, int studentId, LocalDate date) {
        return update("""
                INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso, estado_membresia)
                SELECT ?, c.id_club, ?, 'Activa' FROM clubes c
                WHERE c.id_club = ? AND c.esta_activo = TRUE
                  AND (SELECT COUNT(*) FROM membresias_club m
                       WHERE m.id_club = c.id_club AND m.estado_membresia = 'Activa') < c.capacidad_maxima
                """, studentId, date, clubId) > 0;
    }

    @Override
    public boolean withdraw(int clubId, int studentId, LocalDate date, String reason) {
        return update("""
                UPDATE membresias_club SET estado_membresia = 'Retirada', fecha_retiro = ?, motivo_retiro = ?
                WHERE id_club = ? AND id_estudiante = ? AND estado_membresia = 'Activa'
                """, date, nullIfBlank(reason), clubId, studentId) > 0;
    }

    @Override
    public List<String[]> listRows() {
        return query("SELECT c.id_club, c.nombre_club, "
                + "(SELECT COUNT(*) FROM membresias_club m WHERE m.id_club = c.id_club AND m.estado_membresia = 'Activa') AS inscritos, "
                + "c.capacidad_maxima, COALESCE(" + TEACHER_NAME + ", '(sin asignar)'), "
                + "IF(c.esta_activo, 'Activo', 'Inactivo'), COALESCE(c.horario_reunion, '') "
                + """
                FROM clubes c LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor
                ORDER BY c.nombre_club
                """, columns(7));
    }

    @Override
    public List<String[]> findMembers(int clubId) {
        return query("SELECT e.id_estudiante, " + STUDENT_NAME + ", m.fecha_ingreso "
                + """
                FROM membresias_club m JOIN estudiantes e ON e.id_estudiante = m.id_estudiante
                WHERE m.id_club = ? AND m.estado_membresia = 'Activa'
                ORDER BY e.primer_apellido, e.primer_nombre
                """, columns(3), clubId);
    }
}
