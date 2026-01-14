package de.paf.tarifvergleich.domain;

public enum KostenBasis {
    FIX,        // fixer Betrag (bei EURO sinnvoll)
    BEITRAG,    // Prozent vom Monatsbeitrag
    KAPITAL     // Prozent vom aktuellen Kapital
}