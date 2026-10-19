package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class GuardianDaoImpl extends BaseJdbcDao implements GuardianDao {

    private static final String SELECT = """
            SELECT a.id_acudiente, a.tipo_documento, a.numero_documento, a.primer_nombre, a.segundo_nombre,
                   a.primer_apellido, a.segundo_apellido, a.parentesco, a.telefono_principal, a.correo_electronico,
                   a.direccion, a.es_responsable_financiero
            FROM acudientes a
            """;

    private static Guardian map(ResultSet rs) throws SQLException {
        return new Guardian(rs.getInt("id_acudiente"), rs.getString("tipo_documento"), rs.getString("numero_documento"),
                rs.getString("primer_nombre"), rs.getString("segundo_nombre"), rs.getString("primer_apellido"),
                rs.getString("segundo_apellido"), rs.getString("parentesco"), rs.getString("telefono_principal"),
                rs.getString("correo_electronico"), rs.getString("direccion"), rs.getBoolean("es_responsable_financiero"));
    }

    @Override
    public int insert(Guardian g) {
        return insert("""
                INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
                    segundo_apellido, parentesco, telefono_principal, correo_electronico, direccion, es_responsable_financiero)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, g.documentType(), g.documentNumber(), g.firstName(), nullIfBlank(g.middleName()), g.lastName(),
                nullIfBlank(g.secondLastName()), g.relationship(), g.phone(), nullIfBlank(g.email()),
                nullIfBlank(g.address()), g.financialResponsible());
    }

    @Override
    public boolean update(Guardian g) {
        return update("""
                UPDATE acudientes SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?,
                    primer_apellido = ?, segundo_apellido = ?, parentesco = ?, telefono_principal = ?,
                    correo_electronico = ?, direccion = ?, es_responsable_financiero = ? WHERE id_acudiente = ?
                """, g.documentType(), g.documentNumber(), g.firstName(), nullIfBlank(g.middleName()), g.lastName(),
                nullIfBlank(g.secondLastName()), g.relationship(), g.phone(), nullIfBlank(g.email()),
                nullIfBlank(g.address()), g.financialResponsible(), g.id()) > 0;
    }

    @Override
    public boolean delete(int guardianId) {
        return update("DELETE FROM acudientes WHERE id_acudiente = ?", guardianId) > 0;
    }

    @Override
    public Optional<Guardian> findById(int guardianId) {
        return queryOne(SELECT + " WHERE a.id_acudiente = ?", GuardianDaoImpl::map, guardianId);
    }

    @Override
    public Optional<Guardian> findByDocument(String documentNumber) {
        return queryOne(SELECT + " WHERE a.numero_documento = ?", GuardianDaoImpl::map, documentNumber);
    }

    @Override
    public List<Guardian> findAll() {
        return query(SELECT + " ORDER BY a.primer_apellido, a.primer_nombre", GuardianDaoImpl::map);
    }

    @Override
    public List<Guardian> findByStudent(int studentId) {
        return query(SELECT + """
                JOIN estudiante_acudiente ea ON ea.id_acudiente = a.id_acudiente
                WHERE ea.id_estudiante = ?
                ORDER BY ea.es_principal DESC, a.es_responsable_financiero DESC, a.id_acudiente
                """, GuardianDaoImpl::map, studentId);
    }

    @Override
    public void link(int studentId, int guardianId, boolean principal, boolean emergencyContact) {
        update("""
                INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, es_contacto_emergencia)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE es_principal = VALUES(es_principal), es_contacto_emergencia = VALUES(es_contacto_emergencia)
                """, studentId, guardianId, principal, emergencyContact);
    }

    @Override
    public void clearPrincipal(int studentId) {
        update("UPDATE estudiante_acudiente SET es_principal = FALSE WHERE id_estudiante = ?", studentId);
    }

    @Override
    public boolean unlink(int studentId, int guardianId) {
        return update("DELETE FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?",
                studentId, guardianId) > 0;
    }

    @Override
    public int countStudents(int guardianId) {
        return count("SELECT COUNT(*) FROM estudiante_acudiente WHERE id_acudiente = ?", guardianId);
    }
}
