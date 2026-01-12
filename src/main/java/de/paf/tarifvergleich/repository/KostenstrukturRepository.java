package de.paf.tarifvergleich.repository;

import de.paf.tarifvergleich.domain.Kostenstruktur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KostenstrukturRepository extends JpaRepository<Kostenstruktur, Long> {

    List<Kostenstruktur> findByBeitragMonatAndLaufzeitJahreAndAktivTrue(
            Integer beitragMonat,
            Integer laufzeitJahre
    );

    Optional<Kostenstruktur> findFirstByTarif_IdAndBeitragMonatAndLaufzeitJahreAndAktivTrue(
            Long tarifId,
            Integer beitragMonat,
            Integer laufzeitJahre
    );
}