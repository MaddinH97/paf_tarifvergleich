package de.paf.tarifvergleich.controller;

import de.paf.tarifvergleich.controller.dto.TarifKurzDto;
import de.paf.tarifvergleich.domain.Tarif;
import de.paf.tarifvergleich.repository.KostenstrukturRepository;
import de.paf.tarifvergleich.repository.TarifRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarife")
@RequiredArgsConstructor
public class TarifController {

    private final TarifRepository tarifRepository;
    private final KostenstrukturRepository kostenstrukturRepository;

    // (optional) alte "alles"-Liste kannst du behalten – aber UI sollte sie NICHT nutzen
    @GetMapping
    public List<Tarif> getAll() {
        return tarifRepository.findAll();
    }

    // ✅ NEU: nur Tarife, die für Beitrag+Laufzeit eine aktive Kostenstruktur haben UND Tarif aktiv ist
    @GetMapping("/verfuegbar")
    public List<TarifKurzDto> verfuegbar(
            @RequestParam Integer beitrag,
            @RequestParam Integer laufzeit
    ) {
        return kostenstrukturRepository
                .findByBeitragMonatAndLaufzeitJahreAndAktivTrue(beitrag, laufzeit)
                .stream()
                .map(k -> k.getTarif())          // Tarif aus Kostenstruktur
                .filter(t -> t.getAktiv() !=null && t.getAktiv())          // BEIDES: Tarif muss aktiv sein
                .distinct()
                .map(t -> new TarifKurzDto(
                        t.getId(),
                        t.getTarifName(),
                        t.getTarifCode(),
                        t.getAnbieter() != null ? t.getAnbieter().getName() : ""
                ))
                .toList();
    }

    // optional: Debug endpoint (zeigt, ob überhaupt Kostenstrukturen existieren)
    @GetMapping("/grid")
    public Object grid(@RequestParam Integer beitrag, @RequestParam Integer laufzeit) {
        return kostenstrukturRepository.findByBeitragMonatAndLaufzeitJahreAndAktivTrue(beitrag, laufzeit);
    }
}