package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.view.ResourceView;

/** Orchestrates the resources (library / labs) menus. */
public class ResourceController extends BaseController {

    private final ResourceView view;
    private final ResourceService service;

    public ResourceController(ResourceView view, ResourceService service) {
        super(view);
        this.view = view;
        this.service = service;
    }

    public void run() {
        int option;
        do {
            option = view.mainMenu();
            switch (option) {
                case 1 -> safely(this::addResource);
                case 2 -> safely(() -> view.showResources(service.listResources()));
                case 3 -> safely(() -> view.showResources(
                        service.listByStatus(view.chooseOption("Estado", Resource.STATUSES))));
                case 4 -> safely(() -> {
                    String code = view.readText("Código del recurso: ");
                    service.changeStatus(code, view.chooseOption("Nuevo estado", Resource.STATUSES));
                    view.success("Estado actualizado.");
                });
                case 5 -> safely(this::lend);
                case 6 -> safely(() -> {
                    boolean teacher = askTeacherBorrower();
                    service.giveBack(view.readInt("ID del préstamo: "), teacher);
                    view.success("Devolución registrada. El recurso está disponible nuevamente.");
                });
                case 7 -> safely(() -> view.showLoans(service.activeLoans()));
                case 8 -> safely(() -> view.showOverdue(service.overdueLoans()));
                case 9 -> safely(() -> {
                    String code = view.readText("Código del recurso a eliminar: ");
                    if (view.confirm("¿Eliminar el recurso " + service.getByCode(code).name() + "?")) {
                        service.deleteResource(code);
                        view.success("Recurso eliminado.");
                    }
                });
                default -> { }
            }
        } while (option != 0);
    }

    private void addResource() {
        String code = view.readText("Código único (ej. LIB-031): ");
        String name = view.readText("Nombre: ");
        view.showCategories(service.categories());
        int categoryId = view.readInt("ID de la categoría: ");
        String description = view.readOptionalText("Descripción");
        String location = view.readOptionalText("Ubicación física");
        view.success("Recurso agregado con ID " + service.addResource(code, name, categoryId, description, location) + ".");
    }

    private void lend() {
        String code = view.readText("Código del recurso a prestar: ");
        boolean teacher = askTeacherBorrower();
        int borrowerId = view.readInt(teacher ? "ID del profesor: " : "ID del estudiante: ");
        int loanId = service.lend(code, borrowerId, teacher);
        view.success("Préstamo " + loanId + " registrado. Plazo de devolución: " + service.loanDays() + " días.");
    }

    private boolean askTeacherBorrower() {
        return view.readIntInRange("Prestatario (1 = Estudiante, 2 = Profesor): ", 1, 2) == 2;
    }
}
