package de.paf.tarifvergleich.controller.admin;

import de.paf.tarifvergleich.controller.admin.dto.*;
import de.paf.tarifvergleich.domain.*;
import de.paf.tarifvergleich.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TarifRepository tarifRepository;
    private final FinanzdienstleistungsunternehmenRepository unternehmenRepository;
    private final FondsRepository fondsRepository;

    private final KostenstrukturRepository kostenstrukturRepository;
    private final KostenpunktRepository kostenpunktRepository;

    private final KapitalanlageRepository kapitalanlageRepository;

    // =========================
    // TARIFE
    // =========================

    @GetMapping("/tarife")
    public List<AdminTarifDto> listTarife() {
        return tarifRepository.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/tarife/{id}")
    public AdminTarifDto getTarif(@PathVariable Long id) {
        return toDto(tarifRepository.findById(id).orElseThrow());
    }

    @PostMapping("/tarife")
    @Transactional
    public AdminTarifDto createTarif(@RequestBody AdminTarifDto dto) {
        Tarif t = new Tarif();
        apply(dto, t);
        return toDto(tarifRepository.save(t));
    }

    @PutMapping("/tarife/{id}")
    @Transactional
    public AdminTarifDto updateTarif(@PathVariable Long id, @RequestBody AdminTarifDto dto) {
        Tarif t = tarifRepository.findById(id).orElseThrow();
        apply(dto, t);
        return toDto(t);
    }

    @DeleteMapping("/tarife/{id}")
    public void deleteTarif(@PathVariable Long id) {
        tarifRepository.deleteById(id);
    }

    // =========================
    // KOSTENSTRUKTUREN
    // =========================

    @GetMapping("/kostenstrukturen")
    public List<AdminKostenstrukturDto> listKostenstrukturen(@RequestParam(required = false) Long tarifId) {
        List<Kostenstruktur> list = (tarifId == null)
                ? kostenstrukturRepository.findAll()
                : kostenstrukturRepository.findByTarif_Id(tarifId);

        return list.stream().map(this::toDto).toList();
    }

    @PostMapping("/kostenstrukturen")
    @Transactional
    public AdminKostenstrukturDto createKostenstruktur(@RequestBody AdminKostenstrukturDto dto) {
        Tarif tarif = tarifRepository.findById(dto.tarifId()).orElseThrow();

        Kostenstruktur k = Kostenstruktur.builder()
                .tarif(tarif)
                .beitragMonat(dto.beitragMonat())
                .laufzeitJahre(dto.laufzeitJahre())
                .aktiv(dto.aktiv())
                .abschlusskosten(dto.abschlusskosten())
                .verwaltungskosten(dto.verwaltungskosten())
                .fondskosten(dto.fondskosten())
                .risikokosten(dto.risikokosten())
                .sonstigeKosten(dto.sonstigeKosten())
                .build();

        return toDto(kostenstrukturRepository.save(k));
    }

    @PutMapping("/kostenstrukturen/{id}")
    @Transactional
    public AdminKostenstrukturDto updateKostenstruktur(@PathVariable Long id, @RequestBody AdminKostenstrukturDto dto) {
        Kostenstruktur k = kostenstrukturRepository.findById(id).orElseThrow();

        k.setBeitragMonat(dto.beitragMonat());
        k.setLaufzeitJahre(dto.laufzeitJahre());
        k.setAktiv(dto.aktiv());

        k.setAbschlusskosten(dto.abschlusskosten());
        k.setVerwaltungskosten(dto.verwaltungskosten());
        k.setFondskosten(dto.fondskosten());
        k.setRisikokosten(dto.risikokosten());
        k.setSonstigeKosten(dto.sonstigeKosten());

        return toDto(k);
    }

    @DeleteMapping("/kostenstrukturen/{id}")
    public void deleteKostenstruktur(@PathVariable Long id) {
        kostenstrukturRepository.deleteById(id);
    }

    // =========================
    // KOSTENPUNKTE
    // =========================

    @GetMapping("/kostenpunkte")
    public List<AdminKostenpunktDto> listKostenpunkte(@RequestParam Long kostenstrukturId) {
        return kostenpunktRepository.findByKostenstruktur_Id(kostenstrukturId).stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping("/kostenpunkte")
    @Transactional
    public AdminKostenpunktDto createKostenpunkt(@RequestBody AdminKostenpunktDto dto) {
        Kostenstruktur ks = kostenstrukturRepository.findById(dto.kostenstrukturId()).orElseThrow();

        Kostenpunkt kp = Kostenpunkt.builder()
                .kostenstruktur(ks)
                .code(dto.name())
                .aktiv(dto.aktiv())
                .rhythmus(dto.rhythmus())
                .typ(dto.typ())
                .basis(dto.basis())
                .wert(dto.wert())
                .build();

        return toDto(kostenpunktRepository.save(kp));
    }

    @PutMapping("/kostenpunkte/{id}")
    @Transactional
    public AdminKostenpunktDto updateKostenpunkt(@PathVariable Long id, @RequestBody AdminKostenpunktDto dto) {
        Kostenpunkt kp = kostenpunktRepository.findById(id).orElseThrow();

        kp.setCode(dto.name());
        kp.setAktiv(dto.aktiv());
        kp.setRhythmus(dto.rhythmus());
        kp.setTyp(dto.typ());
        kp.setBasis(dto.basis());
        kp.setWert(dto.wert());

        return toDto(kp);
    }

    @DeleteMapping("/kostenpunkte/{id}")
    public void deleteKostenpunkt(@PathVariable Long id) {
        kostenpunktRepository.deleteById(id);
    }

    // =========================
    // KAPITALANLAGEN
    // =========================

    @GetMapping("/kapitalanlagen")
    public List<AdminKapitalanlageDto> listKapitalanlagen() {
        return kapitalanlageRepository.findAll().stream().map(this::toDto).toList();
    }

    @PutMapping("/kapitalanlagen/{id}")
    @Transactional
    public AdminKapitalanlageDto updateKapitalanlage(@PathVariable Long id, @RequestBody AdminKapitalanlageDto dto) {
        Kapitalanlage k = kapitalanlageRepository.findById(id).orElseThrow();
        k.setName(dto.name());
        k.setTyp(dto.typ());
        k.setAnnualRate(dto.annualRate());
        k.setAktiv(dto.aktiv());
        return toDto(k);
    }

    // =========================
    // MAPPER
    // =========================

    private AdminTarifDto toDto(Tarif t) {
        List<Long> fondsIds = (t.getFondsListe() == null) ? List.of() : t.getFondsListe().stream().map(Fonds::getId).toList();
        List<String> fondsNames = (t.getFondsListe() == null) ? List.of() : t.getFondsListe().stream().map(Fonds::getName).toList();

        return new AdminTarifDto(
                t.getId(),
                t.getTarifName(),
                t.getTarifCode(),
                t.getErscheinungsjahr(),
                t.getAktiv(),
                t.getGarantiezins(),
                t.getTarifTyp(),
                t.getGarantieModus(),
                t.getGarantieNiveau(),
                t.getTopfBFloor(),
                t.getMinStartalter(),
                t.getMaxEndalter(),
                t.getMindestbeitragMonat(),
                t.getAnbieter() != null ? t.getAnbieter().getId() : null,
                t.getAnbieter() != null ? t.getAnbieter().getName() : null,
                fondsIds,
                fondsNames
        );
    }

    private void apply(AdminTarifDto dto, Tarif t) {
        t.setTarifName(dto.tarifName());
        t.setTarifCode(dto.tarifCode());
        t.setErscheinungsjahr(dto.erscheinungsjahr());
        t.setAktiv(dto.aktiv());

        t.setGarantiezins(nz(dto.garantiezins()));
        t.setTarifTyp(dto.tarifTyp() != null ? dto.tarifTyp() : TarifTyp.FONDS);
        t.setGarantieModus(dto.garantieModus() != null ? dto.garantieModus() : GarantieModus.OHNE_UEBERSCHUESSE);
        t.setGarantieNiveau(nz(dto.garantieNiveau()));
        t.setTopfBFloor(nz(dto.topfBFloor()));

        t.setMinStartalter(dto.minStartalter());
        t.setMaxEndalter(dto.maxEndalter());
        t.setMindestbeitragMonat(dto.mindestbeitragMonat());

        if (dto.anbieterId() != null) {
            Finanzdienstleistungsunternehmen a = unternehmenRepository.findById(dto.anbieterId()).orElseThrow();
            t.setAnbieter(a);
        } else {
            t.setAnbieter(null);
        }

        if (dto.fondsIds() != null && !dto.fondsIds().isEmpty()) {
            List<Fonds> fonds = fondsRepository.findAllById(dto.fondsIds());
            t.setFondsListe(fonds);
        } else {
            t.setFondsListe(List.of());
        }
    }

    private AdminKostenstrukturDto toDto(Kostenstruktur k) {
        return new AdminKostenstrukturDto(
                k.getId(),
                k.getTarif() != null ? k.getTarif().getId() : null,
                k.getTarif() != null ? k.getTarif().getTarifName() : null,
                k.getBeitragMonat(),
                k.getLaufzeitJahre(),
                k.isAktiv(),
                k.getAbschlusskosten(),
                k.getVerwaltungskosten(),
                k.getFondskosten(),
                k.getRisikokosten(),
                k.getSonstigeKosten()
        );
    }

    private AdminKostenpunktDto toDto(Kostenpunkt p) {
        return new AdminKostenpunktDto(
                p.getId(),
                p.getKostenstruktur() != null ? p.getKostenstruktur().getId() : null,
                p.getCode(),
                p.isAktiv(),
                p.getRhythmus(),
                p.getTyp(),
                p.getBasis(),
                p.getWert()
        );
    }

    private AdminKapitalanlageDto toDto(Kapitalanlage k) {
        return new AdminKapitalanlageDto(
                k.getId(),
                k.getName(),
                k.getTyp(),
                k.getAnnualRate(),
                k.isAktiv()
        );
    }

    private static java.math.BigDecimal nz(java.math.BigDecimal v) {
        return v == null ? java.math.BigDecimal.ZERO : v;
    }
}