package ru.midgard.winter;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AirDropSnowSupportTest {
    @Test void fullSnowFieldIsSolidSupport(){assertTrue(WinterPlugin.airDropSolidSupport(Material.SNOW_BLOCK));assertTrue(WinterPlugin.airDropSolidSupport(Material.STONE));}
    @Test void canopyAndIceStillFailSolidSupport(){for(var type:new Material[]{Material.OAK_LEAVES,Material.SPRUCE_LOG,Material.ICE,Material.POWDER_SNOW,Material.SNOW,Material.AIR})assertFalse(WinterPlugin.airDropSolidSupport(type),type.toString());}
}
