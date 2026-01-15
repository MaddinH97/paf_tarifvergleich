package de.paf.tarifvergleich.service.kapitalanlage;

import de.paf.tarifvergleich.domain.Kapitalanlage;
import de.paf.tarifvergleich.domain.KapitalanlageTyp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class KapitalanlageFactory {

    private KapitalanlageFactory() {}

    /**
     * Baut eine Kapitalanlage aus monatlichen Renditen.
     *
     * @param name Anzeigename (z.B. "FantasyFonds")
     * @param monthlyReturns Dezimalrenditen je Monat (z.B. 0.01 = +1%)
     */
    public static Kapitalanlage createFromMonthlyReturns(String name, List<BigDecimal> monthlyReturns) {
        List<BigDecimal> renditen = (monthlyReturns == null) ? List.of() : monthlyReturns;

        return Kapitalanlage.builder()
                .name(name)
                .typ(KapitalanlageTyp.FANTASY_FONDS)
                .annualRate(null) // bei echten Monatsreihen nicht nötig
                .monatlicheRenditen(new ArrayList<>(renditen))
                .aktiv(true)
                .build();
    }

    /**
     * Helper: nimmt Werte als String-Liste im DE-Format (Komma) und parsed zu BigDecimal.
     * Beispiel: "0,01039413" -> 0.01039413
     */
    public static List<BigDecimal> parseGermanDecimalLines(String rawLines) {
        if (rawLines == null || rawLines.isBlank()) return List.of();

        String[] lines = rawLines.split("\\R+");
        List<BigDecimal> out = new ArrayList<>(lines.length);

        for (String line : lines) {
            String s = line.trim();
            if (s.isEmpty()) continue;

            // deutsches Dezimal-Komma -> Punkt
            s = s.replace(",", ".");

            out.add(new BigDecimal(s));
        }
        return out;
    }
}