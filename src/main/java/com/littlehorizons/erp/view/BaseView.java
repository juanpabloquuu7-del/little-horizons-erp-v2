package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.InputValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Console I/O primitives shared by every view: framed menus, validated prompts and tables.
 * All user-facing text is in Spanish.
 */
public abstract class BaseView {

    private static final int WIDTH = 66;
    private static final int MAX_CELL = 38;

    private final Scanner scanner;

    protected BaseView(Scanner scanner) {
        this.scanner = scanner;
    }

    // ------------------------------------------------------------------ menus

    /**
     * Draws a framed menu and returns the chosen option. Option 0 is the "back" entry labelled
     * {@code zeroLabel}; when {@code zeroLabel} is null there is no option 0 (the caller adds its own exit entry).
     */
    public int showMenu(String title, String zeroLabel, String... options) {
        int min = zeroLabel == null ? 1 : 0;
        while (true) {
            System.out.println();
            System.out.println("╔" + "═".repeat(WIDTH) + "╗");
            System.out.println("║" + center(title, WIDTH) + "║");
            System.out.println("╠" + "═".repeat(WIDTH) + "╣");
            for (int i = 0; i < options.length; i++) {
                System.out.println("║" + pad("  " + (i + 1) + ". " + options[i], WIDTH) + "║");
            }
            if (zeroLabel != null) {
                System.out.println("║" + pad("  0. " + zeroLabel, WIDTH) + "║");
            }
            System.out.println("╚" + "═".repeat(WIDTH) + "╝");
            String raw = readLine("Seleccione una opción: ");
            var choice = InputValidator.parseInt(raw);
            if (choice.isPresent() && choice.get() >= min && choice.get() <= options.length) {
                return choice.get();
            }
            error("Opción inválida. Ingrese un número entre " + min + " y " + options.length + ".");
        }
    }

    /** Shows a numbered list of fixed values and returns the one chosen (avoids typing accents). */
    public String chooseOption(String prompt, List<String> options) {
        System.out.println("\n" + prompt + ":");
        for (int i = 0; i < options.size(); i++) {
            System.out.println("   " + (i + 1) + ". " + options.get(i));
        }
        return options.get(readIntInRange("Opción: ", 1, options.size()) - 1);
    }

    // ------------------------------------------------------------------ prompts

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** Required, non-blank text. */
    public String readText(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (InputValidator.isNotBlank(value)) {
                return value;
            }
            error("Este campo es obligatorio.");
        }
    }

    /** Text that may be left blank (returns an empty string). */
    public String readOptionalText(String prompt) {
        return readLine(prompt + " (Enter para omitir): ");
    }

    public String readEmail(String prompt) {
        while (true) {
            String value = readText(prompt);
            if (InputValidator.isValidEmail(value)) {
                return value;
            }
            error("Correo electrónico inválido (ejemplo: nombre@dominio.com).");
        }
    }

    public int readInt(String prompt) {
        while (true) {
            var value = InputValidator.parseInt(readLine(prompt));
            if (value.isPresent()) {
                return value.get();
            }
            error("Ingrese un número entero válido.");
        }
    }

    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            error("El valor debe estar entre " + min + " y " + max + ".");
        }
    }

    public BigDecimal readDecimal(String prompt) {
        while (true) {
            var value = InputValidator.parseDecimal(readLine(prompt));
            if (value.isPresent()) {
                return value.get();
            }
            error("Ingrese un número válido (ejemplo: 4.5).");
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            var value = InputValidator.parseDate(readLine(prompt + " (AAAA-MM-DD): "));
            if (value.isPresent()) {
                return value.get();
            }
            error("Fecha inválida. Use el formato AAAA-MM-DD, por ejemplo 2026-10-19.");
        }
    }

    public LocalTime readTime(String prompt) {
        while (true) {
            try {
                return LocalTime.parse(readLine(prompt + " (HH:MM): "));
            } catch (DateTimeParseException e) {
                error("Hora inválida. Use el formato HH:MM, por ejemplo 07:30.");
            }
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String answer = readLine(prompt + " (s/n): ").toLowerCase();
            if (answer.equals("s") || answer.equals("si") || answer.equals("sí")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            error("Responda 's' o 'n'.");
        }
    }

    public void pause() {
        readLine("\nPresione Enter para continuar...");
    }

    // ------------------------------------------------------------------ messages

    public void success(String message) {
        System.out.println("\n✔ " + message);
    }

    public void error(String message) {
        System.out.println("\n✘ " + message);
    }

    public void info(String message) {
        System.out.println("\nℹ " + message);
    }

    public void title(String text) {
        System.out.println("\n── " + text + " " + "─".repeat(Math.max(3, WIDTH - text.length() - 4)));
    }

    // ------------------------------------------------------------------ tables

    public void table(String[] headers, List<String[]> rows) {
        if (rows.isEmpty()) {
            info("No hay registros para mostrar.");
            return;
        }
        int[] widths = new int[headers.length];
        for (int c = 0; c < headers.length; c++) {
            final int col = c;
            widths[c] = Math.min(MAX_CELL, Math.max(headers[c].length(),
                    rows.stream().mapToInt(r -> cell(r, col).length()).max().orElse(0)));
        }
        System.out.println();
        System.out.println(border('┌', '┬', '┐', widths));
        System.out.println(row(headers, widths));
        System.out.println(border('├', '┼', '┤', widths));
        rows.forEach(r -> System.out.println(row(r, widths)));
        System.out.println(border('└', '┴', '┘', widths));
        System.out.println("  " + rows.size() + " registro(s)");
    }

    private static String cell(String[] row, int col) {
        String value = col < row.length && row[col] != null ? row[col] : "";
        return value.length() > MAX_CELL ? value.substring(0, MAX_CELL - 1) + "…" : value;
    }

    private static String row(String[] values, int[] widths) {
        StringBuilder sb = new StringBuilder("│");
        for (int c = 0; c < widths.length; c++) {
            sb.append(' ').append(pad(cell(values, c), widths[c])).append(" │");
        }
        return sb.toString();
    }

    private static String border(char left, char middle, char right, int[] widths) {
        StringBuilder sb = new StringBuilder().append(left);
        for (int c = 0; c < widths.length; c++) {
            sb.append("─".repeat(widths[c] + 2)).append(c < widths.length - 1 ? middle : right);
        }
        return sb.toString();
    }

    private static String pad(String text, int width) {
        return text.length() >= width ? text.substring(0, width) : text + " ".repeat(width - text.length());
    }

    private static String center(String text, int width) {
        int left = Math.max(0, (width - text.length()) / 2);
        return pad(" ".repeat(left) + text, width);
    }

    protected static String[] cells(Object... values) {
        return Arrays.stream(values).map(v -> v == null ? "" : String.valueOf(v)).toArray(String[]::new);
    }
}
