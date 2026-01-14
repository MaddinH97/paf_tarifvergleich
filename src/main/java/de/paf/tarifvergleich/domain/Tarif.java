package de.paf.tarifvergleich.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Tarif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tarifName;

    @Column(nullable = false, unique = true)
    private String tarifCode;

    private Integer erscheinungsjahr;

    /**
     * Garantiezins p.a. (z.B. 0.0225 = 2,25% p.a.)
     */
    @Column(nullable = false, precision = 18, scale = 10)
    @Builder.Default
    private BigDecimal garantiezins = BigDecimal.ZERO;

    private Integer minStartalter;
    private Integer maxEndalter;

    @Column(precision = 18, scale = 2)
    private BigDecimal mindestbeitragMonat;

    /**
     * Aktiv-Flag (Boolean damit null-sicher geprüft werden kann)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean aktiv = true;

    // ===== Anbieter =====
    @ManyToOne
    @JoinColumn(name = "anbieter_id")
    private Finanzdienstleistungsunternehmen anbieter;

    // ===== Fonds-Liste (Altbestand) =====
    @ManyToMany
    @JoinTable(
            name = "tarif_fonds",
            joinColumns = @JoinColumn(name = "tarif_id"),
            inverseJoinColumns = @JoinColumn(name = "fonds_id")
    )
    @Builder.Default
    private List<Fonds> fondsListe = List.of();

    /**
     * Kostenstrukturen je Beitrag/Laufzeit (Grid)
     */
    @OneToMany(mappedBy = "tarif", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    @Builder.Default
    private List<Kostenstruktur> kostenstrukturen = List.of();

    /**
     * Optional: bisheriger Script-Platzhalter.
     */
    @Lob
    private String berechnungsScript;

    // =========================================================
    // NEU: Tarif-Typen / Hybrid-Konfiguration
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TarifTyp tarifTyp = TarifTyp.FONDS;

    /**
     * Garantiezins mit/ohne Überschüsse (für Garantie-Topf).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GarantieModus garantieModus = GarantieModus.OHNE_UEBERSCHUESSE;

    /**
     * Garantie-Niveau bezogen auf Summe der Beiträge.
     * 1.0 = 100% Beitragsgarantie
     * 0.5 = 50% Beitragsgarantie
     */
    @Column(nullable = false, precision = 18, scale = 6)
    @Builder.Default
    private BigDecimal garantieNiveau = BigDecimal.ONE;

    /**
     * Nur relevant für 3-Topf-Hybrid:
     * Topf B soll nicht unter floor * letztem Topf-B-Wert fallen.
     * Beispiel: 0.80 = 80%.
     */
    @Column(nullable = false, precision = 18, scale = 6)
    @Builder.Default
    private BigDecimal topfBFloor = new BigDecimal("0.80");

    /**
     * Nur relevant für HYBRID_2_TOPF:
     * Legt fest, ob der zweite Topf Kapitalanlage B (Garantiefonds) ist
     * oder der klassische GARANTIE-Zins-Topf.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TopfTyp zweiterTopfTyp = TopfTyp.GARANTIE;
}