package de.paf.tarifvergleich.controller.admin.dto;

import de.paf.tarifvergleich.domain.KapitalanlageTyp;

import java.math.BigDecimal;

public record AdminKapitalanlageDto(
        Long id,
        String name,
        KapitalanlageTyp typ,
        BigDecimal annualRate,
        boolean aktiv
) {}