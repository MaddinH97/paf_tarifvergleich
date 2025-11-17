package de.paf.tarifvergleich.repository;

import de.paf.tarifvergleich.domain.Tarif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TarifRepository extends JpaRepository<Tarif, Long> {
}