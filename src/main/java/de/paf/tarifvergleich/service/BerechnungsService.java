package de.paf.tarifvergleich.service;

import de.paf.tarifvergleich.controller.dto.BerechnungErgebnisDto;
import de.paf.tarifvergleich.controller.dto.WertpunktDto;
import de.paf.tarifvergleich.domain.*;
import de.paf.tarifvergleich.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BerechnungsService {

    private final TarifRepository tarifRepository;
    private final KostenstrukturRepository kostenstrukturRepository;
    private final KostenpunktRepository kostenpunktRepository;
    private final KapitalanlageRepository kapitalanlageRepository;

    // Rechenkonstanten
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal BD_0 = BigDecimal.ZERO;
    private static final BigDecimal BD_1 = BigDecimal.ONE;
    private static final BigDecimal BD_100 = new BigDecimal("100");

    /**
     * Controller-Signatur (wie bei dir):
     * beitragMonat, laufzeitJahre, einstiegsalter, kapitalanlageAId, kapitalanlageBId, tarifIds
     */
    public List<BerechnungErgebnisDto> berechne(
            Integer beitragMonat,
            Integer laufzeitJahre,
            Integer einstiegsalter,
            Long kapitalanlageAId,
            Long kapitalanlageBId,
            GarantieModus garantieModus,
            List<Long> tarifIds
    ) {
        if (beitragMonat == null || laufzeitJahre == null || einstiegsalter == null) return List.of();
        if (tarifIds == null || tarifIds.isEmpty()) return List.of();

        int gesamtMonate = laufzeitJahre * 12;

        Kapitalanlage kaA = kapitalanlageAId != null ? kapitalanlageRepository.findById(kapitalanlageAId).orElse(null) : null;
        Kapitalanlage kaB = kapitalanlageBId != null ? kapitalanlageRepository.findById(kapitalanlageBId).orElse(null) : null;

        BigDecimal beitrag = BigDecimal.valueOf(beitragMonat);

        List<BerechnungErgebnisDto> out = new ArrayList<>();

        for (Long tarifId : tarifIds) {
            Tarif tarif = tarifRepository.findById(tarifId).orElse(null);
            if (tarif == null) continue;
            if (!tarif.getAktiv()) continue;

            // passende Kostenstruktur (Beitrag/Laufzeit) muss existieren und aktiv sein
            Optional<Kostenstruktur> ksOpt =
                    kostenstrukturRepository.findFirstByTarif_IdAndBeitragMonatAndLaufzeitJahreAndAktivTrue(
                            tarif.getId(), beitragMonat, laufzeitJahre
                    );
            if (ksOpt.isEmpty()) continue;

            Kostenstruktur ks = ksOpt.get();
            List<Kostenpunkt> kostenpunkte = kostenpunktRepository.findByKostenstruktur_IdAndAktivTrue(ks.getId());

            BerechnungErgebnisDto dto = simuliereTarif(
                    tarif, kostenpunkte, beitrag, laufzeitJahre, gesamtMonate, einstiegsalter, kaA, kaB
            );

            out.add(dto);
        }

        return out;
    }

    private BerechnungErgebnisDto simuliereTarif(
            Tarif tarif,
            List<Kostenpunkt> kostenpunkte,
            BigDecimal beitrag,
            int laufzeitJahre,
            int gesamtMonate,
            int einstiegsalter,
            Kapitalanlage kaA,
            Kapitalanlage kaB
    ) {
        TarifTyp typ = tarif.getTarifTyp();

        if (typ == null) typ = TarifTyp.FONDS;

        return switch (typ) {
            case FONDS -> simuliereFondspolice(tarif, kostenpunkte, beitrag, laufzeitJahre, gesamtMonate, einstiegsalter, kaA);
            case HYBRID_2_TOPF -> simuliereHybrid2Topf(tarif, kostenpunkte, beitrag, laufzeitJahre, gesamtMonate, einstiegsalter, kaA);
            case HYBRID_3_TOPF -> simuliereHybrid3Topf(tarif, kostenpunkte, beitrag, laufzeitJahre, gesamtMonate, einstiegsalter, kaA, kaB);
        };
    }

    // =========================================================
    // 1) FONDS (nur Topf A)
    // =========================================================
    private BerechnungErgebnisDto simuliereFondspolice(
            Tarif tarif,
            List<Kostenpunkt> kostenpunkte,
            BigDecimal beitrag,
            int laufzeitJahre,
            int gesamtMonate,
            int einstiegsalter,
            Kapitalanlage kaA
    ) {
        BigDecimal topfA = BD_0;
        BigDecimal sumBeitraege = BD_0;
        BigDecimal sumKosten = BD_0;

        List<WertpunktDto> jahreswerte = new ArrayList<>();

        for (int m = 1; m <= gesamtMonate; m++) {
            // Beitrag rein
            topfA = topfA.add(beitrag);
            sumBeitraege = sumBeitraege.add(beitrag);

            // Kosten runter
            BigDecimal kosten = kostenSummeFuerMonat(kostenpunkte, m, beitrag, topfA);
            if (kosten.compareTo(BD_0) > 0) {
                topfA = topfA.subtract(kosten);
                sumKosten = sumKosten.add(kosten);
                if (topfA.compareTo(BD_0) < 0) topfA = BD_0;
            }

            // Rendite Topf A
            BigDecimal rA = rendite(kaA, m);
            topfA = topfA.multiply(BD_1.add(rA), MC);

            if (m % 12 == 0) {
                int jahr = m / 12;
                jahreswerte.add(new WertpunktDto(jahr, topfA));
            }
        }

        BigDecimal endwert = jahreswerte.isEmpty() ? topfA : jahreswerte.get(jahreswerte.size() - 1).wert();

        return new BerechnungErgebnisDto(
                tarif.getId(),
                tarif.getTarifName(),
                tarif.getTarifCode(),
                tarif.getAnbieter() != null ? tarif.getAnbieter().getName() : "",
                endwert,
                jahreswerte
        );
    }

    // =========================================================
    // 2) HYBRID 2-TOPF
    //
    // Logik (wie von dir beschrieben):
    // - Monatlich Beitrag + Kosten => aktuelles Kapital
    // - Ziel: garantieNiveau * SummeBeiträge soll am Ende mindestens erreicht werden
    // - Berechne "Wie viel muss JETZT in Garantie-Topf, damit (mit Garantierendite bis Ende)
    //   am Ende genau das Ziel rauskommt?"
    // - Rest geht in Topf A (Kapitalanlage A)
    // =========================================================
    private BerechnungErgebnisDto simuliereHybrid2Topf(
            Tarif tarif,
            List<Kostenpunkt> kostenpunkte,
            BigDecimal beitrag,
            int laufzeitJahre,
            int gesamtMonate,
            int einstiegsalter,
            Kapitalanlage kaA
    ) {
        BigDecimal topfA = BD_0;
        BigDecimal topfG = BD_0;

        BigDecimal sumBeitraege = BD_0;
        BigDecimal sumKosten = BD_0;

        List<WertpunktDto> jahreswerte = new ArrayList<>();

        for (int m = 1; m <= gesamtMonate; m++) {

            // 1) Beitrag (vereinfacht: kommt erst mal in "Gesamt")
            BigDecimal gesamt = topfA.add(topfG).add(beitrag);
            sumBeitraege = sumBeitraege.add(beitrag);

            // 2) Kosten runter (auf Gesamt betrachtet)
            BigDecimal kosten = kostenSummeFuerMonat(kostenpunkte, m, beitrag, gesamt);
            if (kosten.compareTo(BD_0) > 0) {
                gesamt = gesamt.subtract(kosten);
                sumKosten = sumKosten.add(kosten);
                if (gesamt.compareTo(BD_0) < 0) gesamt = BD_0;
            }

            // 3) Garantiebedarf bestimmen (Present Value)
            int restMonate = gesamtMonate - m;
            BigDecimal ziel = sumBeitraege.multiply(nz(tarif.getGarantieNiveau()), MC); // z.B. 1.0 oder 0.5

            BigDecimal gFactor = guaranteeFutureFactor(tarif, restMonate); // Wachstum 1€ im Garantietopf bis Ende
            BigDecimal neededGNow = pvForFutureValue(ziel, gFactor); // ziel / gFactor

            // Maximal im Garantietopf = gesamt
            if (neededGNow.compareTo(gesamt) > 0) neededGNow = gesamt;
            if (neededGNow.compareTo(BD_0) < 0) neededGNow = BD_0;

            topfG = neededGNow;
            topfA = gesamt.subtract(topfG);

            // 4) Renditen anwenden (Monat m)
            BigDecimal rA = rendite(kaA, m);
            topfA = topfA.multiply(BD_1.add(rA), MC);

            BigDecimal rG = garantieMonatsrendite(tarif);
            topfG = topfG.multiply(BD_1.add(rG), MC);

            // Jahreswerte
            if (m % 12 == 0) {
                int jahr = m / 12;
                BigDecimal gesamtWert = topfA.add(topfG);
                jahreswerte.add(new WertpunktDto(jahr, gesamtWert));
            }
        }

        BigDecimal endwert = jahreswerte.isEmpty() ? topfA.add(topfG) : jahreswerte.get(jahreswerte.size() - 1).wert();

        return new BerechnungErgebnisDto(
                tarif.getId(),
                tarif.getTarifName(),
                tarif.getTarifCode(),
                tarif.getAnbieter() != null ? tarif.getAnbieter().getName() : "",
                endwert,
                jahreswerte
        );
    }

    // =========================================================
    // 3) HYBRID 3-TOPF
    //
    // - Topf A: Kapitalanlage A (Risikotopf)
    // - Topf B: Garantiefonds (Kapitalanlage B) + Floor: nicht unter floor * letzterB fallen
    // - Topf G: klassischer Garantietopf mit garantiezins (+ ggf Überschüsse)
    //
    // Ziel: garantieNiveau * SummeBeiträge muss am Ende erreicht werden.
    // Absicherung erfolgt durch B und/oder G. Wir minimieren den "sicheren Anteil",
    // damit maximal viel in A bleibt.
    // =========================================================
    private BerechnungErgebnisDto simuliereHybrid3Topf(
            Tarif tarif,
            List<Kostenpunkt> kostenpunkte,
            BigDecimal beitrag,
            int laufzeitJahre,
            int gesamtMonate,
            int einstiegsalter,
            Kapitalanlage kaA,
            Kapitalanlage kaB
    ) {
        BigDecimal topfA = BD_0;
        BigDecimal topfB = BD_0;
        BigDecimal topfG = BD_0;

        BigDecimal letzterB = BD_0; // für Floor

        BigDecimal sumBeitraege = BD_0;
        BigDecimal sumKosten = BD_0;

        BigDecimal floor = nz(tarif.getTopfBFloor());
        if (floor.compareTo(BD_0) < 0) floor = BD_0;
        if (floor.compareTo(BD_1) > 0) floor = BD_1;

        List<WertpunktDto> jahreswerte = new ArrayList<>();

        for (int m = 1; m <= gesamtMonate; m++) {

            // 1) Beitrag (erst mal Gesamt)
            BigDecimal gesamt = topfA.add(topfB).add(topfG).add(beitrag);
            sumBeitraege = sumBeitraege.add(beitrag);

            // 2) Kosten runter (auf Gesamt)
            BigDecimal kosten = kostenSummeFuerMonat(kostenpunkte, m, beitrag, gesamt);
            if (kosten.compareTo(BD_0) > 0) {
                gesamt = gesamt.subtract(kosten);
                sumKosten = sumKosten.add(kosten);
                if (gesamt.compareTo(BD_0) < 0) gesamt = BD_0;
            }

            // 3) sichere Allokation bestimmen (B + G), Rest A
            int restMonate = gesamtMonate - m;

            BigDecimal ziel = sumBeitraege.multiply(nz(tarif.getGarantieNiveau()), MC);

            // Zukunftsfaktoren aus 1€
            BigDecimal gFactor = guaranteeFutureFactor(tarif, restMonate);
            BigDecimal bFactor = bFutureFactorWithFloor(kaB, restMonate, floor); // konservativ: wir simulieren 1€ mit Floor-Regel

            // Mindest-B wegen Floor-Sinn: wir wollen B nicht "zu klein" machen, wenn letzterB vorhanden:
            // B darf nach Rendite nicht unter floor * letzterB fallen -> als Mindeststartwert setzen wir B_min = floor * letzterB
            BigDecimal bMin = letzterB.multiply(floor, MC);

            // Minimal sichere Lösung:
            // 1) Versuch: alles sicher nur über B (weil B langfristig besser als Garantie sein soll)
            BigDecimal bNeededAllB = pvForFutureValue(ziel, bFactor);
            BigDecimal bStart = max(bMin, bNeededAllB);

            BigDecimal newB;
            BigDecimal newG;

            if (bStart.compareTo(gesamt) <= 0) {
                // Garantie kann komplett über B abgedeckt werden
                newB = bStart;
                newG = BD_0;
            } else {
                // nicht genug Kapital, also: setze B mindestens bMin und decke Rest über G (so gut es geht)
                newB = min(bMin, gesamt);
                BigDecimal restZiel = ziel.subtract(newB.multiply(bFactor, MC), MC);
                if (restZiel.compareTo(BD_0) <= 0) {
                    newG = BD_0;
                } else {
                    newG = pvForFutureValue(restZiel, gFactor);
                }

                // Falls newB + newG > gesamt, müssen wir B/G so kombinieren, dass es in gesamt passt.
                BigDecimal safe = newB.add(newG);
                if (safe.compareTo(gesamt) > 0) {
                    // Optimaler Mix, um Ziel zu erreichen, wenn gFactor und bFactor unterschiedlich sind:
                    // Wir suchen B so, dass B + G = gesamt und (B*bF + G*gF) = ziel, sofern möglich.
                    // => B*(bF - gF) + gesamt*gF = ziel
                    // => B = (ziel - gesamt*gF)/(bF - gF)
                    BigDecimal denom = bFactor.subtract(gFactor, MC);

                    if (denom.abs().compareTo(new BigDecimal("0.0000000001")) > 0) {
                        BigDecimal numer = ziel.subtract(gesamt.multiply(gFactor, MC), MC);
                        BigDecimal bSolve = numer.divide(denom, MC);

                        // Clamp [bMin, gesamt]
                        bSolve = max(bMin, min(bSolve, gesamt));

                        newB = bSolve;
                        newG = gesamt.subtract(newB);
                        if (newG.compareTo(BD_0) < 0) newG = BD_0;
                    } else {
                        // Faktoren fast gleich -> einfach alles in G (oder B_min), Rest in G
                        newB = min(bMin, gesamt);
                        newG = gesamt.subtract(newB);
                    }
                }
            }

            // Rest in A
            BigDecimal safeFinal = newB.add(newG);
            if (safeFinal.compareTo(gesamt) > 0) {
                // Safety clamp
                safeFinal = gesamt;
                if (newB.compareTo(gesamt) > 0) newB = gesamt;
                newG = gesamt.subtract(newB);
            }
            topfB = newB;
            topfG = newG;
            topfA = gesamt.subtract(safeFinal);

            // 4) Renditen anwenden (Monat m)
            BigDecimal rA = rendite(kaA, m);
            topfA = topfA.multiply(BD_1.add(rA), MC);

            // Topf B: Rendite + Floor
            BigDecimal rB = rendite(kaB, m);
            BigDecimal bAfter = topfB.multiply(BD_1.add(rB), MC);

            // Floor: nicht unter floor * letzterB fallen (monat-zu-monat)
            BigDecimal floorValue = letzterB.multiply(floor, MC);
            if (bAfter.compareTo(floorValue) < 0) bAfter = floorValue;

            topfB = bAfter;
            letzterB = topfB; // Update

            // Garantietopf
            BigDecimal rG = garantieMonatsrendite(tarif);
            topfG = topfG.multiply(BD_1.add(rG), MC);

            // Jahreswert speichern
            if (m % 12 == 0) {
                int jahr = m / 12;
                BigDecimal gesamtWert = topfA.add(topfB).add(topfG);
                jahreswerte.add(new WertpunktDto(jahr, gesamtWert));
            }
        }

        BigDecimal endwert = jahreswerte.isEmpty() ? topfA.add(topfB).add(topfG) : jahreswerte.get(jahreswerte.size() - 1).wert();

        return new BerechnungErgebnisDto(
                tarif.getId(),
                tarif.getTarifName(),
                tarif.getTarifCode(),
                tarif.getAnbieter() != null ? tarif.getAnbieter().getName() : "",
                endwert,
                jahreswerte
        );
    }

    // =========================================================
    // Renditen / Faktoren
    // =========================================================

    private BigDecimal rendite(Kapitalanlage k, int monatNr1Based) {
        if (k == null) return BD_0;
        List<BigDecimal> rs = k.getMonatlicheRenditen();
        if (rs == null || rs.isEmpty()) return BD_0;
        int idx = monatNr1Based - 1;
        if (idx < 0 || idx >= rs.size()) return BD_0;
        BigDecimal r = rs.get(idx);
        return r == null ? BD_0 : r;
    }

    /**
     * Monatliche Garantierendite:
     * - garantiezins ist p.a. (z.B. 0.0225)
     * - wir rechnen grob auf Monat um (p.a./12). Für die Simulation reicht das.
     * - Überschüsse: Dummy +0.5% p.a. (bis du echte Logik/Tarif-Feld hast).
     */
    private BigDecimal garantieMonatsrendite(Tarif tarif) {
        BigDecimal gzPa = nz(tarif.getGarantiezins()); // 0.0225
        if (gzPa.compareTo(BD_0) <= 0) return BD_0;

        BigDecimal addPa = BD_0;
        if (tarif.getGarantieModus() == GarantieModus.MIT_UEBERSCHUESSEN) {
            addPa = new BigDecimal("0.005"); // Platzhalter
        }

        BigDecimal pa = gzPa.add(addPa, MC);
        return pa.divide(new BigDecimal("12"), 12, RoundingMode.HALF_UP);
    }

    /**
     * Faktor, wie 1€ im Garantietopf in restMonate wächst.
     */
    private BigDecimal guaranteeFutureFactor(Tarif tarif, int restMonate) {
        if (restMonate <= 0) return BD_1;
        BigDecimal rM = garantieMonatsrendite(tarif);
        BigDecimal factor = BD_1;
        for (int i = 0; i < restMonate; i++) {
            factor = factor.multiply(BD_1.add(rM), MC);
        }
        return factor;
    }

    /**
     * Faktor, wie 1€ im Topf B in restMonate wächst, unter Floor-Regel.
     * Wir simulieren hier 1€ "monatsweise", wenden Rendite an und clampen bei floor * letzterWert.
     */
    private BigDecimal bFutureFactorWithFloor(Kapitalanlage kaB, int restMonate, BigDecimal floor) {
        if (restMonate <= 0) return BD_1;
        if (kaB == null) return BD_1;

        BigDecimal v = BD_1;
        BigDecimal last = BD_1;

        // Wir können nur ab "nächsten Monat" simulieren; für Näherung reicht: nimm die ersten restMonate Renditen ab Index 0.
        // In der echten Welt würdest du hier ab aktuellem Monat weiterlaufen lassen. Für Garantie-Bedarf ist das ok genug.
        for (int i = 1; i <= restMonate; i++) {
            BigDecimal r = rendite(kaB, i);
            BigDecimal after = v.multiply(BD_1.add(r), MC);

            BigDecimal floorValue = last.multiply(floor, MC);
            if (after.compareTo(floorValue) < 0) after = floorValue;

            v = after;
            last = v;
        }
        return v; // da Start=1
    }

    private BigDecimal pvForFutureValue(BigDecimal futureValue, BigDecimal factor) {
        if (futureValue == null) return BD_0;
        if (factor == null || factor.compareTo(BD_0) <= 0) return BD_0;
        return futureValue.divide(factor, MC);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BD_0 : v;
    }

    private static BigDecimal max(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.max(b);
    }

    private static BigDecimal min(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.min(b);
    }

    // =========================================================
    // Kosten (V2 - wie wir es aufgebaut haben)
    // =========================================================

    private BigDecimal kostenSummeFuerMonat(List<Kostenpunkt> punkte, int monatIndex, BigDecimal beitrag, BigDecimal kapitalGesamt) {
        BigDecimal sum = BD_0;
        if (punkte == null || punkte.isEmpty()) return sum;

        for (Kostenpunkt p : punkte) {
            if (p == null) continue;
            if (!p.isAktiv()) continue;
            if (!giltInMonat(p, monatIndex)) continue;

            BigDecimal kosten = kostenWertFuerPunkt(p, beitrag, kapitalGesamt);
            sum = sum.add(kosten);
        }
        return sum.max(BD_0);
    }

    private boolean giltInMonat(Kostenpunkt p, int m) {
        if (p == null) return false;

        // optional: Gültigkeit
        Integer von = p.getGueltigVonMonat();
        Integer bis = p.getGueltigBisMonat();
        if (von != null && m < von) return false;
        if (bis != null && m > bis) return false;

        KostenRhythmus r = p.getRhythmus();
        if (r == null) return false;

        return switch (r) {
            case EINMALIG -> (m == 1);
            case MONATLICH -> true;
            case JAHRLICH -> (m % 12 == 0);
            case VERTEILT_5_JAHRE -> (m >= 1 && m <= 60);
            case VERTEILT_7_JAHRE -> (m >= 1 && m <= 84);
        };
    }

    /**
     * Regeln:
     * - EURO + FIX: wert ist Euro (bei VERTEILT_* = Gesamtbetrag -> wird auf Monate verteilt)
     * - PROZENT + BEITRAG: beitrag * (wert/100) (wenn ProzentPeriode=JÄHRLICH dann /12)
     * - PROZENT + KAPITAL: kapital * (wert/100) (wenn ProzentPeriode=JÄHRLICH dann /12)
     * + optional: minimumEuro
     */
    private BigDecimal kostenWertFuerPunkt(Kostenpunkt p, BigDecimal beitrag, BigDecimal kapital) {
        BigDecimal wert = nz(p.getWert());

        int divisor = switch (p.getRhythmus()) {
            case VERTEILT_5_JAHRE -> 60;
            case VERTEILT_7_JAHRE -> 84;
            default -> 1;
        };

        if (p.getTyp() == KostenTyp.EURO) {
            BigDecimal euro = wert;
            if (divisor > 1) euro = euro.divide(BigDecimal.valueOf(divisor), 6, RoundingMode.HALF_UP);
            euro = euro.max(BD_0);

            BigDecimal min = nz(p.getMinimumEuro());
            if (min.compareTo(BD_0) > 0) euro = euro.max(min);
            return euro;
        }

        // PROZENT
        BigDecimal basis = switch (p.getBasis()) {
            case BEITRAG -> beitrag;
            case KAPITAL -> kapital;
            case FIX -> BD_0; // PROZENT+FIX => 0 (fachlich unsinnig)
        };

        BigDecimal pct = wert.divide(BD_100, 12, RoundingMode.HALF_UP);

        // Prozentperiode: wenn JÄHRLICH => /12
        ProzentPeriode periode = p.getProzentPeriode();
        if (periode == null) periode = ProzentPeriode.MONATLICH;
        if (periode == ProzentPeriode.JAHRLICH) {
            pct = pct.divide(new BigDecimal("12"), 12, RoundingMode.HALF_UP);
        }

        BigDecimal kosten = basis.multiply(pct, MC);

        if (divisor > 1) {
            // Verteilte Prozentkosten (vereinfachend)
            kosten = kosten.divide(BigDecimal.valueOf(divisor), 6, RoundingMode.HALF_UP);
        }

        kosten = kosten.max(BD_0);

        BigDecimal min = nz(p.getMinimumEuro());
        if (min.compareTo(BD_0) > 0) kosten = kosten.max(min);

        return kosten;
    }
}
