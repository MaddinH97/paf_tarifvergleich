package de.paf.tarifvergleich.controller;

import de.paf.tarifvergleich.controller.dto.KapitalanlageKurzDto;
import de.paf.tarifvergleich.domain.Kapitalanlage;
import de.paf.tarifvergleich.repository.KapitalanlageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kapitalanlagen")
@RequiredArgsConstructor
public class KapitalanlageController {

    private final KapitalanlageRepository kapitalanlageRepository;

    // UI: nur kurze Liste (ohne 780 Werte)
    @GetMapping
    public List<KapitalanlageKurzDto> aktiv() {
        return kapitalanlageRepository.findByAktivTrueOrderByNameAsc().stream()
                .map(k -> new KapitalanlageKurzDto(k.getId(), k.getName(), k.getTyp()))
                .toList();
    }

    // Backend/Debug: volle Anlage inkl. Monatsrenditen
    @GetMapping("/{id}")
    public Kapitalanlage byId(@PathVariable Long id) {
        return kapitalanlageRepository.findById(id).orElseThrow();
    }
}