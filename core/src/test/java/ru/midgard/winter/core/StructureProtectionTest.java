package ru.midgard.winter.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StructureProtectionTest {
    @Test void deepMineshaftProtectsItsVolumeWithoutBlockingTheEntireForest(){
        assertTrue(StructureProtection.canUseVerticalBounds("minecraft:mineshaft"));
        Box mine=new Box(0,-40,0,30,-20,30);assertFalse(mine.intersects(new Box(8,64,8,16,87,16),8));assertTrue(mine.intersects(new Box(8,-30,8,16,-10,16),8));
    }
    @Test void staleShipwreckYAndUnknownStructuresRemainFullHeight(){for(String id:new String[]{"shipwreck","minecraft:ocean_ruin_cold","village_plains","mansion","ruined_portal","custom:buried_ruin"})assertFalse(StructureProtection.canUseVerticalBounds(id));}
}
