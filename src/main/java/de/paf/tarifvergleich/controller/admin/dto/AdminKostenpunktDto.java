package de.paf.tarifvergleich.controller.admin.dto;

import de.paf.tarifvergleich.domain.KostenBasis;
import de.paf.tarifvergleich.domain.KostenRhythmus;
import de.paf.tarifvergleich.domain.KostenTyp;

import java.math.BigDecimal;

public record AdminKostenpunktDto(
        Long id,
        Long kostenstrukturId,

        String name,
        boolean aktiv,

        KostenRhythmus rhythmus,
        KostenTyp typ,
        KostenBasis basis,

        BigDecimal wert
) {}