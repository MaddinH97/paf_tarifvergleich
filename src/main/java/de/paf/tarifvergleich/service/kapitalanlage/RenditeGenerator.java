package de.paf.tarifvergleich.service.kapitalanlage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class RenditeGenerator {

    private RenditeGenerator() {}

    public static List<BigDecimal> generateMsciWorldLikeMonths(
            int months,
            double targetAnnualCagr,
            int calibrationYears,
            long seed
    ) {
        List<Double> raw = new ArrayList<>(months);
        Random rnd = new Random(seed);

        double monthlyTrend =
                Math.pow(1.0 + targetAnnualCagr, 1.0 / 12.0) - 1.0;

        double sigma = 0.025; // Volatilität

        for (int i = 0; i < months; i++) {
            double r = monthlyTrend + rnd.nextGaussian() * sigma;

            if (rnd.nextDouble() < 0.012) { // Crash
                r -= (0.08 + rnd.nextDouble() * 0.18);
            }

            if (r <= -0.95) r = -0.95;
            raw.add(r);
        }

        // Kalibrierung
        int calibMonths = Math.min(months, calibrationYears * 12);
        double product = 1.0;
        for (int i = 0; i < calibMonths; i++) {
            product *= (1.0 + raw.get(i));
        }

        double targetProduct =
                Math.pow(1.0 + targetAnnualCagr, calibMonths / 12.0);

        double k = Math.pow(targetProduct / product, 1.0 / calibMonths);

        List<BigDecimal> out = new ArrayList<>(months);
        for (int i = 0; i < months; i++) {
            double rCal = (1.0 + raw.get(i)) * k - 1.0;
            out.add(BigDecimal.valueOf(rCal).setScale(6, RoundingMode.HALF_UP));
        }

        return out;
    }
}