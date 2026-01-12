package de.paf.tarifvergleich.controller;

import de.paf.tarifvergleich.domain.Fonds;
import de.paf.tarifvergleich.repository.FondsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fonds")
@RequiredArgsConstructor
public class FondsController {

    private final FondsRepository fondsRepository;

    @GetMapping
    public List<Fonds> getAll() {
        return fondsRepository.findAll();
    }
}