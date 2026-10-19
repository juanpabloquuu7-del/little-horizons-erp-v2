package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Invoice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class InvoiceDaoImpl extends BaseJdbcDao implements InvoiceDao {

    @Override
    public int insert(Invoice i) {
        return insert("INSERT INTO facturas (numero_factura, id_pago, fecha_emision, cufe, nombre_archivo_pdf, ruta_archivo) VALUES (?, ?, ?, ?, ?, ?)",
                i.number(), i.paymentId(), i.issueDate(), i.cufe(), i.pdfName(), i.pdfPath());
    }

    @Override
    public boolean updateFile(int invoiceId, String pdfName, String pdfPath) {
        return update("UPDATE facturas SET nombre_archivo_pdf = ?, ruta_archivo = ? WHERE id_factura = ?",
                pdfName, pdfPath, invoiceId) > 0;
    }

    @Override
    public Optional<Invoice> findByPaymentId(int paymentId) {
        return queryOne("""
                SELECT id_factura, numero_factura, id_pago, fecha_emision, cufe, nombre_archivo_pdf, ruta_archivo
                FROM facturas WHERE id_pago = ? ORDER BY id_factura LIMIT 1
                """, rs -> new Invoice(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getObject(4, LocalDate.class),
                        rs.getString(5), rs.getString(6), rs.getString(7)), paymentId);
    }

    @Override
    public List<String[]> listRows() {
        return query("SELECT f.numero_factura, f.fecha_emision, p.numero_recibo, " + STUDENT_NAME + ", COALESCE(f.nombre_archivo_pdf, '-') "
                + """
                FROM facturas f JOIN pagos p ON p.id_pago = f.id_pago
                JOIN estudiantes e ON e.id_estudiante = p.id_estudiante
                ORDER BY f.id_factura DESC
                """, columns(5));
    }

    @Override
    public int nextSequence() {
        return count("SELECT COALESCE(MAX(id_factura), 0) + 1 FROM facturas");
    }
}
