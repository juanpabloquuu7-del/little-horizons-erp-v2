package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.LoanDaoImpl;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.ResourceDaoImpl;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentDaoImpl;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TeacherDaoImpl;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.CustomExceptions.NotFoundException;

import java.time.LocalDate;
import java.util.List;

/** Business logic of the library / laboratory inventory and its loans. */
public class ResourceService {

    private final ResourceDao resourceDao;
    private final LoanDao loanDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;
    private final int loanDays;

    public ResourceService() {
        this(new ResourceDaoImpl(), new LoanDaoImpl(), new StudentDaoImpl(), new TeacherDaoImpl(),
                AppConfig.getInstance().getInt("business.loanDays", 7));
    }

    public ResourceService(ResourceDao resourceDao, LoanDao loanDao, StudentDao studentDao,
                           TeacherDao teacherDao, int loanDays) {
        this.resourceDao = resourceDao;
        this.loanDao = loanDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
        this.loanDays = loanDays;
    }

    public List<ResourceCategory> categories() {
        return resourceDao.findCategories();
    }

    public int addResource(String code, String name, int categoryId, String description, String location) {
        Validation.requireText(code, "El código");
        Validation.requireText(name, "El nombre");
        resourceDao.findCategoryById(categoryId).orElseThrow(() -> new NotFoundException("No existe la categoría " + categoryId));
        String normalizedCode = code.trim().toUpperCase();
        if (resourceDao.findByCode(normalizedCode).isPresent()) {
            throw new BusinessException("Ya existe un recurso con el código " + normalizedCode + ".");
        }
        return resourceDao.insert(new Resource(0, normalizedCode, name.trim(), categoryId, null, description,
                LocalDate.now(), location, Resource.AVAILABLE));
    }

    /** Changes the status manually (maintenance, retired...). A loaned resource must be returned first. */
    public void changeStatus(String code, String status) {
        Validation.requireOneOf(status, Resource.STATUSES, "El estado");
        Resource resource = getByCode(code);
        if (Resource.LOANED.equals(resource.status()) || Resource.LOANED.equals(status)) {
            throw new BusinessException("El estado 'Prestado' sólo cambia mediante préstamos y devoluciones.");
        }
        resourceDao.updateStatus(resource.id(), status);
    }

    public void deleteResource(String code) {
        Resource resource = getByCode(code);
        if (Resource.LOANED.equals(resource.status())) {
            throw new BusinessException("No se puede eliminar un recurso que está prestado.");
        }
        resourceDao.delete(resource.id());
    }

    public Resource getByCode(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        return resourceDao.findByCode(normalized)
                .orElseThrow(() -> new NotFoundException("No existe un recurso con el código " + normalized));
    }

    public List<Resource> listResources() {
        return resourceDao.findAll();
    }

    public List<Resource> listByStatus(String status) {
        Validation.requireOneOf(status, Resource.STATUSES, "El estado");
        return resourceDao.findByStatus(status);
    }

    /** Lends a resource to a student or teacher; returns the new loan id. */
    public int lend(String resourceCode, int borrowerId, boolean teacherBorrower) {
        Resource resource = getByCode(resourceCode);
        if (!resource.isAvailable()) {
            throw new BusinessException("El recurso " + resource.code() + " no está disponible (estado: " + resource.status() + ").");
        }
        if (teacherBorrower) {
            teacherDao.findById(borrowerId).orElseThrow(() -> new NotFoundException("No existe un profesor con id " + borrowerId));
        } else {
            Student student = studentDao.findById(borrowerId)
                    .orElseThrow(() -> new NotFoundException("No existe un estudiante con id " + borrowerId));
            if (!student.isActive()) {
                throw new BusinessException("El estudiante no está activo (estado: " + student.status() + ").");
            }
        }
        LocalDate today = LocalDate.now();
        return loanDao.lend(new Loan(0, resource.id(), borrowerId, teacherBorrower, today, today.plusDays(loanDays),
                null, Loan.ACTIVE, null));
    }

    public void giveBack(int loanId, boolean teacherBorrower) {
        if (!loanDao.giveBack(loanId, teacherBorrower, LocalDate.now())) {
            throw new BusinessException("No existe un préstamo pendiente con ese id para el tipo de prestatario indicado.");
        }
    }

    /** Rows: loan id, borrower type, borrower, resource code, resource, loan date, due date, status. */
    public List<String[]> activeLoans() {
        loanDao.markOverdue();
        return loanDao.findActive();
    }

    /** Same columns as {@link #activeLoans()} plus days late. */
    public List<String[]> overdueLoans() {
        loanDao.markOverdue();
        return loanDao.findOverdue();
    }

    public int loanDays() {
        return loanDays;
    }
}
