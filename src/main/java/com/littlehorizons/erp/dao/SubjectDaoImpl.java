package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class SubjectDaoImpl extends BaseJdbcDao implements SubjectDao {

    private static final String SELECT =
            "SELECT id_materia, codigo_materia, nombre_materia, descripcion, id_grado, horas_semanales, esta_activa FROM materias";

    private static Subject map(ResultSet rs) throws SQLException {
        return new Subject(rs.getInt("id_materia"), rs.getString("codigo_materia"), rs.getString("nombre_materia"),
                rs.getString("descripcion"), rs.getInt("id_grado"), rs.getInt("horas_semanales"), rs.getBoolean("esta_activa"));
    }

    @Override
    public int insert(Subject s) {
        return insert("INSERT INTO materias (codigo_materia, nombre_materia, descripcion, id_grado, horas_semanales, esta_activa) VALUES (?, ?, ?, ?, ?, ?)",
                s.code(), s.name(), nullIfBlank(s.description()), s.gradeId(), s.weeklyHours(), s.active());
    }

    @Override
    public boolean setActive(int subjectId, boolean active) {
        return update("UPDATE materias SET esta_activa = ? WHERE id_materia = ?", active, subjectId) > 0;
    }

    @Override
    public Optional<Subject> findById(int subjectId) {
        return queryOne(SELECT + " WHERE id_materia = ?", SubjectDaoImpl::map, subjectId);
    }

    @Override
    public Optional<Subject> findByCode(String code) {
        return queryOne(SELECT + " WHERE codigo_materia = ?", SubjectDaoImpl::map, code);
    }

    @Override
    public List<Subject> findByGrade(int gradeId) {
        return query(SELECT + " WHERE id_grado = ? ORDER BY nombre_materia", SubjectDaoImpl::map, gradeId);
    }

    @Override
    public List<String[]> listRows(int gradeId) {
        return query("""
                SELECT m.id_materia, m.codigo_materia, m.nombre_materia, g.nombre_grado, m.horas_semanales,
                       IF(m.esta_activa, 'Activa', 'Inactiva')
                FROM materias m JOIN grados g ON g.id_grado = m.id_grado
                WHERE (? = 0 OR m.id_grado = ?)
                ORDER BY g.nivel_numerico, m.nombre_materia
                """, columns(6), gradeId, gradeId);
    }
}
