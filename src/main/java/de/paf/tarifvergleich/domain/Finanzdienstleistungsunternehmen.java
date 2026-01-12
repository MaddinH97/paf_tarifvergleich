package de.paf.tarifvergleich.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
     * JSON ignorieren, damit keine Endlosschleifen entstehen.
     */
    @OneToMany(mappedBy = "anbieter")
    @JsonIgnore
    @ToString.Exclude
    @Builder.Default
    private List<Tarif> tarife = List.of();
}