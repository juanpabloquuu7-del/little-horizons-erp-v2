package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class StudentDaoImpl extends BaseJdbcDao implements StudentDao {

    private static final String SELECT = """
            SELECT e.id_estudiante, e.tipo_documento, e.numero_documento, e.primer_nombre, e.segundo_nombre,
                   e.primer_apellido, e.segundo_apellido, e.fecha_nacimiento, e.genero, e.direccion, e.telefono,
                   e.correo_electronico, e.fecha_ingreso, e.estado
            FROM estudiantes e
            """;

    private static Student map(ResultSet rs) throws SQLException {
        return new Student(rs.getInt("id_estudiante"), rs.getString("tipo_documento"), rs.getString("numero_documento"),
                rs.getString("primer_nombre"), rs.getString("segundo_nombre"), rs.getString("primer_apellido"),
                rs.getString("segundo_apellido"), rs.getObject("fecha_nacimiento", LocalDate.class), rs.getString("genero"),
                rs.getString("direccion"), rs.getString("telefono"), rs.getString("correo_electronico"),
                rs.getObject("fecha_ingreso", LocalDate.class), rs.getString("estado"));
    }

    @Override
    public int insert(Student s) {
        return insert("""
                INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
                    segundo_apellido, fecha_nacimiento, genero, direccion, telefono, correo_electronico, fecha_ingreso, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, s.documentType(), s.documentNumber(), s.firstName(), nullIfBlank(s.middleName()), s.lastName(),
                nullIfBlank(s.secondLastName()), s.birthDate(), s.gender(), nullIfBlank(s.address()), nullIfBlank(s.phone()),
                nullIfBlank(s.email()), s.admissionDate(), s.status());
    }

    @Override
    public boolean update(Student s) {
        return update("""
                UPDATE estudiantes SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?,
                    primer_apellido = ?, segundo_apellido = ?, fecha_nacimiento = ?, genero = ?, direccion = ?,
                    telefono = ?, correo_electronico = ? WHERE id_estudiante = ?
                """, s.documentType(), s.documentNumber(), s.firstName(), nullIfBlank(s.middleName()), s.lastName(),
                nullIfBlank(s.secondLastName()), s.birthDate(), s.gender(), nullIfBlank(s.address()), nullIfBlank(s.phone()),
                nullIfBlank(s.email()), s.id()) > 0;
    }

    @Override
    public boolean updateStatus(int studentId, String status) {
        return update("UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?", status, studentId) > 0;
    }

    @Override
    public boolean delete(int studentId) {
        return update("DELETE FROM estudiantes WHERE id_estudiante = ?", studentId) > 0;
    }

    @Override
    public Optional<Student> findById(int studentId) {
        return queryOne(SELECT + " WHERE e.id_estudiante = ?", StudentDaoImpl::map, studentId);
    }

    @Override
    public Optional<Student> findByDocument(String documentNumber) {
        return queryOne(SELECT + " WHERE e.numero_documento = ?", StudentDaoImpl::map, documentNumber);
    }

    @Override
    public List<Student> findAll() {
        return query(SELECT + " ORDER BY e.primer_apellido, e.primer_nombre", StudentDaoImpl::map);
    }

    @Override
    public List<Student> findByGrade(int gradeId, int year) {
        return query(SELECT + """
                JOIN matriculas m ON m.id_estudiante = e.id_estudiante
                WHERE m.id_grado = ? AND m.año_academico = ? AND m.estado_matricula = 'Activa'
                ORDER BY e.primer_apellido, e.primer_nombre
                """, StudentDaoImpl::map, gradeId, year);
    }

    @Override
    public List<Student> findSiblings(int studentId) {
        return query(SELECT + """
                WHERE e.id_estudiante <> ? AND e.id_estudiante IN (
                    SELECT other.id_estudiante FROM estudiante_acudiente mine
                    JOIN estudiante_acudiente other ON other.id_acudiente = mine.id_acudiente
                    WHERE mine.id_estudiante = ?)
                ORDER BY e.fecha_nacimiento
                """, StudentDaoImpl::map, studentId, studentId);
    }

    @Override
    public List<String[]> listWithGrade(int year) {
        return query("SELECT e.id_estudiante, e.numero_documento, " + STUDENT_NAME
                + ", COALESCE(g.nombre_grado, '(sin matrícula)'), e.estado "
                + """
                FROM estudiantes e
                LEFT JOIN matriculas m ON m.id_estudiante = e.id_estudiante AND m.año_academico = ?
                LEFT JOIN grados g ON g.id_grado = m.id_grado
                ORDER BY e.primer_apellido, e.primer_nombre
                """, columns(5), year);
    }
}
