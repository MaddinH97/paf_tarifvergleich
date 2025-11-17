package de.paf.tarifvergleich.domain;

import java.math.BigDecimal;

/**
 * Parameter, die der User pro Reiter eingibt.
 */
public class BerechnungsParameter {

    private Integer einstiegsalter;
    private Integer auszahlungsalter;
    private BigDecimal sparrateMonat;

    private Integer fondswechsel1Jahr;
    private Integer fondswechsel2Jahr;

    private BigDecimal steuersatzRente;
    private Fonds fonds; // ausgewählter Fonds

    public BerechnungsParameter() {
    }

    // Getter/Setter können wir später generieren oder mit Lombok machen
}
