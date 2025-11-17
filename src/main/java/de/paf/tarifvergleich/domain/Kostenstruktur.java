package de.paf.tarifvergleich.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "kostenstruktur")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kostenstruktur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal abschlusskosten;
    private BigDecimal verwaltungskosten;
    private BigDecimal fondskosten;
    private BigDecimal risikokosten;
    private BigDecimal sonstigeKosten;

    @OneToOne(mappedBy = "kostenstruktur")
    @ToString.Exclude
    private Tarif tarif;
}