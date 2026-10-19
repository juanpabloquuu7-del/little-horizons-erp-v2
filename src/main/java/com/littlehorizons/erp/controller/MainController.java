package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.view.MainMenuView;

/** Top-level navigation between the modules. */
public class MainController {

    private final MainMenuView view;
    private final StudentController students;
    private final AcademicController academic;
    private final TeacherController teachers;
    private final ExtracurricularController extracurricular;
    private final FinanceController finance;
    private final ResourceController resources;
    private final ReportController reports;

    public MainController(MainMenuView view, StudentController students, AcademicController academic,
                          TeacherController teachers, ExtracurricularController extracurricular,
                          FinanceController finance, ResourceController resources, ReportController reports) {
        this.view = view;
        this.students = students;
        this.academic = academic;
        this.teachers = teachers;
        this.extracurricular = extracurricular;
        this.finance = finance;
        this.resources = resources;
        this.reports = reports;
    }

    public void run() {
        view.welcome();
        int option;
        do {
            option = view.mainMenu();
            switch (option) {
                case 1 -> students.run();
                case 2 -> academic.run();
                case 3 -> teachers.run();
                case 4 -> extracurricular.run();
                case 5 -> finance.run();
                case 6 -> resources.run();
                case 7 -> reports.run();
                default -> { }
            }
        } while (option != MainMenuView.EXIT_OPTION);
        view.goodbye();
    }
}
