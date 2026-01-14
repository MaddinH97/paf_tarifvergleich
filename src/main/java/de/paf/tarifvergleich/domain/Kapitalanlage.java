package de.paf.tarifvergleich.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kapitalanlage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kapitalanlage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // "FantasyFonds", "3% Rendite", ...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KapitalanlageTyp typ;

    /**
     * Nur für FIXED sinnvoll (0.03 / 0.06 / 0.09).
     * Für FANTASY kann es null sein.
     */
    private BigDecimal annualRate;

    /**
     * Monatsrenditen als Dezimalwerte (z.B. 0.0025 = +0.25%).
     * Reihenfolge ist wichtig.
     */
    @ElementCollection
    @CollectionTable(
            name = "kapitalanlage_monatsrendite",
            joinColumns = @JoinColumn(name = "kapitalanlage_id")
    )
    @OrderColumn(name = "monat_index")
    @Column(name = "rendite", nullable = false, precision = 20, scale = 10)
    @Builder.Default
    private List<BigDecimal> monatlicheRenditen = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean aktiv = true;
}