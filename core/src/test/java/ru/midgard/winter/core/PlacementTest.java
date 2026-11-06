package ru.midgard.winter.core;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;
class PlacementTest {
    static Stream<DecorationPlan> plans(){return Stream.of(DecorationPlan.conifer(1,0,0),DecorationPlan.conifer(2,1,0),
        DecorationPlan.shrub(true),DecorationPlan.shrub(false),DecorationPlan.fallenLog(true),DecorationPlan.fallenLog(false),
        DecorationPlan.ice(42,0,0),DecorationPlan.clearing(true),DecorationPlan.clearing(false),DecorationPlan.cabin(true),DecorationPlan.cabin(false));}
    static class Flat implements PlacementValidator.Site {
        public int minHeight(){return -64;} public int maxHeight(){return 320;}
        public boolean loaded(int x,int z){return true;} public int surface(int x,int z){return 64;}
        public boolean naturalGround(int x,int y,int z){return y==64;}
        public boolean naturalSupport(int x,int y,int z){return y<64;}
        public boolean allowedBiome(int x,int y,int z){return true;}
        public boolean replaceable(int x,int y,int z){return y>64;}
        public boolean protectedBox(Box b){return false;}
    }
    PlacementValidator.Result check(DecorationPlan p,Flat f){return PlacementValidator.validate(p,f,8,64,8,0,0,512,11);}
    @ParameterizedTest @MethodSource("plans") void validTemplatesPlace(DecorationPlan p){assertTrue(check(p,new Flat()).allowed());assertTrue(p.cells().size()<=512);}
    @ParameterizedTest @MethodSource("plans") void noDuplicateCellsAndSnowSupported(DecorationPlan p){
        Set<DecorationPlan.Position> positions=new HashSet<>();
        for(var c:p.cells()) assertTrue(positions.add(new DecorationPlan.Position(c.x(),c.y(),c.z())));
        for(var c:p.cells()) if(c.block().equals("SNOW")) assertTrue(positions.contains(new DecorationPlan.Position(c.x(),c.y()-1,c.z())));
    }
    @ParameterizedTest @MethodSource("plans") void neverOverwritePlayerBlocksOrTrees(DecorationPlan p){
        assertEquals("obstructed",check(p,new Flat(){public boolean replaceable(int x,int y,int z){return false;}}).reason());
    }
    @ParameterizedTest @MethodSource("plans") void rejectsExcludedBiomeInsideFootprint(DecorationPlan p){
        assertEquals("biome",check(p,new Flat(){public boolean allowedBiome(int x,int y,int z){return x<p.bounds(8,64,8).maxX();}}).reason());
    }
    @ParameterizedTest @MethodSource("plans") void reservedAirDropProtectsWholeTemplate(DecorationPlan p){
        assertEquals("protected",check(p,new Flat(){public boolean protectedBox(Box b){return true;}}).reason());
    }
    @Test void slopeNoFoundationRewrite(){assertEquals("terrain",check(DecorationPlan.cabin(true),new Flat(){public int surface(int x,int z){return x==6?65:64;}}).reason());}
    @Test void caveBelowFootprint(){assertEquals("unsupported",check(DecorationPlan.cabin(false),new Flat(){public boolean naturalSupport(int x,int y,int z){return y!=62;}}).reason());}
    @Test void budgetRejectsCompleteObject(){assertEquals("block_budget",PlacementValidator.validate(DecorationPlan.cabin(true),new Flat(),8,64,8,0,0,16,7).reason());}
    @Test void noReadingUnloadedChunk(){assertEquals("unloaded",check(DecorationPlan.cabin(true),new Flat(){public boolean loaded(int x,int z){return false;}public int surface(int x,int z){throw new AssertionError("unloaded world read");}}).reason());}
    @Test void negativeChunkBoundary(){var p=DecorationPlan.cabin(false);assertTrue(PlacementValidator.validate(p,new Flat(),-8,64,-8,-1,-1,512,7).allowed());assertEquals("chunk_boundary",PlacementValidator.validate(p,new Flat(),-1,64,-1,-1,-1,512,7).reason());}
    @Test void noWritingOutsideWorldHeight(){assertEquals("world_height",PlacementValidator.validate(DecorationPlan.cabin(false),new Flat(),8,318,8,0,0,512,7).reason());}
    @Test void maximumObjectSizeRespected(){assertEquals("object_size",PlacementValidator.validate(DecorationPlan.cabin(false),new Flat(),8,64,8,0,0,512,3).reason());}
    @Test void biomeIdentity(){for(var c:DecorationPlan.shrub(true).cells())assertFalse(c.block().contains("SPRUCE"));for(var c:DecorationPlan.shrub(false).cells())assertFalse(c.block().contains("BIRCH"));}
    @Test void determinismAcrossGenerationOrder(){
        Map<Integer,List<DecorationPlan.Cell>> a=new HashMap<>();for(int i=-50;i<50;i++)a.put(i,DecorationPlan.conifer(42,i,-i).cells());
        IntStream.range(-50,50).parallel().forEach(i->assertEquals(a.get(i),DecorationPlan.conifer(42,i,-i).cells()));
    }
    @Test void sparseGridSpacing(){
        List<Integer> xs=new ArrayList<>();for(int x=-64;x<64;x++)if(Candidates.rare(1,x,2,4,7,1))xs.add(x);
        for(int i=1;i<xs.size();i++)assertEquals(4,xs.get(i)-xs.get(i-1));
        assertFalse(Candidates.rare(1,2,2,4,7,0));
    }
    @Test void snowNeverCreatesDeepWall(){for(int x=-500;x<500;x++){int n=Candidates.snowLayers(42,x,-x,3);assertTrue(n>=1&&n<=3);}}
    @Test void invalidBlueprintRejected(){assertThrows(IllegalArgumentException.class,()->new DecorationPlan.Cell(6,1,0,"ICE",0));assertThrows(IllegalArgumentException.class,()->new DecorationPlan.Cell(0,1,0,"CHEST",0));assertThrows(IllegalArgumentException.class,()->new DecorationPlan.Cell(0,1,0,"SNOW",8));}
}
