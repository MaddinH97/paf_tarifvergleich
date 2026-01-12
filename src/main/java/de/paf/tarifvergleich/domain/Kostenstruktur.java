package de.paf.tarifvergleich.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "kostenstruktur",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tarif_beitrag_laufzeit",
                columnNames = {"tarif_id", "beitrag_monat", "laufzeit_jahre"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kostenstruktur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Zu welchem Tarif gehört diese Kostenstruktur (Grid-Eintrag)?
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "tarif_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Tarif tarif;

    /**
     * Grid-Keys
     */
    @Column(name = "beitrag_monat", nullable = false)
    private Integer beitragMonat; // 25..200 (nur deine 8 Werte)

    @Column(name = "laufzeit_jahre", nullable = false)
    private Integer laufzeitJahre; // 15..65 (deine Werte)

    /**
     * Nur wenn TRUE, ist diese Kombination berechenbar.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean aktiv = false;

    /**
     * === HINWEIS ===
     * Du wolltest sehr viele Kostenpunkte + je Punkt EURO oder PROZENT.
     * Dafür erweitern wir das Schema als nächstes.
     *
     * Für jetzt lassen wir deine bisherigen Felder drin, damit es weiterläuft.
     * (Wir bauen die große Liste im nächsten Schritt.)
     */

    // bisherige Felder (aus deinem Stand)
    private BigDecimal abschlusskosten;
    private BigDecimal verwaltungskosten;
    private BigDecimal fondskosten;
    private BigDecimal risikokosten;
    private BigDecimal sonstigeKosten;
}