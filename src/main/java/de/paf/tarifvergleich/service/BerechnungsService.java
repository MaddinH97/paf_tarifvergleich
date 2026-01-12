package de.paf.tarifvergleich.service;

import de.paf.tarifvergleich.controller.dto.BerechnungErgebnisDto;
import de.paf.tarifvergleich.controller.dto.WertpunktDto;
import de.paf.tarifvergleich.domain.Kostenstruktur;
import de.paf.tarifvergleich.domain.Tarif;
import de.paf.tarifvergleich.repository.KostenstrukturRepository;
import de.paf.tarifvergleich.repository.TarifRepository;
import lombok.RequiredArgsConstructor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BerechnungsService {

    private final TarifRepository tarifRepository;
    private final KostenstrukturRepository kostenstrukturRepository;

    public List<BerechnungErgebnisDto> berechne(
            Integer beitragMonat,
            Integer laufzeitJahre,
            Integer einstiegsalter,
            List<Long> tarifIds
    ) {
        if (beitragMonat == null || laufzeitJahre == null || einstiegsalter == null) {
            return List.of();
        }
        if (tarifIds == null || tarifIds.isEmpty()) {
            return List.of();
        }

        List<Tarif> tarife = tarifRepository.findAllById(tarifIds);

        List<BerechnungErgebnisDto> out = new ArrayList<>();

        for (Tarif tarif : tarife) {
            // Tarif muss aktiv sein
            if (tarif.getAktiv() == null || !tarif.getAktiv()) continue;

            // passende Kostenstruktur muss existieren & aktiv sein
            Optional<Kostenstruktur> ksOpt =
                    kostenstrukturRepository.findFirstByTarif_IdAndBeitragMonatAndLaufzeitJahreAndAktivTrue(
                            tarif.getId(), beitragMonat, laufzeitJahre
                    );
            if (ksOpt.isEmpty()) continue;

            Kostenstruktur ks = ksOpt.get();

            BerechnungErgebnisDto ergebnis =
                    executeScriptAndBuildResult(tarif, ks, beitragMonat, laufzeitJahre, einstiegsalter);

            out.add(ergebnis);
        }

        return out;
    }

    private BerechnungErgebnisDto executeScriptAndBuildResult(
            Tarif tarif,
            Kostenstruktur ks,
            Integer beitragMonat,
            Integer laufzeitJahre,
            Integer einstiegsalter
    ) {
        String script = tarif.getBerechnungsScript();
        if (script == null || script.isBlank()) {
            script = "return beitrag * laufzeit;"; // Fallback
        }

        BigDecimal beitrag = new BigDecimal(beitragMonat);
        BigDecimal laufzeit = new BigDecimal(laufzeitJahre);

        // Script ausführen -> Ergebnis als Zahl (Number)
        BigDecimal scriptResult = runJsNumber(script, beitrag, laufzeit, new BigDecimal(einstiegsalter), ks);

        // Interpretation:
        // - wenn Ergebnis <= 10 => wir behandeln es als Faktor (0.95 / 1.05 etc.)
        // - sonst behandeln wir es als Endwert
        BigDecimal endwert;
        BigDecimal faktor;
        if (scriptResult.compareTo(new BigDecimal("10")) <= 0) {
            faktor = scriptResult;
            endwert = beitrag
                    .multiply(new BigDecimal("12"))
                    .multiply(laufzeit)
                    .multiply(faktor)
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            endwert = scriptResult.setScale(2, RoundingMode.HALF_UP);
            // Faktor nur „abgeleitet“ für Dummy-Kurve
            BigDecimal basis = beitrag.multiply(new BigDecimal("12")).multiply(laufzeit);
            faktor = basis.signum() == 0 ? BigDecimal.ONE : endwert.divide(basis, 8, RoundingMode.HALF_UP);
        }

        // Dummy-Wertentwicklung: linear von 0 bis Endwert in Jahres-Schritten
        List<WertpunktDto> curve = new ArrayList<>();
        curve.add(new WertpunktDto(0, BigDecimal.ZERO));
        for (int j = 1; j <= laufzeitJahre; j++) {
            BigDecimal v = endwert
                    .multiply(new BigDecimal(j))
                    .divide(new BigDecimal(laufzeitJahre), 2, RoundingMode.HALF_UP);
            curve.add(new WertpunktDto(j, v));
        }

        String anbieterName = (tarif.getAnbieter() != null) ? tarif.getAnbieter().getName() : "";

        return new BerechnungErgebnisDto(
                tarif.getId(),
                tarif.getTarifName(),
                tarif.getTarifCode(),
                anbieterName,
                endwert,
                curve
        );
    }

    private BigDecimal runJsNumber(String script, BigDecimal beitrag, BigDecimal laufzeit, BigDecimal einstiegsalter, Kostenstruktur ks) {
        // Wir evaluieren ein "return ..." Script.
        // Damit "return" klappt, wrappen wir in eine Funktion und rufen sie auf.
        String wrapped = """
                (function() {
                  %s
                })()
                """.formatted(script);

        try (Context ctx = Context.newBuilder("js")
                .allowAllAccess(false) // wichtig: kein Zugriff auf Java Klassen etc.
                .build()) {

            // Variablen in JS "global" bereitstellen
            Value bindings = ctx.getBindings("js");
            bindings.putMember("beitrag", beitrag.doubleValue());
            bindings.putMember("laufzeit", laufzeit.doubleValue());
            bindings.putMember("einstiegsalter", einstiegsalter.intValue());

            // ks als Map bereitstellen (nur primitive Werte)
            Map<String, Object> ksMap = new HashMap<>();
            ksMap.put("abschlusskosten", bdOrZero(ks.getAbschlusskosten()));
            ksMap.put("verwaltungskosten", bdOrZero(ks.getVerwaltungskosten()));
            ksMap.put("fondskosten", bdOrZero(ks.getFondskosten()));
            ksMap.put("risikokosten", bdOrZero(ks.getRisikokosten()));
            ksMap.put("sonstigeKosten", bdOrZero(ks.getSonstigeKosten()));

            bindings.putMember("ks", ksMap);

            Value result = ctx.eval("js", wrapped);

            if (result == null || result.isNull()) return BigDecimal.ZERO;
            if (!result.isNumber()) return BigDecimal.ZERO;

            return new BigDecimal(result.asDouble()).setScale(8, RoundingMode.HALF_UP);
        } catch (Exception e) {
            // bei Script-Fehlern lieber 0 statt App-Crash
            return BigDecimal.ZERO;
        }
    }

    private double bdOrZero(BigDecimal bd) {
        return bd == null ? 0.0 : bd.doubleValue();
    }
}