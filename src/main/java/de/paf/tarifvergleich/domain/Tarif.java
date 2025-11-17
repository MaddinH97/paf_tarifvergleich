package de.paf.tarifvergleich.domain;

import java.math.BigDecimal;
import java.util.List;

public class Tarif {

    private Long id;
    private String tarifName;
    private String tarifCode;

    private BigDecimal garantiezins;
    private Integer laufzeitJahre;
    private BigDecimal rentenfaktor;

    private Finanzdienstleistungsunternehmen anbieter;
    private Kostenstruktur kostenstruktur;
    private List<Fonds> fondsListe;

    public Tarif() {
    }
}