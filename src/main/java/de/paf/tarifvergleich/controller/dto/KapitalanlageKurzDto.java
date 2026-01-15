package de.paf.tarifvergleich.controller.dto;

import de.paf.tarifvergleich.domain.KapitalanlageTyp;

public record KapitalanlageKurzDto(
        Long id,
        String name,
        String typ
) {}