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

        // =========================
        // Scripts (GraalJS-kompatibel)
        // =========================

        // Tarif Komfort: etwas "teurer", dafür leicht höhere Rendite (Demo)
        String scriptKomfort = """
                function calc(input) {
                  const beitrag = input.beitrag;      // int (z.B. 50)
                  const laufzeit = input.laufzeit;    // int (z.B. 30)
                  const kosten  = input.kosten || {};
                
                  // simple Demo-Parameter:
                  // - Rendite p.a. (hier fix, später abhängig von Fonds etc.)
                  const renditePa = 0.035; // 3.5% p.a.
                
                  let kapital = 0;
                  const punkte = [{ jahr: 0, wert: 0 }];
                
                  // Abschlusskosten einmalig am Anfang (Demo):
                  kapital -= (kosten.abschlusskosten || 0);
                
                  for (let j = 1; j <= laufzeit; j++) {
                    // jährliche Einzahlung
                    kapital += beitrag * 12;
                
                    // Verwaltungskosten & Sonstige: wir interpretieren sie hier als "Euro pro Monat" (Demo)
                    const vk = (kosten.verwaltungskosten || 0) * 12;
                    const sk = (kosten.sonstigeKosten || 0) * 12;
                    kapital -= (vk + sk);
                
                    // Fondskosten/Risikokosten: wir interpretieren sie als "Prozent p.a." in Dezimalform (Demo)
                    const fondProzent = (kosten.fondskosten || 0);   // z.B. 0.012
                    const risikoProzent = (kosten.risikokosten || 0); // z.B. 0.005
                
                    // Rendite nach Kosten
                    const nettoRendite = renditePa - fondProzent - risikoProzent;
                    kapital *= (1 + nettoRendite);
                
                    punkte.push({ jahr: j, wert: kapital });
                  }
                
                  return { endwert: kapital, punkte: punkte };
                }
                """;

        // Tarif Flex: Mindestbeitrag 50, etwas niedrigere Rendite (Demo)
        String scriptFlex = """
                function calc(input) {
                  const beitrag = input.beitrag;
                  const laufzeit = input.laufzeit;
                  const kosten  = input.kosten || {};
                
                  const renditePa = 0.028; // 2.8% p.a.
                
                  let kapital = 0;
                  const punkte = [{ jahr: 0, wert: 0 }];
                
                  // Abschlusskosten einmalig am Anfang (Demo)
                  kapital -= (kosten.abschlusskosten || 0);
                
                  for (let j = 1; j <= laufzeit; j++) {
                    kapital += beitrag * 12;
                
                    // etwas andere Interpretation (Demo): Verwaltungskosten als Euro/Jahr
                    const vkJahr = (kosten.verwaltungskosten || 0) * 12;
                    kapital -= vkJahr;
                
                    // Fondskosten/Risikokosten als Prozent p.a.
                    const fondProzent = (kosten.fondskosten || 0);
                    const risikoProzent = (kosten.risikokosten || 0);
                
                    const nettoRendite = renditePa - fondProzent - risikoProzent;
                    kapital *= (1 + nettoRendite);
                
                    punkte.push({ jahr: j, wert: kapital });
                  }
                
                  return { endwert: kapital, punkte: punkte };
                }
                """;

        // =========================
        // Tarife speichern
        // =========================

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
                        .berechnungsScript(scriptKomfort)
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
                        .mindestbeitragMonat(new BigDecimal("50")) // Mindestbeitrag 50
                        .aktiv(true)
                        .anbieter(anbieterB)
                        .fondsListe(List.of(f1, f2))
                        .berechnungsScript(scriptFlex)
                        .build()
        );

        // =========================
        // Kostenstrukturen (Grid)
        // =========================
        kostenstrukturRepository.saveAll(List.of(
                // Komfort: 25/30
                Kostenstruktur.builder()
                        .tarif(tarifKomfort)
                        .beitragMonat(25)
                        .laufzeitJahre(30)
                        .aktiv(true)
                        // abschlusskosten als EURO einmalig (Demo)
                        .abschlusskosten(new BigDecimal("500"))
                        // verwaltungskosten als EURO/Monat (Demo)
                        .verwaltungskosten(new BigDecimal("2.50"))
                        // fondskosten als PROZENT p.a. in Dezimalform (Demo) -> 1.20% = 0.0120
                        .fondskosten(new BigDecimal("0.0120"))
                        // risikokosten als PROZENT p.a. -> 0.50% = 0.0050
                        .risikokosten(new BigDecimal("0.0050"))
                        .sonstigeKosten(BigDecimal.ZERO)
                        .build(),

                // Komfort: 50/30
                Kostenstruktur.builder()
                        .tarif(tarifKomfort)
                        .beitragMonat(50)
                        .laufzeitJahre(30)
                        .aktiv(true)
                        .abschlusskosten(new BigDecimal("450"))
                        .verwaltungskosten(new BigDecimal("2.30"))
                        .fondskosten(new BigDecimal("0.0110"))
                        .risikokosten(new BigDecimal("0.0045"))
                        .sonstigeKosten(BigDecimal.ZERO)
                        .build(),

                // Flex: Mindestbeitrag 50 -> 25 absichtlich nicht vorhanden
                // Flex: 50/30
                Kostenstruktur.builder()
                        .tarif(tarifFlex)
                        .beitragMonat(50)
                        .laufzeitJahre(30)
                        .aktiv(true)
                        .abschlusskosten(new BigDecimal("300"))
                        .verwaltungskosten(new BigDecimal("2.00"))
                        .fondskosten(new BigDecimal("0.0100"))
                        .risikokosten(new BigDecimal("0.0030"))
                        .sonstigeKosten(BigDecimal.ZERO)
                        .build(),

                // Flex: 100/35
                Kostenstruktur.builder()
                        .tarif(tarifFlex)
                        .beitragMonat(100)
                        .laufzeitJahre(35)
                        .aktiv(true)
                        .abschlusskosten(new BigDecimal("280"))
                        .verwaltungskosten(new BigDecimal("1.90"))
                        .fondskosten(new BigDecimal("0.0095"))
                        .risikokosten(new BigDecimal("0.0028"))
                        .sonstigeKosten(BigDecimal.ZERO)
                        .build()
        ));
    }
}