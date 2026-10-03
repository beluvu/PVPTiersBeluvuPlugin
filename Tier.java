package dev.beluvu.btiers;

import java.util.Locale;

/** Lowest (LT5) to highest (HT1). Points are used for /tiertop. */
enum Tier {
    LT5(1), HT5(2), LT4(3), HT4(4), LT3(6), HT3(10), LT2(20), HT2(30), LT1(45), HT1(60);

    final int points;

    Tier(int points) {
        this.points = points;
    }

    static Tier parse(String s) {
        if (s == null) return null;
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
