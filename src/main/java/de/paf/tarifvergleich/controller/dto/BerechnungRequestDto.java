package de.paf.tarifvergleich.controller.dto;

import java.util.List;

public record BerechnungRequestDto(
        Integer beitragMonat,
        Integer laufzeitJahre,
        Integer einstiegsalter,
        List<Long> tarifIds
) {}
