package de.paf.tarifvergleich.repository;

import de.paf.tarifvergleich.domain.Finanzdienstleistungsunternehmen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinanzdienstleistungsunternehmenRepository
        extends JpaRepository<Finanzdienstleistungsunternehmen, Long> {
}
