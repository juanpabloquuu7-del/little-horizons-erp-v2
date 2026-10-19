package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** Catalog entry for a payment concept with its base amount. */
public record PaymentConcept(int id, String code, String name, String description, BigDecimal baseAmount,
                             boolean recurring, boolean active) {
}
