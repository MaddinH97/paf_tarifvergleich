package de.paf.tarifvergleich.repository;

import de.paf.tarifvergleich.domain.Tarif;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarifRepository extends JpaRepository<Tarif, Long> {

    // Aktive Tarife, sauber sortiert
    List<Tarif> findByAktivTrueOrderByTarifNameAsc();

    // falls du es noch irgendwo nutzt:
    List<Tarif> findByAktivTrue();
}