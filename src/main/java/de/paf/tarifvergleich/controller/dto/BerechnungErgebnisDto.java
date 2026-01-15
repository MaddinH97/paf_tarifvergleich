package de.paf.tarifvergleich.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public record BerechnungErgebnisDto(
        Long tarifId,
        String tarifName,
        String tarifCode,
        String anbieterName,

        BigDecimal endwert,                 // = Endkapital am Laufzeitende
        List<WertpunktDto> wertentwicklung  // Jahreswerte inkl. Töpfe
) {}