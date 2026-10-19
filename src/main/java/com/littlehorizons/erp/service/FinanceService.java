package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.EnrollmentDaoImpl;
import com.littlehorizons.erp.dao.GradeLevelDao;
import com.littlehorizons.erp.dao.GradeLevelDaoImpl;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.GuardianDaoImpl;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.InvoiceDaoImpl;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.PaymentDaoImpl;
import com.littlehorizons.erp.dao.ScholarshipDao;
import com.littlehorizons.erp.dao.ScholarshipDaoImpl;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentDaoImpl;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Scholarship;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.CustomExceptions.DocumentException;
import com.littlehorizons.erp.util.CustomExceptions.NotFoundException;
import com.littlehorizons.erp.util.LoggerUtility;
import com.littlehorizons.erp.util.PdfGenerator;
import com.littlehorizons.erp.util.PdfGenerator.InvoiceData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/** Business logic of payments, delinquency and simulated DIAN electronic invoices. */
public class FinanceService {

    private final PaymentDao paymentDao;
    private final InvoiceDao invoiceDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final EnrollmentDao enrollmentDao;
    private final GradeLevelDao gradeLevelDao;
    private final ScholarshipDao scholarshipDao;
    private final AppConfig config = AppConfig.getInstance();

    public FinanceService() {
        this(new PaymentDaoImpl(), new InvoiceDaoImpl(), new StudentDaoImpl(), new GuardianDaoImpl(),
                new EnrollmentDaoImpl(), new GradeLevelDaoImpl(), new ScholarshipDaoImpl());
    }

    public FinanceService(PaymentDao paymentDao, InvoiceDao invoiceDao, StudentDao studentDao, GuardianDao guardianDao,
                          EnrollmentDao enrollmentDao, GradeLevelDao gradeLevelDao,
                          ScholarshipDao scholarshipDao) {
        this.paymentDao = paymentDao;
        this.invoiceDao = invoiceDao;
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.enrollmentDao = enrollmentDao;
        this.gradeLevelDao = gradeLevelDao;
        this.scholarshipDao = scholarshipDao;
    }

    public List<PaymentConcept> listConcepts() {
        return paymentDao.findActiveConcepts();
    }

    public PaymentConcept getConcept(int conceptId) {
        return paymentDao.findConceptById(conceptId)
                .orElseThrow(() -> new NotFoundException("No existe un concepto de pago con id " + conceptId));
    }

    /**
     * Suggested amount for a student. Recurring concepts get the larger of two discounts (they do not stack):
     * the sibling discount, when the student shares a guardian with other students, and the student's
     * scholarship for the current academic year. The user may override the amount.
     */
    public BigDecimal suggestedAmount(int studentId, int conceptId) {
        PaymentConcept concept = getConcept(conceptId);
        BigDecimal percent = discountPercent(studentId, concept);
        if (percent.signum() == 0) {
            return concept.baseAmount();
        }
        BigDecimal factor = BigDecimal.valueOf(100).subtract(percent).movePointLeft(2);
        return concept.baseAmount().multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /** True when {@link #suggestedAmount} applied the sibling discount. */
    public boolean hasSiblingDiscount(int studentId, int conceptId) {
        PaymentConcept concept = getConcept(conceptId);
        return concept.recurring() && siblingPercent(studentId).signum() > 0
                && siblingPercent(studentId).compareTo(scholarshipPercent(studentId)) >= 0;
    }

    /** Description of the discount applied by {@link #suggestedAmount}, or an empty string when there is none. */
    public String discountDescription(int studentId, int conceptId) {
        PaymentConcept concept = getConcept(conceptId);
        if (discountPercent(studentId, concept).signum() == 0) {
            return "";
        }
        if (hasSiblingDiscount(studentId, conceptId)) {
            return "descuento por hermanos (" + siblingPercent(studentId).stripTrailingZeros().toPlainString() + "%)";
        }
        return "beca (" + scholarshipPercent(studentId).stripTrailingZeros().toPlainString() + "%)";
    }

    private BigDecimal discountPercent(int studentId, PaymentConcept concept) {
        studentDao.findById(studentId).orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + studentId));
        if (!concept.recurring()) {
            return BigDecimal.ZERO;
        }
        return siblingPercent(studentId).max(scholarshipPercent(studentId));
    }

    private BigDecimal siblingPercent(int studentId) {
        int percent = config.getInt("business.siblingDiscountPercent", 10);
        return percent > 0 && !studentDao.findSiblings(studentId).isEmpty() ? BigDecimal.valueOf(percent) : BigDecimal.ZERO;
    }

    private BigDecimal scholarshipPercent(int studentId) {
        return scholarshipDao.findByStudentAndYear(studentId, config.getAcademicYear())
                .map(Scholarship::percent).orElse(BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------ scholarships

    /** Assigns (or replaces) the scholarship of a student for the current academic year. */
    public void assignScholarship(int studentId, BigDecimal percent, String reason) {
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + studentId));
        if (!student.isActive()) {
            throw new BusinessException("El estudiante no está activo (estado: " + student.status() + ").");
        }
        if (percent == null || percent.signum() <= 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("El porcentaje de la beca debe estar entre 0 (sin incluir) y 100.");
        }
        scholarshipDao.save(new Scholarship(0, studentId, config.getAcademicYear(),
                percent.setScale(2, RoundingMode.HALF_UP), reason, LocalDate.now()));
    }

    public void removeScholarship(int studentId) {
        if (scholarshipDao.delete(studentId, config.getAcademicYear()) == 0) {
            throw new NotFoundException("El estudiante " + studentId + " no tiene beca en " + config.getAcademicYear() + ".");
        }
    }

    /** Rows: student id, student, percent, reason, assigned on. */
    public List<String[]> scholarshipRows() {
        return scholarshipDao.listRows(config.getAcademicYear());
    }

    /**
     * Registers a received payment and immediately issues its electronic invoice (PDF).
     *
     * @return the generated invoice file
     */
    public Path registerPaymentAndInvoice(int studentId, int conceptId, BigDecimal amount, String method,
                                          int academicYear, String period, String notes) {
        int paymentId = registerPayment(studentId, conceptId, amount, method, academicYear, period, notes);
        return issueInvoice(paymentId);
    }

    public int registerPayment(int studentId, int conceptId, BigDecimal amount, String method,
                               int academicYear, String period, String notes) {
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + studentId));
        getConcept(conceptId);
        Validation.requireOneOf(method, Payment.METHODS, "El método de pago");
        Validation.requireOneOf(period, Payment.PERIODS, "El período");
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("El monto debe ser mayor que cero.");
        }
        if (!student.isActive()) {
            throw new BusinessException("El estudiante no está activo (estado: " + student.status() + ").");
        }
        Guardian payer = payerOf(studentId);
        String receipt = "REC-%06d".formatted(paymentDao.nextSequence());
        return paymentDao.insert(new Payment(0, studentId, payer.id(), conceptId, amount.setScale(2, RoundingMode.HALF_UP),
                LocalDate.now(), method, receipt, academicYear, period, notes));
    }

    /** Rows: id, receipt, date, concept, amount, method, period/year. */
    public List<String[]> paymentsOfStudent(int studentId) {
        studentDao.findById(studentId).orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + studentId));
        return paymentDao.historyByStudent(studentId);
    }

    /** All payments of every student linked to the guardian. Rows: id, receipt, date, student, concept, amount, method. */
    public List<String[]> paymentsOfGuardian(int guardianId) {
        guardianDao.findById(guardianId).orElseThrow(() -> new NotFoundException("No existe un acudiente con id " + guardianId));
        return paymentDao.historyByGuardian(guardianId);
    }

    /** Rows: id, student, grade, last payment, days elapsed, guardian phone. */
    public List<String[]> delinquencyReport() {
        return paymentDao.findDelinquent(config.get("business.delinquencyConcept", "PENSION"),
                config.getAcademicYear(), delinquencyDays());
    }

    public int delinquencyDays() {
        return config.getInt("business.delinquencyDays", 30);
    }

    public String delinquencyConceptCode() {
        return config.get("business.delinquencyConcept", "PENSION");
    }

    public List<String[]> invoiceRows() {
        return invoiceDao.listRows();
    }

    /**
     * Issues (or re-prints) the electronic invoice of a payment and writes the PDF to the invoices folder.
     *
     * @return absolute path of the PDF
     */
    public Path issueInvoice(int paymentId) {
        Payment payment = paymentDao.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("No existe un pago con id " + paymentId));
        Student student = studentDao.findById(payment.studentId())
                .orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + payment.studentId()));
        Guardian client = guardianDao.findById(payment.guardianId() == 0 ? payerOf(student.id()).id() : payment.guardianId())
                .orElseThrow(() -> new NotFoundException("No existe el acudiente del pago"));
        PaymentConcept concept = getConcept(payment.conceptId());

        Invoice existing = invoiceDao.findByPaymentId(paymentId).orElse(null);
        int sequence = existing != null ? invoiceSequence(existing) : invoiceDao.nextSequence();
        String number = existing != null ? existing.number() : "FE-%06d".formatted(sequence);
        LocalDate issueDate = existing != null ? existing.issueDate() : LocalDate.now();
        String cufe = existing != null && existing.cufe() != null
                ? existing.cufe() : computeCufe(number, config.get("school.nit"), issueDate);
        String fileName = "factura_%06d.pdf".formatted(sequence);
        Path output = Path.of(config.get("paths.invoicesDir", "facturas"), fileName);

        Invoice invoice = new Invoice(existing != null ? existing.id() : 0, number, paymentId, issueDate, cufe,
                fileName, output.toAbsolutePath().toString());
        BigDecimal discount = concept.baseAmount().subtract(payment.amountPaid()).max(BigDecimal.ZERO);
        String grade = enrollmentDao.findByStudentAndYear(student.id(), payment.academicYear())
                .flatMap(e -> gradeLevelDao.findById(e.gradeId())).map(g -> g.name()).orElse("-");

        PdfGenerator.generateInvoice(new InvoiceData(config.get("school.name"), config.get("school.nit"),
                config.get("school.address"), config.get("school.phone"), config.get("school.email"),
                config.get("school.resolution"), invoice,
                client.fullName(), client.documentType() + " " + client.documentNumber(),
                client.email(), client.address(), student.fullName(), grade, concept.name(),
                concept.baseAmount(), discount, payment.amountPaid(), payment.method(), payment.receiptNumber(),
                payment.paymentDate()), output);

        if (existing == null) {
            invoiceDao.insert(invoice);
        }
        return output.toAbsolutePath();
    }

    /** Simulated CUFE: SHA-256 (hex) of invoice number + issuer NIT + issue date. */
    public static String computeCufe(String invoiceNumber, String nit, LocalDate issueDate) {
        String source = invoiceNumber + nit + issueDate;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            LoggerUtility.logError("FinanceService.computeCufe", e);
            throw new DocumentException("SHA-256 no está disponible en esta JVM.", e);
        }
    }

    /** The guardian who pays: the financially responsible one, preferring the principal guardian. */
    private Guardian payerOf(int studentId) {
        return guardianDao.findByStudent(studentId).stream()
                .max(Comparator.comparing(Guardian::financialResponsible))
                .orElseThrow(() -> new BusinessException("El estudiante no tiene acudiente registrado."));
    }

    private static int invoiceSequence(Invoice invoice) {
        return Integer.parseInt(invoice.number().replaceAll("\\D", ""));
    }
}
