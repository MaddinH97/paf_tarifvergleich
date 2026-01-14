package de.paf.tarifvergleich.config;

import de.paf.tarifvergleich.domain.*;
import de.paf.tarifvergleich.repository.*;
import de.paf.tarifvergleich.service.kapitalanlage.KapitalanlageFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final FondsRepository fondsRepository;
    private final TarifRepository tarifRepository;
    private final FinanzdienstleistungsunternehmenRepository unternehmenRepository;
    private final KostenstrukturRepository kostenstrukturRepository;
    private final KapitalanlageRepository kapitalanlageRepository;

    // optional (wenn du Kostenpunkte bereits nutzt)
    private final KostenpunktRepository kostenpunktRepository;

    private static final int MONTHS_65Y = 65 * 12;

    @Override
    public void run(String... args) {

        // WICHTIG: wir brechen nur ab, wenn wirklich schon Daten da sind
        // (sonst erzeugen wir doppelt Einträge und laufen in Unique Constraints)
        if (tarifRepository.count() > 0
                || fondsRepository.count() > 0
                || kapitalanlageRepository.count() > 0
                || kostenstrukturRepository.count() > 0) {
            return;
        }

        // ===== Kapitalanlagen =====
        saveKapitalanlageIfMissing(KapitalanlageFactory.createFixed("3% Rendite", new BigDecimal("0.03")));
        saveKapitalanlageIfMissing(KapitalanlageFactory.createFixed("6% Rendite", new BigDecimal("0.06")));
        saveKapitalanlageIfMissing(KapitalanlageFactory.createFixed("9% Rendite", new BigDecimal("0.09")));
        saveKapitalanlageIfMissing(KapitalanlageFactory.createFantasyMsciWorldLike()); // ~9% p.a. über 40 Jahre

        // ===== Anbieter =====
        var anbieterA = unternehmenRepository.save(
                Finanzdienstleistungsunternehmen.builder().name("Anbieter A").build()
        );
        var anbieterB = unternehmenRepository.save(
                Finanzdienstleistungsunternehmen.builder().name("Anbieter B").build()
        );

        // ===== Fonds (dein Stand) =====
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

        // ===== Tarife =====
        // 1) Reine Fondspolice (nur Topf A)
        var tarifFonds = tarifRepository.save(
                Tarif.builder()
                        .tarifName("Tarif FondsPur")
                        .tarifCode("F-FONDS")
                        .erscheinungsjahr(2020)
                        .tarifTyp(TarifTyp.FONDS)
                        .garantiezins(new BigDecimal("0.00")) // irrelevant
                        .garantieModus(GarantieModus.OHNE_UEBERSCHUESSE)
                        .minStartalter(18)
                        .maxEndalter(67)
                        .mindestbeitragMonat(new BigDecimal("25"))
                        .aktiv(true)
                        .anbieter(anbieterA)
                        .fondsListe(List.of(f1, f3))
                        .berechnungsScript("""
                                // später Script: hier nur Platzhalter
                                return beitrag;
                                """)
                        .build()
        );

        // 2) 2-Topf Hybrid (A + B)
        var tarifHybrid2AB = tarifRepository.save(
                Tarif.builder()
                        .tarifName("Tarif Hybrid 2-Topf (A/B)")
                        .tarifCode("H2-AB")
                        .erscheinungsjahr(2021)
                        .tarifTyp(TarifTyp.HYBRID_2_TOPF)
                        .zweiterTopfTyp(TopfTyp.B)
                        .garantiezins(new BigDecimal("0.00")) // irrelevant (weil zweiter Topf=B)
                        .garantieModus(GarantieModus.OHNE_UEBERSCHUESSE)
                        .minStartalter(18)
                        .maxEndalter(67)
                        .mindestbeitragMonat(new BigDecimal("50"))
                        .aktiv(true)
                        .anbieter(anbieterB)
                        .fondsListe(List.of(f1, f2))
                        .berechnungsScript("""
                                // später Script: Platzhalter
                                return beitrag;
                                """)
                        .build()
        );

        // 3) 2-Topf Hybrid (A + Garantie)
        var tarifHybrid2AG = tarifRepository.save(
                Tarif.builder()
                        .tarifName("Tarif Hybrid 2-Topf (A/Garantie)")
                        .tarifCode("H2-AG")
                        .erscheinungsjahr(2022)
                        .tarifTyp(TarifTyp.HYBRID_2_TOPF)
                        .zweiterTopfTyp(TopfTyp.GARANTIE)
                        .garantiezins(new BigDecimal("0.0125")) // 1.25% p.a.
                        .garantieModus(GarantieModus.MIT_UEBERSCHUESSEN)
                        .minStartalter(18)
                        .maxEndalter(67)
                        .mindestbeitragMonat(new BigDecimal("25"))
                        .aktiv(true)
                        .anbieter(anbieterA)
                        .fondsListe(List.of(f1, f3, f4))
                        .berechnungsScript("""
                                // später Script: Platzhalter
                                return beitrag;
                                """)
                        .build()
        );

        // 4) 3-Topf Hybrid (A + B + Garantie)
        var tarifHybrid3 = tarifRepository.save(
                Tarif.builder()
                        .tarifName("Tarif Hybrid 3-Topf (A/B/Garantie)")
                        .tarifCode("H3-ABG")
                        .erscheinungsjahr(2023)
                        .tarifTyp(TarifTyp.HYBRID_3_TOPF)
                        .garantiezins(new BigDecimal("0.0100")) // 1.00% p.a.
                        .garantieModus(GarantieModus.OHNE_UEBERSCHUESSE)
                        .minStartalter(18)
                        .maxEndalter(67)
                        .mindestbeitragMonat(new BigDecimal("25"))
                        .aktiv(true)
                        .anbieter(anbieterB)
                        .fondsListe(List.of(f1, f2, f3))
                        .berechnungsScript("""
                                // später Script: Platzhalter
                                return beitrag;
                                """)
                        .build()
        );

        // ===== Kostenstrukturen (minimal, damit UI-Verfügbar-Filter sinnvoll ist) =====
        // Wir legen für Beitrag=50 und Laufzeit=30 für alle Tarife eine aktive Kombi an
        // (bei Mindestbeitrag 50 ist 25 nicht nötig)

        createKs(tarifFonds, 25, 30, true);
        createKs(tarifFonds, 50, 30, true);

        createKs(tarifHybrid2AB, 50, 30, true);
        createKs(tarifHybrid2AB, 100, 35, true);

        createKs(tarifHybrid2AG, 25, 30, true);
        createKs(tarifHybrid2AG, 50, 30, true);

        createKs(tarifHybrid3, 25, 30, true);
        createKs(tarifHybrid3, 50, 30, true);

        // Optional: Beispiel-Kostenpunkte für eine der Kostenstrukturen (nur wenn du Kostenpunkte bereits nutzt)
        // Wenn du es noch nicht nutzt, kannst du diesen Block löschen.
        if (kostenpunktRepository != null) {
            var ks = kostenstrukturRepository.findFirstByTarif_IdAndBeitragMonatAndLaufzeitJahreAndAktivTrue(
                    tarifFonds.getId(), 50, 30
            ).orElse(null);

            if (ks != null && kostenpunktRepository.count() == 0) {
                kostenpunktRepository.saveAll(List.of(
                        Kostenpunkt.builder()
                                .kostenstruktur(ks)
                                .code("ABSCHLUSS_5J")
                                .typ(KostenTyp.EURO)
                                .basis(KostenBasis.FIX)
                                .rhythmus(KostenRhythmus.VERTEILT_5_JAHRE)
                                .wert(new BigDecimal("600.00"))
                                .aktiv(true)
                                .build(),
                        Kostenpunkt.builder()
                                .kostenstruktur(ks)
                                .code("FIX_STUFE1")
                                .typ(KostenTyp.EURO)
                                .basis(KostenBasis.FIX)
                                .rhythmus(KostenRhythmus.MONATLICH)
                                .wert(new BigDecimal("2.00"))
                                .gueltigVonMonat(1)
                                .gueltigBisMonat(120)
                                .aktiv(true)
                                .build(),
                        Kostenpunkt.builder()
                                .kostenstruktur(ks)
                                .code("GUTHABEN_PCT")
                                .typ(KostenTyp.PROZENT)
                                .basis(KostenBasis.KAPITAL)
                                .rhythmus(KostenRhythmus.MONATLICH)
                                .wert(new BigDecimal("0.60")) // 0.60% p.a. (wenn du ProzentPeriode=JAHRLICH nutzt)
                                .prozentPeriode(ProzentPeriode.JAHRLICH)
                                .minimumEuro(new BigDecimal("0.50"))
                                .aktiv(true)
                                .build()
                ));
            }
        }
    }

    private void createKs(Tarif tarif, int beitrag, int laufzeit, boolean aktiv) {
        kostenstrukturRepository.save(
                Kostenstruktur.builder()
                        .tarif(tarif)
                        .beitragMonat(beitrag)
                        .laufzeitJahre(laufzeit)
                        .aktiv(aktiv)
                        // alte Felder bleiben (können später entfernt werden)
                        .abschlusskosten(BigDecimal.ZERO)
                        .verwaltungskosten(BigDecimal.ZERO)
                        .fondskosten(BigDecimal.ZERO)
                        .risikokosten(BigDecimal.ZERO)
                        .sonstigeKosten(BigDecimal.ZERO)
                        .build()
        );
    }

    private void saveKapitalanlageIfMissing(Kapitalanlage k) {
        kapitalanlageRepository.findByName(k.getName())
                .orElseGet(() -> kapitalanlageRepository.save(k));
    }

    // Falls du die monthlyRateFromAnnual später wieder brauchst
    @SuppressWarnings("unused")
    private BigDecimal monthlyRateFromAnnual(BigDecimal annualRate) {
        double rY = annualRate.doubleValue();
        double rM = Math.pow(1.0 + rY, 1.0 / 12.0) - 1.0;
        return new BigDecimal(rM, new MathContext(20, RoundingMode.HALF_UP));
    }
}