package de.paf.tarifvergleich.controller;

import de.paf.tarifvergleich.controller.dto.TarifKurzDto;
import de.paf.tarifvergleich.domain.Tarif;
import de.paf.tarifvergleich.repository.KostenstrukturRepository;
import de.paf.tarifvergleich.repository.TarifRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/tarife")
@RequiredArgsConstructor
public class TarifController {

    private final TarifRepository tarifRepository;
    private final KostenstrukturRepository kostenstrukturRepository;

    /**
     * UI ruft: /api/tarife?beitragMonat=50&laufzeit=30
     *
     * Regeln:
     * - Inaktive Tarife: NICHT anzeigen
     * - Aktive Tarife: immer anzeigen
     *   - wenn für (beitragMonat,laufzeit) eine aktive Kostenstruktur existiert -> auswählbar=true
     *   - sonst -> auswählbar=false + Hinweis "keine Daten vorhanden"
     * - Mindestbeitrag: wenn beitragMonat < tarif.mindestbeitragMonat -> auswählbar=false + Hinweis
     */
    @GetMapping
    public List<TarifKurzDto> listTarifeMitVerfuegbarkeit(
            @RequestParam Integer beitragMonat,
            @RequestParam Integer laufzeit
    ) {
        var aktiveTarife = tarifRepository.findByAktivTrueOrderByTarifNameAsc();

        return aktiveTarife.stream()
                .map(t -> toKurzDto(t, beitragMonat, laufzeit))
                .toList();
    }

    private TarifKurzDto toKurzDto(Tarif t, Integer beitragMonat, Integer laufzeit) {
        boolean mindestOk = t.getMindestbeitragMonat() == null
                || BigDecimal.valueOf(beitragMonat).compareTo(t.getMindestbeitragMonat()) >= 0;

        boolean hatKostenstruktur = kostenstrukturRepository
                .existsByTarif_IdAndBeitragMonatAndLaufzeitJahreAndAktivTrue(t.getId(), beitragMonat, laufzeit);

        boolean auswählbar = mindestOk && hatKostenstruktur;

        String hinweis = null;
        if (!mindestOk) {
            hinweis = "Mindestbeitrag nicht erreicht";
        } else if (!hatKostenstruktur) {
            hinweis = "keine Daten vorhanden";
        }

        return new TarifKurzDto(
                t.getId(),
                t.getTarifName(),
                t.getTarifCode(),
                t.getAnbieter() != null ? t.getAnbieter().getName() : "",
                auswählbar,
                hinweis
        );
    }
}