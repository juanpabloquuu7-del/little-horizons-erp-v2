package com.littlehorizons.erp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.util.CustomExceptions.DocumentException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

/** Renders the simulated DIAN electronic invoice as a PDF (PDFBox) with a QR code (ZXing). */
public final class PdfGenerator {

    /** Everything the invoice document prints. */
    public record InvoiceData(String issuerName, String issuerNit, String issuerAddress, String issuerPhone,
                              String issuerEmail, String resolution, Invoice invoice,
                              String clientName, String clientDocument, String clientEmail, String clientAddress,
                              String studentName, String gradeName, String conceptName, BigDecimal conceptBase,
                              BigDecimal discount, BigDecimal amountPaid, String paymentMethod,
                              String receiptNumber, LocalDate paymentDate) {
    }

    private static final PDFont REGULAR = PDType1Font.HELVETICA;
    private static final PDFont BOLD = PDType1Font.HELVETICA_BOLD;
    private static final float MARGIN = 50f;
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    private PdfGenerator() {
    }

    public static void generateInvoice(InvoiceData data, Path output) {
        try {
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    draw(cs, page.getMediaBox(), data);
                }
                document.save(output.toFile());
            }
        } catch (IOException | WriterException e) {
            LoggerUtility.logError("PdfGenerator.generateInvoice", e);
            throw new DocumentException("No se pudo generar la factura en PDF: " + e.getMessage(), e);
        }
    }

    private static void draw(PDPageContentStream cs, PDRectangle box, InvoiceData d) throws IOException, WriterException {
        Invoice inv = d.invoice();
        float width = box.getWidth() - 2 * MARGIN;
        float y = box.getHeight() - MARGIN;

        // Issuer header
        text(cs, BOLD, 16, MARGIN, y, d.issuerName());
        y -= 16;
        text(cs, REGULAR, 10, MARGIN, y, "NIT: " + d.issuerNit());
        y -= 13;
        text(cs, REGULAR, 10, MARGIN, y, d.issuerAddress());
        y -= 13;
        text(cs, REGULAR, 10, MARGIN, y, "Tel: " + d.issuerPhone() + "   Correo: " + d.issuerEmail());
        y -= 13;
        text(cs, REGULAR, 8, MARGIN, y, d.resolution());
        y -= 12;
        line(cs, MARGIN, y, MARGIN + width, y);

        // Invoice title
        y -= 24;
        text(cs, BOLD, 14, MARGIN, y, "FACTURA ELECTRÓNICA DE VENTA");
        text(cs, BOLD, 12, MARGIN + width - 110, y, "No. " + inv.number());
        y -= 16;
        text(cs, REGULAR, 10, MARGIN, y, "Fecha de emisión: " + inv.issueDate());
        text(cs, REGULAR, 10, MARGIN + 250, y, "Recibo: " + d.receiptNumber());

        // Client
        y -= 26;
        text(cs, BOLD, 11, MARGIN, y, "CLIENTE (ACUDIENTE RESPONSABLE DEL PAGO)");
        y -= 14;
        text(cs, REGULAR, 10, MARGIN, y, "Nombre: " + d.clientName() + "     Documento: " + d.clientDocument());
        y -= 13;
        text(cs, REGULAR, 10, MARGIN, y, "Dirección: " + nvl(d.clientAddress()) + "     Correo: " + nvl(d.clientEmail()));
        y -= 13;
        text(cs, REGULAR, 10, MARGIN, y, "Estudiante: " + d.studentName() + "     Grado: " + d.gradeName());
        y -= 13;
        text(cs, REGULAR, 10, MARGIN, y, "Forma de pago: " + d.paymentMethod() + "     Fecha de pago: " + d.paymentDate());

        // Items table
        y -= 28;
        cs.setNonStrokingColor(new Color(225, 235, 245));
        cs.addRect(MARGIN, y - 4, width, 18);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);
        text(cs, BOLD, 10, MARGIN + 4, y, "Descripción");
        text(cs, BOLD, 10, MARGIN + 290, y, "Cant.");
        text(cs, BOLD, 10, MARGIN + 340, y, "Valor unitario");
        text(cs, BOLD, 10, MARGIN + 435, y, "Subtotal");
        y -= 20;
        text(cs, REGULAR, 10, MARGIN + 4, y, d.conceptName());
        text(cs, REGULAR, 10, MARGIN + 295, y, "1");
        text(cs, REGULAR, 10, MARGIN + 340, y, money(d.conceptBase()));
        text(cs, REGULAR, 10, MARGIN + 435, y, money(d.conceptBase()));
        if (d.discount().signum() > 0) {
            y -= 16;
            text(cs, REGULAR, 10, MARGIN + 4, y, "Descuento aplicado (hermanos / beca)");
            text(cs, REGULAR, 10, MARGIN + 295, y, "1");
            text(cs, REGULAR, 10, MARGIN + 340, y, "-" + money(d.discount()));
            text(cs, REGULAR, 10, MARGIN + 435, y, "-" + money(d.discount()));
        }
        y -= 10;
        line(cs, MARGIN, y, MARGIN + width, y);

        // Totals
        y -= 18;
        text(cs, REGULAR, 10, MARGIN + 300, y, "Subtotal:");
        text(cs, REGULAR, 10, MARGIN + 435, y, money(d.amountPaid()));
        y -= 14;
        text(cs, REGULAR, 10, MARGIN + 300, y, "IVA (0%, servicio educativo):");
        text(cs, REGULAR, 10, MARGIN + 435, y, money(BigDecimal.ZERO));
        y -= 16;
        text(cs, BOLD, 12, MARGIN + 300, y, "TOTAL:");
        text(cs, BOLD, 12, MARGIN + 425, y, money(d.amountPaid()));

        // CUFE + QR
        y -= 40;
        text(cs, BOLD, 10, MARGIN, y, "CUFE (SHA-256, simulado):");
        y -= 13;
        text(cs, REGULAR, 8, MARGIN, y, inv.cufe().substring(0, 32));
        y -= 11;
        text(cs, REGULAR, 8, MARGIN, y, inv.cufe().substring(32));
        y -= 24;
        text(cs, REGULAR, 8, MARGIN, y, "Escanee el código QR para consultar el documento (representación simulada).");

        String qrContent = "NumFac: " + inv.number() + "\nFecFac: " + inv.issueDate()
                + "\nNitFac: " + d.issuerNit() + "\nValTotal: " + d.amountPaid().toPlainString()
                + "\nCUFE: " + inv.cufe()
                + "\nURL: https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=" + inv.cufe();
        drawQr(cs, qrContent, MARGIN, y - 130, 120f);

        text(cs, REGULAR, 7, MARGIN, MARGIN - 20,
                "Documento generado con fines académicos. Simulación de factura electrónica DIAN sin validez fiscal.");
    }

    private static void drawQr(PDPageContentStream cs, String content, float x, float y, float size)
            throws IOException, WriterException {
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0,
                Map.of(EncodeHintType.MARGIN, 0));
        float module = size / matrix.getWidth();
        cs.setNonStrokingColor(Color.BLACK);
        for (int row = 0; row < matrix.getHeight(); row++) {
            for (int col = 0; col < matrix.getWidth(); col++) {
                if (matrix.get(col, row)) {
                    cs.addRect(x + col * module, y + size - (row + 1) * module, module, module);
                }
            }
        }
        cs.fill();
    }

    private static void line(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
        cs.setStrokingColor(Color.GRAY);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private static void text(PDPageContentStream cs, PDFont font, float size, float x, float y, String value)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(value));
        cs.endText();
    }

    private static String money(BigDecimal value) {
        return MONEY.format(value);
    }

    private static String nvl(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /** Helvetica (WinAnsi) cannot encode every character; replace unsupported ones and control characters. */
    private static String sanitize(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            sb.append(c >= 32 && c <= 255 ? c : '?');
        }
        return sb.toString();
    }
}
