package de.paf.tarifvergleich.controller;

import de.paf.tarifvergleich.domain.Tarif;
import de.paf.tarifvergleich.repository.KostenstrukturRepository;
import de.paf.tarifvergleich.repository.TarifRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import de.paf.tarifvergleich.domain.Kostenstruktur;

@RestController
@RequestMapping("/api/tarife")
@RequiredArgsConstructor
public class TarifController {

    private final TarifRepository tarifRepository;
    private final KostenstrukturRepository kostenstrukturRepository;

    @GetMapping
    public List<Tarif> getAll() {
        return tarifRepository.findAll();
    }

    // ✅ NEU: nur Tarife, die für Beitrag+Laufzeit eine aktive Kostenstruktur haben
    @GetMapping("/verfuegbar")
    public List<Tarif> verfuegbar(
            @RequestParam Integer beitrag,
            @RequestParam Integer laufzeit
    ) {
        return kostenstrukturRepository
                .findByBeitragMonatAndLaufzeitJahreAndAktivTrue(beitrag, laufzeit)
                .stream()
                .map(Kostenstruktur::getTarif)
                .distinct()
                .toList();
    }

    // Optional: Debug/Info (was ist aktiv / nicht aktiv) – kann später weg
    @GetMapping("/grid")
    public Map<String, Object> grid(
            @RequestParam Integer beitrag,
            @RequestParam Integer laufzeit
    ) {
        var ks = kostenstrukturRepository.findByBeitragMonatAndLaufzeitJahreAndAktivTrue(beitrag, laufzeit);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("beitrag", beitrag);
        out.put("laufzeit", laufzeit);
        out.put("count", ks.size());
        out.put("tarife", ks.stream().map(k -> k.getTarif().getTarifName()).toList());
        return out;
    }
}