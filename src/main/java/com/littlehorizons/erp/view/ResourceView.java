package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;

import java.util.List;
import java.util.Scanner;

/** Menus and listings of the library / laboratory resources module. */
public class ResourceView extends BaseView {

    public ResourceView(Scanner scanner) {
        super(scanner);
    }

    public int mainMenu() {
        return showMenu("GESTIÓN DE RECURSOS", "Volver al menú principal",
                "Agregar recurso al inventario", "Listar inventario completo", "Listar recursos por estado",
                "Cambiar estado de un recurso", "Prestar recurso", "Registrar devolución",
                "Préstamos pendientes", "Reporte de préstamos vencidos", "Eliminar recurso");
    }

    public void showCategories(List<ResourceCategory> categories) {
        table(new String[]{"ID", "Categoría", "Descripción"},
                categories.stream().map(c -> cells(c.id(), c.name(), c.description())).toList());
    }

    public void showResources(List<Resource> resources) {
        table(new String[]{"ID", "Código", "Nombre", "Categoría", "Ubicación", "Estado"},
                resources.stream().map(r -> cells(r.id(), r.code(), r.name(), r.categoryName(), r.location(), r.status())).toList());
    }

    public void showLoans(List<String[]> rows) {
        table(new String[]{"Préstamo", "Prestatario", "Nombre", "Código", "Recurso", "Prestado", "Límite", "Estado"}, rows);
    }

    public void showOverdue(List<String[]> rows) {
        table(new String[]{"Préstamo", "Prestatario", "Nombre", "Código", "Recurso", "Prestado", "Límite", "Estado", "Días de atraso"}, rows);
    }
}
