package com.littlehorizons.erp.view;

import java.util.Scanner;

/** Main menu and welcome banner. */
public class MainMenuView extends BaseView {

    public static final int EXIT_OPTION = 8;

    public MainMenuView(Scanner scanner) {
        super(scanner);
    }

    public void welcome() {
        System.out.println("""

                ╔══════════════════════════════════════════════════════════════════╗
                ║          COLEGIO INTERNACIONAL "LITTLE HORIZONS"                 ║
                ║              ERP - Sistema de Gestión Educativa                  ║
                ╚══════════════════════════════════════════════════════════════════╝""");
    }

    /** Returns 1-7 for the modules or {@link #EXIT_OPTION}. */
    public int mainMenu() {
        return showMenu("MENÚ PRINCIPAL", null,
                "Gestión de Estudiantes", "Gestión Académica", "Gestión de Profesores",
                "Actividades Extracurriculares", "Gestión Financiera", "Gestión de Recursos",
                "Reportes y Estadísticas", "Salir");
    }

    public void goodbye() {
        System.out.println("\nGracias por usar el ERP Little Horizons. ¡Hasta pronto!");
    }
}
