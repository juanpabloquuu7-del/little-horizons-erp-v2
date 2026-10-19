package com.littlehorizons.erp.model;

/** A labelled average (e.g. a subject, a period or a course) with the number of scores behind it. */
public record AverageLine(String label, double average, long count) {
}
