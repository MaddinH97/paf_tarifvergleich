package de.paf.tarifvergleich.controller.dto;

import java.math.BigDecimal;

public record WertpunktDto(
        int jahr,
        BigDecimal summeEinzahlungen,
        BigDecimal gesamtKapital,
        BigDecimal topf1Fonds,
        BigDecimal topf2Wertsicherung,
        BigDecimal topf3Garantie
) {}