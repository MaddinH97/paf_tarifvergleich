package de.paf.tarifvergleich.repository;


import de.paf.tarifvergleich.domain.Fonds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FondsRepository extends JpaRepository<Fonds, Long> {
}