package de.paf.tarifvergleich.config;

import de.paf.tarifvergleich.domain.*;
import de.paf.tarifvergleich.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final FondsRepository fondsRepository;
    private final TarifRepository tarifRepository;
    private final FinanzdienstleistungsunternehmenRepository unternehmenRepository;
    private final KostenstrukturRepository kostenstrukturRepository;

    @Override
    public void run(String... args) {
        if (tarifRepository.count() > 0 || fondsRepository.count() > 0) return;

        // Anbieter
        var anbieterA = unternehmenRepository.save(
                Finanzdienstleistungsunternehmen.builder().name("Anbieter A").build()
        );
        var anbieterB = unternehmenRepository.save(
                Finanzdienstleistungsunternehmen.builder().name("Anbieter B").build()
        );

        // Fonds
        var f1 = fondsRepository.save(Fonds.builder()
                .isin("DE000F000001")
                .name("Global Growth")
                .typ("Aktienfonds")
                .risikoklasse("hoch")
                .erwarteteRenditePa(new BigDecimal("0.06"))
                .build());

        var f2 = fondsRepository.save(Fonds.builder()
                .isin("DE000F000002")
                .name("European Equity")
                .typ("Aktienfonds")
                .risikoklasse("hoch")
                .erwarteteRenditePa(new BigDecimal("0.055"))
                .build());

        var f3 = fondsRepository.save(Fonds.builder()
                .isin("DE000F000003")
                .name("Nachhaltigkeit Welt")
                .typ("Mischfonds")
                .risikoklasse("mittel")
                .erwarteteRenditePa(new BigDecimal("0.04"))
                .build());

        var f4 = fondsRepository.save(Fonds.builder()
                .isin("DE000F000004")
                .name("Defensiv Mix")
                .typ("Mischfonds")
                .risikoklasse("niedrig")
                .erwarteteRenditePa(new BigDecimal("0.03"))
                .build());

        // Tarife zuerst speichern (ohne Kostenstruktur-Objekt direkt)
        var tarifKomfort = tarifRepository.save(
                Tarif.builder()
                        .tarifName("Tarif Komfort")
                        .tarifCode("A-KOMFORT")
                        .erscheinungsjahr(2020)
                        .garantiezins(new BigDecimal("0.0125"))
                        .minStartalter(18)
                        .maxEndalter(67)
                        .mindestbeitragMonat(new BigDecimal("25"))
                        .aktiv(true)
                        .anbieter(anbieterA)
                        .fondsListe(List.of(f1, f3, f4))
                        .berechnungsScript("""
                                // Beispiel-Logik
                                if (laufzeit < 20) {
                                    return beitrag * 0.95;
                                }
                                return beitrag * 1.05;
                                """)
                        .build()
        );

        var tarifFlex = tarifRepository.save(
                Tarif.builder()
                        .tarifName("Tarif Flex")
                        .tarifCode("B-FLEX")
                        .erscheinungsjahr(2021)
                        .garantiezins(new BigDecimal("0.0100"))
                        .minStartalter(18)
                        .maxEndalter(67)
                        .mindestbeitragMonat(new BigDecimal("50")) // Beispiel: Mindestbeitrag 50
                        .aktiv(true)
                        .anbieter(anbieterB)
                        .fondsListe(List.of(f1, f2))
                        .berechnungsScript("""
                                // Beispiel-Logik
                                if (laufzeit < 25) {
                                    return beitrag * 0.97;
                                }
                                return beitrag * 1.02;
                                """)
                        .build()
        );

        // Kostenstrukturen: jetzt immer mit tarif + beitragMonat + laufzeitJahre
        // Minimal ein paar Kombis, damit das System startet.
        // (Später füllen wir alle Pflichtkombis.)
        kostenstrukturRepository.saveAll(List.of(
                Kostenstruktur.builder()
                        .tarif(tarifKomfort)
                        .beitragMonat(25)
                        .laufzeitJahre(30)
                        .aktiv(true)
                        .abschlusskosten(new BigDecimal("500"))
                        .verwaltungskosten(new BigDecimal("2.50"))
                        .fondskosten(new BigDecimal("1.20"))
                        .risikokosten(new BigDecimal("0.50"))
                        .sonstigeKosten(BigDecimal.ZERO)
                        .build(),

                Kostenstruktur.builder()
                        .tarif(tarifKomfort)
                        .beitragMonat(50)
                        .laufzeitJahre(30)
                        .aktiv(true)
                        .abschlusskosten(new BigDecimal("450"))
                        .verwaltungskosten(new BigDecimal("2.30"))
                        .fondskosten(new BigDecimal("1.10"))
                        .risikokosten(new BigDecimal("0.45"))
                        .sonstigeKosten(BigDecimal.ZERO)
                        .build(),

                // Tarif Flex: Mindestbeitrag 50 -> 25 lassen wir absichtlich weg
                Kostenstruktur.builder()
                        .tarif(tarifFlex)
                        .beitragMonat(50)
                        .laufzeitJahre(30)
                        .aktiv(true)
                        .abschlusskosten(new BigDecimal("300"))
                        .verwaltungskosten(new BigDecimal("2.00"))
                        .fondskosten(new BigDecimal("1.00"))
                        .risikokosten(new BigDecimal("0.30"))
                        .sonstigeKosten(BigDecimal.ZERO)
                        .build(),

                Kostenstruktur.builder()
                        .tarif(tarifFlex)
                        .beitragMonat(100)
                        .laufzeitJahre(35)
                        .aktiv(true)
                        .abschlusskosten(new BigDecimal("280"))
                        .verwaltungskosten(new BigDecimal("1.90"))
                        .fondskosten(new BigDecimal("0.95"))
                        .risikokosten(new BigDecimal("0.28"))
                        .sonstigeKosten(BigDecimal.ZERO)
                        .build()
        ));
    }
}