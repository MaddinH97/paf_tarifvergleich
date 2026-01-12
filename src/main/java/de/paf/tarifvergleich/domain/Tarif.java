package de.paf.tarifvergleich.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "tarif")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stammdaten
    private String tarifName;
    private String tarifCode;

    private Integer erscheinungsjahr;

    private BigDecimal garantiezins;

    private Integer minStartalter;
    private Integer maxEndalter;

    private BigDecimal mindestbeitragMonat;

    @Builder.Default
    private Boolean aktiv = false;

    /**
     * Berechnungslogik als Script/Code in der DB (Text).
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String berechnungsScript;

    // Beziehungen
    @ManyToOne
    @JoinColumn(name = "anbieter_id")
    private Finanzdienstleistungsunternehmen anbieter;

    /**
     * Viele Tarife – viele Fonds (n:m).
     */
    @ManyToMany
    @JoinTable(
            name = "tarif_fonds",
            joinColumns = @JoinColumn(name = "tarif_id"),
            inverseJoinColumns = @JoinColumn(name = "fonds_id")
    )
    @Builder.Default
    private List<Fonds> fondsListe = List.of();

    /**
     * Grid-Einträge: je Kombination aus Beitrag/Laufzeit eine Kostenstruktur.
     * Für JSON ignorieren wir das hier erstmal (sonst riesige Payloads / Zyklen).
     */
    @OneToMany(mappedBy = "tarif", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    @Builder.Default
    private List<Kostenstruktur> kostenstrukturen = List.of();
}