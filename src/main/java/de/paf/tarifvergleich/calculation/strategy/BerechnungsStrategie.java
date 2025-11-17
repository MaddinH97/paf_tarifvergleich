package de.paf.tarifvergleich.calculation.strategy;

import de.paf.tarifvergleich.domain.BerechnungsParameter;

public interface BerechnungsStrategie {

    // später: Liste von Jahreswerten zurückgeben
    void berechne(BerechnungsParameter parameter);
}