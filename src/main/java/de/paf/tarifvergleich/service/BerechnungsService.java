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
    private static final BigDecimal EPS = new BigDecimal("0.0000000001");

    /**
     * Controller-Signatur:
     * beitragMonat, laufzeitJahre, einstiegsalter, kapitalanlageAId, kapitalanlageBId, garantieModus, tarifIds
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

        Kapitalanlage kaA = kapitalanlageAId != null
                ? kapitalanlageRepository.findById(kapitalanlageAId).orElse(null)
                : null;

        Kapitalanlage kaB = kapitalanlageBId != null
                ? kapitalanlageRepository.findById(kapitalanlageBId).orElse(null)
                : null;

        BigDecimal beitrag = BigDecimal.valueOf(beitragMonat);

        List<BerechnungErgebnisDto> out = new ArrayList<>();

        for (Long tarifId : tarifIds) {
            Tarif tarif = tarifRepository.findById(tarifId).orElse(null);
            if (tarif == null) continue;

            // Lombok bei boolean => isAktiv()
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
    // 1) FONDS (nur Topf 1)
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

        List<WertpunktDto> jahreswerte = new ArrayList<>();

        for (int m = 1; m <= gesamtMonate; m++) {

            // Beitrag rein
            topfA = topfA.add(beitrag);
            sumBeitraege = sumBeitraege.add(beitrag);

            // Kosten runter
            BigDecimal kosten = kostenSummeFuerMonat(kostenpunkte, m, beitrag, topfA);
            if (kosten.compareTo(BD_0) > 0) {
                topfA = topfA.subtract(kosten);
                if (topfA.compareTo(BD_0) < 0) topfA = BD_0;
            }

            // Rendite Topf A
            BigDecimal rA = rendite(kaA, m);
            topfA = topfA.multiply(BD_1.add(rA), MC);

            // Jahreswerte
            if (m % 12 == 0) {
                int jahr = m / 12;

                BigDecimal topf1 = topfA;
                BigDecimal topf2 = BD_0;
                BigDecimal topf3 = BD_0;
                BigDecimal gesamt = topf1.add(topf2).add(topf3);

                jahreswerte.add(new WertpunktDto(
                        jahr,
                        sumBeitraege,
                        gesamt,
                        topf1,
                        topf2,
                        topf3
                ));
            }
        }

        BigDecimal endwert = jahreswerte.isEmpty()
                ? topfA
                : jahreswerte.get(jahreswerte.size() - 1).gesamtKapital();

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
    // - Topf1 = Fonds (A)
    // - Topf3 = Garantie (Deckungsstock)
    // - Topf2 existiert hier nicht => immer 0
    //
    // Logik:
    // Beitrag - Kosten => aktuelles Kapital
    // Dann Allokation so, dass Garantie am Ende >= garantieNiveau * SummeEinzahlungen
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

        List<WertpunktDto> jahreswerte = new ArrayList<>();

        for (int m = 1; m <= gesamtMonate; m++) {

            // 1) Beitrag in Gesamt
            sumBeitraege = sumBeitraege.add(beitrag);
            BigDecimal gesamt = topfA.add(topfG).add(beitrag);

            // 2) Kosten runter
            BigDecimal kosten = kostenSummeFuerMonat(kostenpunkte, m, beitrag, gesamt);
            if (kosten.compareTo(BD_0) > 0) {
                gesamt = gesamt.subtract(kosten);
                if (gesamt.compareTo(BD_0) < 0) gesamt = BD_0;
            }

            // 3) Garantiebedarf (PV)
            int restMonate = gesamtMonate - m;

            BigDecimal garantieNiveau = nz(tarif.getGarantieNiveau());
            if (garantieNiveau.compareTo(BD_0) < 0) garantieNiveau = BD_0;
            if (garantieNiveau.compareTo(BD_1) > 0) garantieNiveau = BD_1;

            BigDecimal ziel = sumBeitraege.multiply(garantieNiveau, MC);

            BigDecimal gFactor = guaranteeFutureFactor(tarif, restMonate);
            BigDecimal neededGNow = pvForFutureValue(ziel, gFactor);

            if (neededGNow.compareTo(gesamt) > 0) neededGNow = gesamt;
            if (neededGNow.compareTo(BD_0) < 0) neededGNow = BD_0;

            topfG = neededGNow;
            topfA = gesamt.subtract(topfG);

            // 4) Renditen
            BigDecimal rA = rendite(kaA, m);
            topfA = topfA.multiply(BD_1.add(rA), MC);

            BigDecimal rG = garantieMonatsrendite(tarif);
            topfG = topfG.multiply(BD_1.add(rG), MC);

            // Jahreswerte
            if (m % 12 == 0) {
                int jahr = m / 12;

                BigDecimal topf1 = topfA;
                BigDecimal topf2 = BD_0;     // kein Topf B im 2-Topf
                BigDecimal topf3 = topfG;

                BigDecimal gesamtKapital = topf1.add(topf2).add(topf3);

                jahreswerte.add(new WertpunktDto(
                        jahr,
                        sumBeitraege,
                        gesamtKapital,
                        topf1,
                        topf2,
                        topf3
                ));
            }
        }

        BigDecimal endwert = jahreswerte.isEmpty()
                ? topfA.add(topfG)
                : jahreswerte.get(jahreswerte.size() - 1).gesamtKapital();

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
    // - Topf1 = Fonds (A)
    // - Topf2 = Garantiefonds (B) mit Floor
    // - Topf3 = Deckungsstock/klassische Garantie (G)
    //
    // Floor: TopfB soll nicht unter floor * letzterB fallen (monat-zu-monat)
    // Garantie-Ziel: garantieNiveau * SummeEinzahlungen am Ende
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

        BigDecimal letzterB = BD_0;

        BigDecimal sumBeitraege = BD_0;

        BigDecimal floor = nz(tarif.getTopfBFloor());
        if (floor.compareTo(BD_0) < 0) floor = BD_0;
        if (floor.compareTo(BD_1) > 0) floor = BD_1;

        BigDecimal garantieNiveau = nz(tarif.getGarantieNiveau());
        if (garantieNiveau.compareTo(BD_0) < 0) garantieNiveau = BD_0;
        if (garantieNiveau.compareTo(BD_1) > 0) garantieNiveau = BD_1;

        List<WertpunktDto> jahreswerte = new ArrayList<>();

        for (int m = 1; m <= gesamtMonate; m++) {

            // 1) Beitrag in Gesamt
            sumBeitraege = sumBeitraege.add(beitrag);
            BigDecimal gesamt = topfA.add(topfB).add(topfG).add(beitrag);

            // 2) Kosten runter
            BigDecimal kosten = kostenSummeFuerMonat(kostenpunkte, m, beitrag, gesamt);
            if (kosten.compareTo(BD_0) > 0) {
                gesamt = gesamt.subtract(kosten);
                if (gesamt.compareTo(BD_0) < 0) gesamt = BD_0;
            }

            // 3) Ziel am Ende
            int restMonate = gesamtMonate - m;
            BigDecimal ziel = sumBeitraege.multiply(garantieNiveau, MC);

            // Faktoren
            BigDecimal gFactor = guaranteeFutureFactor(tarif, restMonate);

            // B-Faktor konservativ ab nächstem Monat (damit es zur Stelle "m" passt)
            BigDecimal bFactor = bFutureFactorWithFloor(kaB, m + 1, restMonate, floor);

            // Mindest-B wegen Floor-Regel
            BigDecimal bMin = letzterB.multiply(floor, MC);

            // Strategie: möglichst viel in A lassen, sichere Seite durch (B und ggf. G)
            BigDecimal bNeededAllB = pvForFutureValue(ziel, bFactor);
            BigDecimal bStart = max(bMin, bNeededAllB);

            BigDecimal newB;
            BigDecimal newG;

            if (bStart.compareTo(gesamt) <= 0) {
                // komplett über B machbar
                newB = bStart;
                newG = BD_0;
            } else {
                // nicht genug: setze B minimal, Rest G
                newB = min(bMin, gesamt);

                BigDecimal restZiel = ziel.subtract(newB.multiply(bFactor, MC), MC);
                if (restZiel.compareTo(BD_0) <= 0) {
                    newG = BD_0;
                } else {
                    newG = pvForFutureValue(restZiel, gFactor);
                }

                BigDecimal safe = newB.add(newG);
                if (safe.compareTo(gesamt) > 0) {
                    // Mix so, dass B+G=gesamt und Ziel erfüllt wird (falls möglich)
                    BigDecimal denom = bFactor.subtract(gFactor, MC);

                    if (denom.abs().compareTo(EPS) > 0) {
                        BigDecimal numer = ziel.subtract(gesamt.multiply(gFactor, MC), MC);
                        BigDecimal bSolve = numer.divide(denom, MC);

                        bSolve = max(bMin, min(bSolve, gesamt));
                        newB = bSolve;
                        newG = gesamt.subtract(newB);
                        if (newG.compareTo(BD_0) < 0) newG = BD_0;
                    } else {
                        // Faktoren fast gleich => B=min, Rest G
                        newB = min(bMin, gesamt);
                        newG = gesamt.subtract(newB);
                        if (newG.compareTo(BD_0) < 0) newG = BD_0;
                    }
                }
            }

            BigDecimal safeFinal = newB.add(newG);
            if (safeFinal.compareTo(gesamt) > 0) {
                safeFinal = gesamt;
                if (newB.compareTo(gesamt) > 0) newB = gesamt;
                newG = gesamt.subtract(newB);
                if (newG.compareTo(BD_0) < 0) newG = BD_0;
            }

            topfB = newB;
            topfG = newG;
            topfA = gesamt.subtract(safeFinal);

            // 4) Renditen anwenden

            // A
            BigDecimal rA = rendite(kaA, m);
            topfA = topfA.multiply(BD_1.add(rA), MC);

            // B + Floor (monat-zu-monat)
            BigDecimal rB = rendite(kaB, m);
            BigDecimal bAfter = topfB.multiply(BD_1.add(rB), MC);

            BigDecimal floorValue = letzterB.multiply(floor, MC);
            if (bAfter.compareTo(floorValue) < 0) bAfter = floorValue;

            topfB = bAfter;
            letzterB = topfB;

            // G
            BigDecimal rG = garantieMonatsrendite(tarif);
            topfG = topfG.multiply(BD_1.add(rG), MC);

            // Jahreswerte
            if (m % 12 == 0) {
                int jahr = m / 12;

                BigDecimal topf1 = topfA;
                BigDecimal topf2 = topfB;
                BigDecimal topf3 = topfG;
                BigDecimal gesamtKapital = topf1.add(topf2).add(topf3);

                jahreswerte.add(new WertpunktDto(
                        jahr,
                        sumBeitraege,
                        gesamtKapital,
                        topf1,
                        topf2,
                        topf3
                ));
            }
        }

        BigDecimal endwert = jahreswerte.isEmpty()
                ? topfA.add(topfB).add(topfG)
                : jahreswerte.get(jahreswerte.size() - 1).gesamtKapital();

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
     * - wir rechnen grob p.a./12
     * - Überschüsse: Dummy +0.5% p.a. (Platzhalter)
     */
    private BigDecimal garantieMonatsrendite(Tarif tarif) {
        BigDecimal gzPa = nz(tarif.getGarantiezins());
        if (gzPa.compareTo(BD_0) <= 0) return BD_0;

        BigDecimal addPa = BD_0;
        if (tarif.getGarantieModus() == GarantieModus.MIT_UEBERSCHUESSEN) {
            addPa = new BigDecimal("0.005"); // Platzhalter
        }

        BigDecimal pa = gzPa.add(addPa, MC);
        return pa.divide(new BigDecimal("12"), 12, RoundingMode.HALF_UP);
    }

    /**
     * Faktor: wie 1€ im Garantietopf in restMonate wächst.
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
     * Faktor: wie 1€ in Topf B in restMonate wächst (mit Floor-Regel),
     * simuliert ab startMonth (1-based) für restMonate Monate.
     */
    private BigDecimal bFutureFactorWithFloor(Kapitalanlage kaB, int startMonth, int restMonate, BigDecimal floor) {
        if (restMonate <= 0) return BD_1;
        if (kaB == null) return BD_1;

        BigDecimal v = BD_1;
        BigDecimal last = BD_1;

        for (int i = 0; i < restMonate; i++) {
            int monthIndex = startMonth + i;
            BigDecimal r = rendite(kaB, monthIndex);

            BigDecimal after = v.multiply(BD_1.add(r), MC);

            BigDecimal floorValue = last.multiply(floor, MC);
            if (after.compareTo(floorValue) < 0) after = floorValue;

            v = after;
            last = v;
        }

        return v; // Start=1
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
    // Kosten (V2)
    // =========================================================

    private BigDecimal kostenSummeFuerMonat(List<Kostenpunkt> punkte, int monatIndex, BigDecimal beitrag, BigDecimal kapitalGesamt) {
        BigDecimal sum = BD_0;
        if (punkte == null || punkte.isEmpty()) return sum;

        for (Kostenpunkt p : punkte) {
            if (p == null) continue;

            // Lombok boolean => isAktiv()
            if (!p.isAktiv()) continue;

            if (!giltInMonat(p, monatIndex)) continue;

            BigDecimal kosten = kostenWertFuerPunkt(p, beitrag, kapitalGesamt);
            sum = sum.add(kosten);
        }
        return sum.max(BD_0);
    }

    private boolean giltInMonat(Kostenpunkt p, int m) {
        if (p == null) return false;

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
            case FIX -> BD_0;
        };

        BigDecimal pct = wert.divide(BD_100, 12, RoundingMode.HALF_UP);

        ProzentPeriode periode = p.getProzentPeriode();
        if (periode == null) periode = ProzentPeriode.MONATLICH;
        if (periode == ProzentPeriode.JAHRLICH) {
            pct = pct.divide(new BigDecimal("12"), 12, RoundingMode.HALF_UP);
        }

        BigDecimal kosten = basis.multiply(pct, MC);

        if (divisor > 1) {
            kosten = kosten.divide(BigDecimal.valueOf(divisor), 6, RoundingMode.HALF_UP);
        }

        kosten = kosten.max(BD_0);

        BigDecimal min = nz(p.getMinimumEuro());
        if (min.compareTo(BD_0) > 0) kosten = kosten.max(min);

        return kosten;
    }
}