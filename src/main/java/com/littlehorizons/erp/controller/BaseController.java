package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.CustomExceptions.DataAccessException;
import com.littlehorizons.erp.util.CustomExceptions.DocumentException;
import com.littlehorizons.erp.util.LoggerUtility;
import com.littlehorizons.erp.view.BaseView;

import java.util.NoSuchElementException;

/** Common error-handling wrapper: no exception ever escapes a menu action. */
abstract class BaseController {

    private final BaseView errorView;

    protected BaseController(BaseView errorView) {
        this.errorView = errorView;
    }

    /** Runs a menu action, showing user-friendly messages and logging unexpected failures. */
    protected void safely(Runnable action) {
        try {
            action.run();
        } catch (BusinessException | DataAccessException | DocumentException e) {
            errorView.error(e.getMessage());
        } catch (NoSuchElementException e) {
            throw e; // end of input: let Main terminate gracefully
        } catch (RuntimeException e) {
            LoggerUtility.logError(getClass().getSimpleName(), e);
            errorView.error("Ocurrió un error inesperado. Revise logs/error_log.txt para más detalles.");
        }
    }
}
