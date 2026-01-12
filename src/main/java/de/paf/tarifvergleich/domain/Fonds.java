package de.paf.tarifvergleich.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "fonds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fonds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String isin;
    private String name;
    private String typ;          // Aktienfonds, Mischfonds, ...
    private String risikoklasse; // niedrig/mittel/hoch
    private BigDecimal erwarteteRenditePa;

    /**
     * Viele Tarife können denselben Fonds nutzen (n:m).
     * JSON ignorieren, sonst Zyklen.
     */
    @ManyToMany(mappedBy = "fondsListe")
    @JsonIgnore
    @ToString.Exclude
    private List<Tarif> tarife;
}