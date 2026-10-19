package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.MainController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.service.ExtracurricularService;
import com.littlehorizons.erp.service.FinanceService;
import com.littlehorizons.erp.service.ReportService;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.util.LoggerUtility;
import com.littlehorizons.erp.view.AcademicView;
import com.littlehorizons.erp.view.ExtracurricularView;
import com.littlehorizons.erp.view.FinanceView;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.ReportView;
import com.littlehorizons.erp.view.ResourceView;
import com.littlehorizons.erp.view.StudentView;
import com.littlehorizons.erp.view.TeacherView;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Application entry point: wires services, views and controllers and starts the console UI. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        useUtf8Console();
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        StudentService studentService = new StudentService();
        MainController app = new MainController(
                new MainMenuView(scanner),
                new StudentController(new StudentView(scanner), studentService),
                new AcademicController(new AcademicView(scanner), new AcademicService(), studentService),
                new TeacherController(new TeacherView(scanner), new TeacherService()),
                new ExtracurricularController(new ExtracurricularView(scanner), new ExtracurricularService()),
                new FinanceController(new FinanceView(scanner), new FinanceService()),
                new ResourceController(new ResourceView(scanner), new ResourceService()),
                new ReportController(new ReportView(scanner), new ReportService()));
        try {
            app.run();
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Cerrando el sistema.");
        } catch (RuntimeException e) {
            LoggerUtility.logError("Main", e);
            System.out.println("Error fatal. Revise logs/error_log.txt.");
        }
    }

    /**
     * The Windows console defaults to a legacy code page (850/437) that garbles accents and the
     * Unicode menu frames. Switching it to UTF-8 (65001) makes the output match what the app prints.
     */
    private static void useUtf8Console() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }
        try {
            new ProcessBuilder("cmd", "/c", "chcp 65001 >nul").inheritIO().start().waitFor();
        } catch (java.io.IOException e) {
            LoggerUtility.logError("Main.useUtf8Console", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
