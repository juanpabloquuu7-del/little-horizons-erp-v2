package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.dao.ReportDaoImpl;

import java.util.List;

/** Read-only statistics for the "Reportes y Estadísticas" menu. */
public class ReportService {

    private final ReportDao reportDao;
    private final int academicYear = AppConfig.getInstance().getAcademicYear();

    public ReportService() {
        this(new ReportDaoImpl());
    }

    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public int academicYear() {
        return academicYear;
    }

    public List<String[]> studentsPerGrade() {
        return reportDao.studentsPerGrade(academicYear);
    }

    public List<String[]> topStudents(int limit) {
        return reportDao.topStudents(academicYear, Math.max(1, Math.min(limit, 100)));
    }

    public List<String[]> clubParticipation() {
        return reportDao.clubParticipation();
    }

    public List<String[]> eventParticipation() {
        return reportDao.eventParticipation();
    }

    public List<String[]> incomeByConcept() {
        return reportDao.incomeByConcept(academicYear);
    }

    public List<String[]> incomeByMonth() {
        return reportDao.incomeByMonth(academicYear);
    }

    public List<String[]> resourceStatus() {
        return reportDao.resourceStatus();
    }
}
