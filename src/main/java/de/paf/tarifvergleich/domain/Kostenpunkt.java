package de.paf.tarifvergleich.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "kostenpunkt",
        indexes = {
                @Index(name = "ix_kostenpunkt_kostenstruktur", columnList = "kostenstruktur_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kostenpunkt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Zu welcher Kostenstruktur gehört dieser Kostenpunkt?
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "kostenstruktur_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Kostenstruktur kostenstruktur;

    /**
     * Technischer Code (z.B. "ABSCHLUSS_VERTEILT_5J", "FIX_STUFE1", ...)
     */
    @Column(nullable = false)
    private String code;

    /**
     * EURO oder PROZENT
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KostenTyp typ;

    /**
     * FIX / BEITRAG / KAPITAL
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KostenBasis basis;

    /**
     * Wann wird abgerechnet (monatlich/jährlich/verteilt)?
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KostenRhythmus rhythmus;

    /**
     * Wert:
     * - bei EURO: Eurobetrag (bei VERTEILT_* als Gesamtbetrag)
     * - bei PROZENT: Prozentwert (z.B. 0.60 = 0,60%)
     */
    @Column(precision = 19, scale = 6)
    private BigDecimal wert;

    /**
     * Nur relevant für PROZENT:
     * - JAHRLICH bedeutet: wert ist pro Jahr angegeben (wird in Service auf Monat umgerechnet)
     * - MONATLICH bedeutet: wert ist bereits pro Monat
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ProzentPeriode prozentPeriode = ProzentPeriode.MONATLICH;

    /**
     * Mindestbetrag in Euro (z.B. Mindest-Guthabenkosten pro Monat)
     */
    @Column(precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal minimumEuro = BigDecimal.ZERO;

    /**
     * Gültigkeit (Monatsindex 1..N innerhalb der Laufzeit)
     * Beispiel: Fixkosten Stufe1 nur Monat 1..120
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer gueltigVonMonat = 1;

    /**
     * null = bis zum Ende
     */
    private Integer gueltigBisMonat;

    /**
     * Aktiv?
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean aktiv = true;
}