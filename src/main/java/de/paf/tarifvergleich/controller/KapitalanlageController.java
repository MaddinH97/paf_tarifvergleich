package de.paf.tarifvergleich.controller;

import de.paf.tarifvergleich.controller.dto.KapitalanlageKurzDto;
import de.paf.tarifvergleich.domain.Kapitalanlage;
import de.paf.tarifvergleich.repository.KapitalanlageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kapitalanlagen")
@Slf4j
public class KapitalanlageController {

    private final KapitalanlageRepository kapitalanlageRepository;

    @GetMapping
    public List<KapitalanlageKurzDto> alleAktivenKurz() {
        try {
            List<Kapitalanlage> list = kapitalanlageRepository.findAll();

            return list.stream()
                    .filter(k -> k != null && k.isAktiv())
                    .sorted(Comparator.comparing(
                            k -> k.getName() == null ? "" : k.getName(),
                            String.CASE_INSENSITIVE_ORDER
                    ))
                    .map(k -> new KapitalanlageKurzDto(
                            k.getId(),
                            k.getName(),
                            k.getTyp() != null ? k.getTyp().name() : ""
                    ))
                    .toList();

        } catch (Exception e) {
            log.error("Fehler in GET /api/kapitalanlagen", e);
            throw e; // damit du im Log den echten Stacktrace siehst
        }
    }
}