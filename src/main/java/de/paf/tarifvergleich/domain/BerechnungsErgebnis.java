package de.paf.tarifvergleich.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ergebnis einer Berechnung für einen Tarif.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BerechnungsErgebnis {

    private Tarif tarif;

    /**
     * Verlauf des Vertragswerts über die Jahre.
     */
    private List<WertentwicklungsPunkt> verlauf;

    /**
     * Endkapital am Ende der Ansparphase.
     */
    private BigDecimal endkapital;

    /**
     * Monatliche Rente aus dem Endkapital.
     */
    private BigDecimal monatlicheRente;
}