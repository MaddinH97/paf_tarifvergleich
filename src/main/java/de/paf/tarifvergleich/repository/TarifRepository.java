package de.paf.tarifvergleich.repository;

import de.paf.tarifvergleich.domain.Tarif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TarifRepository extends JpaRepository<Tarif, Long> {

    /**
     * Tarife, die für die Kombination (beitrag, laufzeit) auswählbar sind:
     * - tarif.aktiv = true
     * - beitrag >= tarif.mindestbeitragMonat
     * - es gibt eine kostenstruktur mit aktiv=true und genau dieser Kombi
     */
    @Query("""
        select distinct t
        from Tarif t
        join Kostenstruktur k on k.tarif = t
        where t.aktiv = true
          and t.mindestbeitragMonat <= :beitrag
          and k.aktiv = true
          and k.beitragMonat = :beitrag
          and k.laufzeitJahre = :laufzeit
    """)
    List<Tarif> findEligible(@Param("beitrag") Integer beitrag,
                             @Param("laufzeit") Integer laufzeit);

    List<Tarif> findByAktivTrue();
}