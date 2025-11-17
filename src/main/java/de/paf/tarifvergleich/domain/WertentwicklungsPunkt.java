package de.paf.tarifvergleich.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Ein Punkt im Wertentwicklungsverlauf (z.B. Jahr 5, Wert 12.345 €).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WertentwicklungsPunkt {

    /**
     * Jahr seit Beginn der Ansparphase (0 = Start).
     */
    private int jahr;

    /**
     * Vertragswert zu diesem Zeitpunkt.
     */
    private BigDecimal wert;
}