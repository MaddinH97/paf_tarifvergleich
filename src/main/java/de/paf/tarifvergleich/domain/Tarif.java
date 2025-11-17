package de.paf.tarifvergleich.domain;

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

    private String tarifName;
    private String tarifCode;

    private BigDecimal garantiezins;
    private Integer laufzeitJahre;
    private BigDecimal rentenfaktor;

    @ManyToOne
    @JoinColumn(name = "anbieter_id")
    private Finanzdienstleistungsunternehmen anbieter;

    @OneToOne
    @JoinColumn(name = "kostenstruktur_id")
    private Kostenstruktur kostenstruktur;

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
}