package com.littlehorizons.erp.model;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Helper to build display names from optional name parts. */
final class Names {

    private Names() {
    }

    static String join(String... parts) {
        return Arrays.stream(parts)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.joining(" "));
    }
}
