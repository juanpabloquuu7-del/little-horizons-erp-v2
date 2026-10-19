package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.service.ReportService;
import com.littlehorizons.erp.view.ReportView;

/** Orchestrates the statistics menu. */
public class ReportController extends BaseController {

    private final ReportView view;
    private final ReportService service;

    public ReportController(ReportView view, ReportService service) {
        super(view);
        this.view = view;
        this.service = service;
    }

    public void run() {
        int option;
        do {
            option = view.mainMenu();
            switch (option) {
                case 1 -> safely(() -> view.showStudentsPerGrade(service.studentsPerGrade()));
                case 2 -> safely(() -> view.showTopStudents(
                        service.topStudents(view.readIntInRange("¿Cuántos estudiantes mostrar? (1-100): ", 1, 100))));
                case 3 -> safely(() -> view.showClubParticipation(service.clubParticipation()));
                case 4 -> safely(() -> view.showEventParticipation(service.eventParticipation()));
                case 5 -> safely(() -> view.showIncome("Concepto", service.incomeByConcept()));
                case 6 -> safely(() -> view.showIncome("Mes", service.incomeByMonth()));
                case 7 -> safely(() -> view.showResourceStatus(service.resourceStatus()));
                default -> { }
            }
        } while (option != 0);
    }
}
