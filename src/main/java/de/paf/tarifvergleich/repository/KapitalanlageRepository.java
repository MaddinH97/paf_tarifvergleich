package de.paf.tarifvergleich.repository;

import de.paf.tarifvergleich.domain.Kapitalanlage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KapitalanlageRepository extends JpaRepository<Kapitalanlage, Long> {
    List<Kapitalanlage> findByAktivTrueOrderByNameAsc();
    Optional<Kapitalanlage> findByName(String name);
}