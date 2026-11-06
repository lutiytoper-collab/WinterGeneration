package ru.midgard.winter.core;
import java.util.*;
/** Strict allowlist; unknown IDs and non-overworld dimensions are denied. */
public record BiomePolicy(Set<String> allowed, Set<String> excluded) {
    public static final Set<String> DEFAULT = Set.of("plains", "sunflower_plains", "forest", "flower_forest",
        "birch_forest", "old_growth_birch_forest", "dark_forest", "taiga", "old_growth_pine_taiga",
        "old_growth_spruce_taiga", "snowy_taiga", "snowy_plains", "meadow", "grove",
        "snowy_slopes", "frozen_peaks", "jagged_peaks", "stony_peaks", "windswept_hills",
        "windswept_forest", "windswept_gravelly_hills", "river", "frozen_river");
    private static final Set<String> HOT = Set.of("desert", "savanna", "savanna_plateau", "windswept_savanna",
        "jungle", "sparse_jungle", "bamboo_jungle", "badlands", "eroded_badlands", "wooded_badlands",
        "warm_ocean", "lukewarm_ocean", "deep_lukewarm_ocean", "mangrove_swamp");
    public BiomePolicy {
        allowed = Set.copyOf(allowed); excluded = Set.copyOf(excluded);
        if (!DEFAULT.containsAll(allowed)) throw new IllegalArgumentException("Unsupported biome allowlist");
    }
    public boolean permits(String dimension, String id) {
        String b = id.startsWith("minecraft:") ? id.substring(10) : id;
        return dimension.equals("NORMAL") && allowed.contains(b) && !excluded.contains(b) && !HOT.contains(b);
    }
}
