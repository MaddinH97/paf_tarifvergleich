package de.paf.tarifvergleich.controller;

import de.paf.tarifvergleich.controller.dto.BerechnungErgebnisDto;
import de.paf.tarifvergleich.controller.dto.BerechnungRequestDto;
import de.paf.tarifvergleich.service.BerechnungsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/berechnung")
@RequiredArgsConstructor
public class BerechnungController {

    private final BerechnungsService berechnungService;

    @PostMapping
    public List<BerechnungErgebnisDto> berechne(@RequestBody BerechnungRequestDto req) {
        return berechnungService.berechne(
                req.beitragMonat(),
                req.laufzeitJahre(),
                req.einstiegsalter(),
                req.tarifIds()
        );
    }
}