package ru.midgard.winter.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BiomeZoomerTest {
 @Test void deterministicAndLocal(){long seed=BiomeZoomer.seed(42);for(int x=-100;x<100;x++){var q=BiomeZoomer.sample(seed,x,63,-x);assertEquals(q,BiomeZoomer.sample(seed,x,63,-x));assertTrue(Math.abs((q.x()<<2)-x)<=6);assertTrue(Math.abs((q.z()<<2)+x)<=6);}}
 @Test void preserveOriginalColdClimate(){assertFalse(BiomeZoomer.certainlyWarm(-.5,62,63));assertFalse(BiomeZoomer.certainlyWarm(0,62,63));assertTrue(BiomeZoomer.certainlyWarm(.5,62,63));}
 @Test void noGuessAtHighAltitude(){assertFalse(BiomeZoomer.certainlyWarm(.25,200,63));assertTrue(BiomeZoomer.certainlyWarm(.7,300,63));}
}
