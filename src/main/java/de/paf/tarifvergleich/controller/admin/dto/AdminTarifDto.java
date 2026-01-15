package de.paf.tarifvergleich.controller.admin.dto;

import de.paf.tarifvergleich.domain.GarantieModus;
import de.paf.tarifvergleich.domain.TarifTyp;

import java.math.BigDecimal;
import java.util.List;

public record AdminTarifDto(
        Long id,
        String tarifName,
        String tarifCode,
        Integer erscheinungsjahr,

        boolean aktiv,

        BigDecimal garantiezins,
        TarifTyp tarifTyp,
        GarantieModus garantieModus,
        BigDecimal garantieNiveau,
        BigDecimal topfBFloor,

        Integer minStartalter,
        Integer maxEndalter,
        BigDecimal mindestbeitragMonat,

        Long anbieterId,
        String anbieterName,

        List<Long> fondsIds,
        List<String> fondsNamen
) {}
