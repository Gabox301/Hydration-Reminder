package com.hydration.model;

import java.time.LocalDateTime;

/** Un registro de consumo de agua (un "vaso" tomado). */
public record HydrationEntry(
        long id,
        int amountMl,
        LocalDateTime timestamp) {
    public static HydrationEntry of(int amountMl) {
        return new HydrationEntry(-1, amountMl, LocalDateTime.now());
    }
}
