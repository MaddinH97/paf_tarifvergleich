package de.paf.tarifvergleich.domain;

/**
 * Welche Art "Topf" ein Hybrid-Tarif als zweiten / dritten Topf nutzt.
 *
 * A = Kapitalanlage A (Fonds / Index)
 * B = Kapitalanlage B (bei dir: Garantiefonds)
 * GARANTIE = klassischer Garantiezins-Topf des Versicherers
 */
public enum TopfTyp {
    A,
    B,
    GARANTIE
}