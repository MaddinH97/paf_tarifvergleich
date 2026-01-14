package de.paf.tarifvergleich.domain;

public enum KostenRhythmus {
    EINMALIG,
    MONATLICH,
    JAHRLICH,
    VERTEILT_5_JAHRE, // über 60 Monate
    VERTEILT_7_JAHRE  // über 84 Monate
}