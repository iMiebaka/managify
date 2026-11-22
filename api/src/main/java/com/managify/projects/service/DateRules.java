package com.managify.projects.service;

import com.managify.exception.AppException;

import java.time.LocalDate;

public final class DateRules {

    private DateRules() {}

    /** Both dates are optional, but when both are set the end must not be before the start. */
    public static void requireOrdered(LocalDate start, LocalDate end, String what) {
        if (start != null && end != null && end.isBefore(start)) {
            throw AppException.invalidDates(what + " ends (" + end + ") before it starts (" + start + ")");
        }
    }
}
