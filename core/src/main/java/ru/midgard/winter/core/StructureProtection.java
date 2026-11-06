package ru.midgard.winter.core;
import java.util.Set;
/** Only known, fixed-height underground structures use vertical separation. Unknown/surface stay conservative. */
public final class StructureProtection {
    private static final Set<String> UNDERGROUND=Set.of("mineshaft","mineshaft_mesa","stronghold","ancient_city","trial_chambers");
    private StructureProtection(){}
    public static boolean canUseVerticalBounds(String id){return UNDERGROUND.contains(id.replace("minecraft:",""));}
}
