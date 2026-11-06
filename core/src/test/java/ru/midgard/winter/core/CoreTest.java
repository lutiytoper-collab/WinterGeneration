package ru.midgard.winter.core;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;
class CoreTest {
    BiomePolicy policy=new BiomePolicy(BiomePolicy.DEFAULT,Set.of());
    @ParameterizedTest @ValueSource(strings={"plains","forest","birch_forest","taiga","meadow","river"})
    void allowsTemperate(String id) { assertTrue(policy.permits("NORMAL","minecraft:"+id)); }
    @ParameterizedTest @ValueSource(strings={"desert","savanna","jungle","badlands","warm_ocean","lukewarm_ocean","mangrove_swamp","lush_caves","deep_dark","unknown"})
    void deniesOtherBiomes(String id) { assertFalse(policy.permits("NORMAL",id)); }
    @Test void dimensionDenied() { assertFalse(policy.permits("NETHER","plains")); assertFalse(policy.permits("THE_END","forest")); }
    @Test void emptyAllowlistDenies() { assertFalse(new BiomePolicy(Set.of(),Set.of()).permits("NORMAL","plains")); }
    @Test void excludedWins() { assertFalse(new BiomePolicy(Set.of("plains"),Set.of("plains")).permits("NORMAL","plains")); }
    @Test void hotCannotBeEnabled() { assertThrows(IllegalArgumentException.class,()->new BiomePolicy(Set.of("desert"),Set.of())); }
    @Test void deterministicAcrossThreads() {
        long[] sequential=IntStream.range(-1000,1000).mapToLong(x->Noise.hash(42,x,-x,7)).toArray();
        long[] parallel=IntStream.range(-1000,1000).parallel().mapToLong(x->Noise.hash(42,x,-x,7)).toArray();
        assertArrayEquals(sequential,parallel);
    }
    @Test void seedAndSaltMatter() { assertNotEquals(Noise.hash(1,2,3,4),Noise.hash(2,2,3,4)); assertNotEquals(Noise.hash(1,2,3,4),Noise.hash(1,2,3,5)); }
    @Test void noiseRangeAndNegativeContinuity() {
        for(int x=-300;x<300;x++) { double n=Noise.smooth(1,x,11,8); assertTrue(n>=0&&n<1); assertTrue(Math.abs(n-Noise.smooth(1,x+1,11,8))<.2); }
    }
    @ParameterizedTest @ValueSource(strings={"LIGHT","BALANCED","FAIRYTALE"})
    void validPresets(String name) { assertTrue(Settings.preset(name).maxChanges()<=512); }
    @Test void invalidConfigRejected() {
        assertThrows(IllegalArgumentException.class,()->Settings.preset("FAKE"));
        assertThrows(IllegalArgumentException.class,()->new Settings(Double.NaN,1,0,0,0,0,0,1,0));
        assertThrows(IllegalArgumentException.class,()->new Settings(2,1,0,0,0,0,0,1,0));
        assertThrows(IllegalArgumentException.class,()->new Settings(0,8,0,0,0,0,0,1,0));
        assertThrows(IllegalArgumentException.class,()->new Settings(0,1,0,0,0,0,0,513,0));
    }
    @Test void boxesAndMargins() {
        Box a=new Box(0,0,0,4,4,4),b=new Box(6,0,0,8,4,4);
        assertFalse(a.intersects(b,1)); assertTrue(a.intersects(b,2)); assertTrue(a.intersects(a,0));
        assertFalse(a.intersects(new Box(0,10,0,4,12,4),0));
    }
    @Test void boundariesAndNegativeCoordinates() {
        assertTrue(new Box(-16,0,-16,-1,5,-1).inChunk(-1,-1));
        assertFalse(new Box(-1,0,0,1,5,1).inChunk(0,0));
        assertThrows(IllegalArgumentException.class,()->new Box(2,0,0,1,0,0));
    }
    static class Flat implements AirDropCompatibilityManager.Surface {
        public boolean loaded(int x,int z) { return true; }
        public int height(int x,int z) { return 64; }
        public boolean safeGround(int x,int y,int z) { return y<=64; }
        public boolean air(int x,int y,int z) { return y>64; }
        public boolean protectedAt(int x,int y,int z) { return false; }
    }
    AirDropCompatibilityManager manager=new AirDropCompatibilityManager();
    AirDropCompatibilityManager.Result check(Flat f) { return manager.validate(f,0,0,2,5,3,1); }
    @Test void safePlatform() { assertTrue(check(new Flat()).safe()); assertEquals(65,check(new Flat()).y()); }
    @Test void unloadedNeverRead() {
        var f=new Flat() { public boolean loaded(int x,int z){return false;} public int height(int x,int z){throw new AssertionError("unloaded read");} };
        assertEquals("unloaded",check(f).reason());
    }
    @Test void unsafeWaterIcePowderSnow() { assertEquals("unsafe_support",check(new Flat(){public boolean safeGround(int x,int y,int z){return false;}}).reason()); }
    @Test void caveUnderPlatform() { assertFalse(check(new Flat(){public boolean safeGround(int x,int y,int z){return y!=62;}}).safe()); }
    @Test void protectedRegionDenied() { assertEquals("protected",check(new Flat(){public boolean protectedAt(int x,int y,int z){return true;}}).reason()); }
    @Test void treeCabinObstructionDenied() { assertEquals("obstructed",check(new Flat(){public boolean air(int x,int y,int z){return y>64 && !(x==1&&z==1&&y==67);}}).reason()); }
    @Test void slopeDenied() {
        Flat f=new Flat(){public int height(int x,int z){return 64+(x>0?3:0);} public boolean safeGround(int x,int y,int z){return y<=height(x,z);} public boolean air(int x,int y,int z){return y>height(x,z);}};
        assertEquals("slope",check(f).reason());
    }
    @Test void shallowCentralDipCannotLeaveFloatingChest() {
        Flat f=new Flat(){public int height(int x,int z){return x==0&&z==0?64:65;} public boolean safeGround(int x,int y,int z){return y<=height(x,z);} public boolean air(int x,int y,int z){return y>height(x,z);}};
        assertEquals("center_below_platform",check(f).reason());
        Flat crest=new Flat(){public int height(int x,int z){return x==0&&z==0?65:64;} public boolean safeGround(int x,int y,int z){return y<=height(x,z);} public boolean air(int x,int y,int z){return y>height(x,z);}};
        assertTrue(check(crest).safe());assertEquals(66,check(crest).y());
    }
    @Test void reservationAndRelease() {
        UUID w=UUID.randomUUID(), token=manager.reserve(w,0,0,16); Box b=new Box(15,70,15,20,80,20);
        assertTrue(manager.reserved(w,b)); assertFalse(manager.reserved(UUID.randomUUID(),b));
        manager.release(token); assertFalse(manager.reserved(w,b)); manager.release(token);
    }
    @Test void invalidAirDropLimits() { assertThrows(IllegalArgumentException.class,()->manager.validate(new Flat(),0,0,99,5,3,1)); }
}
