package de.paf.tarifvergleich.domain;

import java.math.BigDecimal;

public class Fonds {

    private String isin;
    private String name;
    private String typ;          // z.B. Aktienfonds, Mischfonds
    private String risikoklasse; // niedrig/mittel/hoch
    private BigDecimal erwarteteRenditePa; // z.B. 0.05 = 5 %

    public Fonds() {
    }
}