package de.paf.tarifvergleich.controller.dto;

import de.paf.tarifvergleich.domain.GarantieModus;

import java.util.List;

public record BerechnungRequestDto(
        Integer beitragMonat,
        Integer laufzeitJahre,
        Integer einstiegsalter,

        // Kapitalanlagen (A ist Pflicht, B optional – je nach Tariftyp)
        Long kapitalanlageAId,
        Long kapitalanlageBId,

        // Nur relevant wenn ein Garantietopf vorkommt (Hybrid/3-Topf)
        GarantieModus garantieModus,

        // Welche Tarife sollen gerechnet werden?
        List<Long> tarifIds
) {}