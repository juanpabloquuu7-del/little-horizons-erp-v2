package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class PaymentDaoImpl extends BaseJdbcDao implements PaymentDao {

    private static final String SELECT = """
            SELECT id_pago, id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, metodo_pago,
                   numero_recibo, año_academico, periodo_academico, notas
            FROM pagos
            """;

    private static final String SELECT_CONCEPT = """
            SELECT id_concepto, codigo_concepto, nombre_concepto, descripcion, monto_base, es_recurrente, esta_activo
            FROM conceptos_pago
            """;

    private static Payment map(ResultSet rs) throws SQLException {
        return new Payment(rs.getInt("id_pago"), rs.getInt("id_estudiante"), rs.getInt("id_acudiente"),
                rs.getInt("id_concepto"), rs.getBigDecimal("monto_pagado"), rs.getObject("fecha_pago", LocalDate.class),
                rs.getString("metodo_pago"), rs.getString("numero_recibo"), rs.getInt("año_academico"),
                rs.getString("periodo_academico"), rs.getString("notas"));
    }

    private static PaymentConcept mapConcept(ResultSet rs) throws SQLException {
        return new PaymentConcept(rs.getInt("id_concepto"), rs.getString("codigo_concepto"), rs.getString("nombre_concepto"),
                rs.getString("descripcion"), rs.getBigDecimal("monto_base"), rs.getBoolean("es_recurrente"),
                rs.getBoolean("esta_activo"));
    }

    @Override
    public List<PaymentConcept> findActiveConcepts() {
        return query(SELECT_CONCEPT + " WHERE esta_activo = TRUE ORDER BY id_concepto", PaymentDaoImpl::mapConcept);
    }

    @Override
    public Optional<PaymentConcept> findConceptById(int conceptId) {
        return queryOne(SELECT_CONCEPT + " WHERE id_concepto = ?", PaymentDaoImpl::mapConcept, conceptId);
    }

    @Override
    public int insert(Payment p) {
        return insert("""
                INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, metodo_pago,
                    numero_recibo, año_academico, periodo_academico, notas)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, p.studentId(), nullableId(p.guardianId()), p.conceptId(), p.amountPaid(), p.paymentDate(),
                p.method(), p.receiptNumber(), p.academicYear(), p.period(), nullIfBlank(p.notes()));
    }

    @Override
    public Optional<Payment> findById(int paymentId) {
        return queryOne(SELECT + " WHERE id_pago = ?", PaymentDaoImpl::map, paymentId);
    }

    @Override
    public int nextSequence() {
        return count("SELECT COALESCE(MAX(id_pago), 0) + 1 FROM pagos");
    }

    @Override
    public List<String[]> historyByStudent(int studentId) {
        return query("""
                SELECT p.id_pago, p.numero_recibo, p.fecha_pago, c.nombre_concepto, p.monto_pagado, p.metodo_pago,
                       CONCAT(COALESCE(p.periodo_academico, '-'), ' / ', COALESCE(p.año_academico, '-'))
                FROM pagos p JOIN conceptos_pago c ON c.id_concepto = p.id_concepto
                WHERE p.id_estudiante = ? ORDER BY p.fecha_pago DESC, p.id_pago DESC
                """, columns(7), studentId);
    }

    @Override
    public List<String[]> historyByGuardian(int guardianId) {
        return query("SELECT pg.id_pago, pg.numero_recibo, pg.fecha_pago, " + STUDENT_NAME + ", c.nombre_concepto, pg.monto_pagado, pg.metodo_pago "
                + """
                FROM pagos pg
                JOIN estudiantes e ON e.id_estudiante = pg.id_estudiante
                JOIN conceptos_pago c ON c.id_concepto = pg.id_concepto
                WHERE pg.id_estudiante IN (SELECT id_estudiante FROM estudiante_acudiente WHERE id_acudiente = ?)
                ORDER BY pg.fecha_pago DESC, pg.id_pago DESC
                """, columns(7), guardianId);
    }

    @Override
    public List<String[]> findDelinquent(String conceptCode, int year, int days) {
        return query("SELECT e.id_estudiante, " + STUDENT_NAME + ", COALESCE(g.nombre_grado, '-'), "
                + "COALESCE(MAX(p.fecha_pago), 'Sin pagos'), "
                + "DATEDIFF(CURDATE(), COALESCE(MAX(p.fecha_pago), e.fecha_ingreso)) AS dias, "
                + """
                COALESCE((SELECT a.telefono_principal FROM estudiante_acudiente ea
                          JOIN acudientes a ON a.id_acudiente = ea.id_acudiente
                          WHERE ea.id_estudiante = e.id_estudiante
                          ORDER BY ea.es_principal DESC LIMIT 1), '-')
                FROM estudiantes e
                LEFT JOIN pagos p ON p.id_estudiante = e.id_estudiante
                     AND p.id_concepto = (SELECT id_concepto FROM conceptos_pago WHERE codigo_concepto = ?)
                LEFT JOIN matriculas m ON m.id_estudiante = e.id_estudiante AND m.año_academico = ? AND m.estado_matricula = 'Activa'
                LEFT JOIN grados g ON g.id_grado = m.id_grado
                WHERE e.estado = 'Activo'
                GROUP BY e.id_estudiante, g.nombre_grado
                HAVING dias > ?
                ORDER BY dias DESC
                """, columns(6), conceptCode, year, days);
    }
}
