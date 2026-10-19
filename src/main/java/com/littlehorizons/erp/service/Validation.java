package com.littlehorizons.erp.service;

import com.littlehorizons.erp.util.CustomExceptions.BusinessException;

import java.util.Collection;

/** Small guard clauses shared by the services; messages are user-facing (Spanish). */
final class Validation {

    private Validation() {
    }

    static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(field + " es obligatorio.");
        }
    }

    static void requireOneOf(String value, Collection<String> allowed, String field) {
        if (value == null || !allowed.contains(value)) {
            throw new BusinessException(field + " inválido. Valores permitidos: " + String.join(", ", allowed) + ".");
        }
    }
}
