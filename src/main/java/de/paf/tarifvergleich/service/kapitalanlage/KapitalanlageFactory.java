package de.paf.tarifvergleich.service.kapitalanlage;

import de.paf.tarifvergleich.domain.Kapitalanlage;
import de.paf.tarifvergleich.domain.KapitalanlageTyp;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class KapitalanlageFactory {

    private static final int MONTHS_65Y = 65 * 12;

    private KapitalanlageFactory() {}

    public static Kapitalanlage createFixed(String name, BigDecimal annualRate) {
        BigDecimal monthly = annualToMonthly(annualRate);

        return Kapitalanlage.builder()
                .name(name)
                .typ(KapitalanlageTyp.FIXED)
                .annualRate(annualRate)
                .monatlicheRenditen(fill(monthly, MONTHS_65Y))
                .aktiv(true)
                .build();
    }

    public static Kapitalanlage createFantasyMsciWorldLike() {
        return Kapitalanlage.builder()
                .name("FantasyFonds (MSCI-World-like, 9% p.a.)")
                .typ(KapitalanlageTyp.FANTASY)
                .annualRate(null)
                .monatlicheRenditen(
                        RenditeGenerator.generateMsciWorldLikeMonths(
                                MONTHS_65Y,
                                0.09,   // 9 % p.a.
                                40,     // Kalibrierung
                                42L     // Seed
                        )
                )
                .aktiv(true)
                .build();
    }

    private static BigDecimal annualToMonthly(BigDecimal annual) {
        double r =
                Math.pow(1.0 + annual.doubleValue(), 1.0 / 12.0) - 1.0;
        return new BigDecimal(r, new MathContext(12, RoundingMode.HALF_UP));
    }

    private static List<BigDecimal> fill(BigDecimal v, int months) {
        List<BigDecimal> out = new ArrayList<>(months);
        for (int i = 0; i < months; i++) out.add(v);
        return out;
    }
}