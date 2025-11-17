package de.paf.tarifvergleich.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "finanzdienstleistungsunternehmen")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Finanzdienstleistungsunternehmen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /**
     * Ein Unternehmen kann mehrere Tarife anbieten.
     */
    @OneToMany(mappedBy = "anbieter")
    @ToString.Exclude
    @Builder.Default
    private List<Tarif> tarife = List.of();
}