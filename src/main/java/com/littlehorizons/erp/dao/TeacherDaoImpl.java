package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Teacher;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class TeacherDaoImpl extends BaseJdbcDao implements TeacherDao {

    private static final String SELECT = """
            SELECT id_profesor, tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
                   segundo_apellido, correo_electronico, telefono, especializacion, fecha_contratacion, estado
            FROM profesores
            """;

    private static Teacher map(ResultSet rs) throws SQLException {
        return new Teacher(rs.getInt("id_profesor"), rs.getString("tipo_documento"), rs.getString("numero_documento"),
                rs.getString("primer_nombre"), rs.getString("segundo_nombre"), rs.getString("primer_apellido"),
                rs.getString("segundo_apellido"), rs.getString("correo_electronico"), rs.getString("telefono"),
                rs.getString("especializacion"), rs.getObject("fecha_contratacion", LocalDate.class), rs.getString("estado"));
    }

    @Override
    public int insert(Teacher t) {
        return insert("""
                INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
                    segundo_apellido, correo_electronico, telefono, especializacion, fecha_contratacion, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, t.documentType(), t.documentNumber(), t.firstName(), nullIfBlank(t.middleName()), t.lastName(),
                nullIfBlank(t.secondLastName()), t.email(), nullIfBlank(t.phone()), nullIfBlank(t.specialization()),
                t.hireDate(), t.status());
    }

    @Override
    public boolean update(Teacher t) {
        return update("""
                UPDATE profesores SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?,
                    primer_apellido = ?, segundo_apellido = ?, correo_electronico = ?, telefono = ?, especializacion = ?
                WHERE id_profesor = ?
                """, t.documentType(), t.documentNumber(), t.firstName(), nullIfBlank(t.middleName()), t.lastName(),
                nullIfBlank(t.secondLastName()), t.email(), nullIfBlank(t.phone()), nullIfBlank(t.specialization()), t.id()) > 0;
    }

    @Override
    public boolean updateStatus(int teacherId, String status) {
        return update("UPDATE profesores SET estado = ? WHERE id_profesor = ?", status, teacherId) > 0;
    }

    @Override
    public boolean delete(int teacherId) {
        return update("DELETE FROM profesores WHERE id_profesor = ?", teacherId) > 0;
    }

    @Override
    public Optional<Teacher> findById(int teacherId) {
        return queryOne(SELECT + " WHERE id_profesor = ?", TeacherDaoImpl::map, teacherId);
    }

    @Override
    public Optional<Teacher> findByDocument(String documentNumber) {
        return queryOne(SELECT + " WHERE numero_documento = ?", TeacherDaoImpl::map, documentNumber);
    }

    @Override
    public List<Teacher> findAll() {
        return query(SELECT + " ORDER BY primer_apellido, primer_nombre", TeacherDaoImpl::map);
    }
}
