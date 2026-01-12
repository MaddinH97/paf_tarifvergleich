package de.paf.tarifvergleich.controller.dto;

import java.math.BigDecimal;

public record WertpunktDto(
        Integer jahr,
        BigDecimal wert
) {}