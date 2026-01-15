package de.paf.tarifvergleich.controller.dto;

public record TarifKurzDto(
        Long id,
        String tarifName,
        String tarifCode,
        String anbieterName,
        boolean verfuegbar,
        String hinweis
) {}