package de.paf.tarifvergleich.repository;

import de.paf.tarifvergleich.domain.Kostenpunkt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KostenpunktRepository extends JpaRepository<Kostenpunkt, Long> {

    List<Kostenpunkt> findByKostenstruktur_Id(Long kostenstrukturId);

    List<Kostenpunkt> findByKostenstruktur_IdAndAktivTrue(Long kostenstrukturId);
}