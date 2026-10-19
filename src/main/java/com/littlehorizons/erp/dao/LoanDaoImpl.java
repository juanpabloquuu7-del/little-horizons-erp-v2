package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

/**
 * Loans live in two tables (students / teachers). Lending and returning touch the loan table and
 * the resource status, so both run inside one transaction.
 */
public class LoanDaoImpl extends BaseJdbcDao implements LoanDao {

    private static final String INSERT_STUDENT_LOAN = """
            INSERT INTO prestamos_estudiante (id_recurso, id_estudiante, fecha_prestamo, fecha_devolucion_esperada, observaciones)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String INSERT_TEACHER_LOAN = """
            INSERT INTO prestamos_profesor (id_recurso, id_profesor, fecha_prestamo, fecha_devolucion_esperada, observaciones)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String SELECT_STUDENT_RESOURCE =
            "SELECT id_recurso FROM prestamos_estudiante WHERE id_prestamo = ? AND fecha_devolucion_real IS NULL FOR UPDATE";
    private static final String SELECT_TEACHER_RESOURCE =
            "SELECT id_recurso FROM prestamos_profesor WHERE id_prestamo = ? AND fecha_devolucion_real IS NULL FOR UPDATE";
    private static final String RETURN_STUDENT_LOAN =
            "UPDATE prestamos_estudiante SET fecha_devolucion_real = ?, estado_prestamo = 'Devuelto' WHERE id_prestamo = ?";
    private static final String RETURN_TEACHER_LOAN =
            "UPDATE prestamos_profesor SET fecha_devolucion_real = ?, estado_prestamo = 'Devuelto' WHERE id_prestamo = ?";
    private static final String SET_RESOURCE_STATUS = "UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?";
    private static final String MARK_STUDENT_OVERDUE = """
            UPDATE prestamos_estudiante SET estado_prestamo = 'Vencido'
            WHERE estado_prestamo = 'Activo' AND fecha_devolucion_real IS NULL AND fecha_devolucion_esperada < CURDATE()
            """;
    private static final String MARK_TEACHER_OVERDUE = """
            UPDATE prestamos_profesor SET estado_prestamo = 'Vencido'
            WHERE estado_prestamo = 'Activo' AND fecha_devolucion_real IS NULL AND fecha_devolucion_esperada < CURDATE()
            """;

    private static final String UNRETURNED_LOANS = """
            SELECT x.id_prestamo, x.tipo, x.prestatario, x.codigo_recurso, x.nombre_recurso, x.fecha_prestamo,
                   x.fecha_devolucion_esperada, x.estado_prestamo, DATEDIFF(CURDATE(), x.fecha_devolucion_esperada) AS dias_atraso
            FROM (
                SELECT l.id_prestamo, 'Estudiante' AS tipo,
                       CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) AS prestatario,
                       r.codigo_recurso, r.nombre_recurso, l.fecha_prestamo, l.fecha_devolucion_esperada,
                       l.estado_prestamo, l.fecha_devolucion_real
                FROM prestamos_estudiante l
                JOIN estudiantes e ON e.id_estudiante = l.id_estudiante
                JOIN recursos r ON r.id_recurso = l.id_recurso
                UNION ALL
                SELECT l.id_prestamo, 'Profesor',
                       CONCAT_WS(' ', p.primer_nombre, p.segundo_nombre, p.primer_apellido, p.segundo_apellido),
                       r.codigo_recurso, r.nombre_recurso, l.fecha_prestamo, l.fecha_devolucion_esperada,
                       l.estado_prestamo, l.fecha_devolucion_real
                FROM prestamos_profesor l
                JOIN profesores p ON p.id_profesor = l.id_profesor
                JOIN recursos r ON r.id_recurso = l.id_recurso
            ) x
            WHERE x.fecha_devolucion_real IS NULL
            """;

    @Override
    public int lend(Loan loan) {
        String insertSql = loan.teacherBorrower() ? INSERT_TEACHER_LOAN : INSERT_STUDENT_LOAN;
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps, loan.resourceId(), loan.borrowerId(), loan.loanDate(), loan.expectedReturn(),
                            nullIfBlank(loan.notes()));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        id = keys.next() ? keys.getInt(1) : 0;
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(SET_RESOURCE_STATUS)) {
                    bind(ps, Resource.LOANED, loan.resourceId());
                    ps.executeUpdate();
                }
                con.commit();
                return id;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw translate(insertSql, e);
        }
    }

    @Override
    public boolean giveBack(int loanId, boolean teacherBorrower, LocalDate returnDate) {
        String selectSql = teacherBorrower ? SELECT_TEACHER_RESOURCE : SELECT_STUDENT_RESOURCE;
        String returnSql = teacherBorrower ? RETURN_TEACHER_LOAN : RETURN_STUDENT_LOAN;
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                int resourceId;
                try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                    bind(ps, loanId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        resourceId = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(returnSql)) {
                    bind(ps, returnDate, loanId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(SET_RESOURCE_STATUS)) {
                    bind(ps, Resource.AVAILABLE, resourceId);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw translate(returnSql, e);
        }
    }

    @Override
    public int markOverdue() {
        return update(MARK_STUDENT_OVERDUE) + update(MARK_TEACHER_OVERDUE);
    }

    @Override
    public List<String[]> findActive() {
        return query(UNRETURNED_LOANS + " ORDER BY x.fecha_devolucion_esperada", columns(8));
    }

    @Override
    public List<String[]> findOverdue() {
        return query(UNRETURNED_LOANS + " AND x.fecha_devolucion_esperada < CURDATE() ORDER BY dias_atraso DESC", columns(9));
    }
}
