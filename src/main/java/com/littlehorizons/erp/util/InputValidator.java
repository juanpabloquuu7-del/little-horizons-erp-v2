package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Pure parsing/validation helpers for console input. Return empty when the input is invalid.
 */
public final class InputValidator {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private InputValidator() {
    }

    public static Optional<Integer> parseInt(String text) {
        try {
            return Optional.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public static Optional<BigDecimal> parseDecimal(String text) {
        try {
            return Optional.of(new BigDecimal(text.trim().replace(',', '.')));
        } catch (NumberFormatException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public static Optional<LocalDate> parseDate(String text) {
        try {
            return Optional.of(LocalDate.parse(text.trim()));
        } catch (DateTimeParseException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public static boolean isValidEmail(String text) {
        return text != null && EMAIL.matcher(text.trim()).matches();
    }

    public static boolean isNotBlank(String text) {
        return text != null && !text.isBlank();
    }
}
