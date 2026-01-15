package de.paf.tarifvergleich.controller.admin.dto;

import java.math.BigDecimal;

public record AdminKostenstrukturDto(
        Long id,
        Long tarifId,
        String tarifName,

        Integer beitragMonat,
        Integer laufzeitJahre,
        boolean aktiv,

        BigDecimal abschlusskosten,
        BigDecimal verwaltungskosten,
        BigDecimal fondskosten,
        BigDecimal risikokosten,
        BigDecimal sonstigeKosten
) {}