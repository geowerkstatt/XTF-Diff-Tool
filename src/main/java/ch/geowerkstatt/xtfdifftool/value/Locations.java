package ch.geowerkstatt.xtfdifftool.value;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Combines the locations of several geometries into the location of one change.
 */
public final class Locations {
    private Locations() { }

    /**
     * Combines the given locations into a single WKT.
     *
     * @param locations the locations as WKT
     * @return {@code null} without a location, the location itself for one, a {@code GEOMETRYCOLLECTION} for several
     */
    public static String combine(Stream<String> locations) {
        var all = locations.toList();
        return switch (all.size()) {
            case 0 -> null;
            case 1 -> all.getFirst();
            default -> all.stream().collect(Collectors.joining(", ", "GEOMETRYCOLLECTION (", ")"));
        };
    }
}
