package de.paf.tarifvergleich.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Parameter, die der User pro Reiter in der UI eingibt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BerechnungsParameter {

    private Integer einstiegsalter;
    private Integer auszahlungsalter;

    /**
     * Monatliche Sparrate in EUR.
     */
    private BigDecimal sparrateMonat;

    /**
     * Fondswechsel nach X Jahren (relativ zum Start).
     */
    private Integer fondswechsel1Jahr;
    private Integer fondswechsel2Jahr;

    /**
     * Steuersatz der Rente, z.B. 0.18 = 18 %.
     */
    private BigDecimal steuersatzRente;

    /**
     * Vom Benutzer ausgewählter Fonds.
     */
    private Fonds fonds;
}