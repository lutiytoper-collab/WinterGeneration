package ru.midgard.winter.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ForestPlacementTest {
    static class Site extends PlacementTest.Flat implements ForestPlacementValidator.Site {
        final Map<String,String> blocks=new HashMap<>();
        String key(int x,int y,int z){return x+","+y+","+z;}
        public String material(int x,int y,int z){return blocks.getOrDefault(key(x,y,z),y<=surface(x,z)?"STONE":"AIR");}
        public boolean replaceable(int x,int y,int z){return Set.of("AIR","SNOW","FERN","SHORT_GRASS").contains(material(x,y,z));}
    }
    final DecorationPlan tree=DecorationPlan.conifer(42,1,1);
    ForestPlacementValidator.Result check(Site site){return ForestPlacementValidator.validate(tree,site,8,64,8,0,0,512,11);}
    @Test void treesUseNaturalHillsWithoutFlattening(){
        var site=new Site(){public int surface(int x,int z){return x==8&&z==8?64:65;}};
        var r=check(site);assertTrue(r.allowed(),r.reason());assertTrue(site.blocks.isEmpty());
        assertEquals("terrain",PlacementValidator.validate(DecorationPlan.cabin(false),site,8,64,8,0,0,512,11).reason());
    }
    @Test void originalMatchingLeavesAreNeverWritten(){
        var site=new Site();var c=tree.cells().stream().filter(v->v.block().equals("SPRUCE_LEAVES")).findFirst().orElseThrow();
        site.blocks.put(site.key(8+c.x(),64+c.y(),8+c.z()),"SPRUCE_LEAVES");var r=check(site);
        assertTrue(r.allowed(),r.reason());assertFalse(r.writes().contains(c));assertEquals("SPRUCE_LEAVES",site.material(8+c.x(),64+c.y(),8+c.z()));
    }
    @Test void otherTreeSpeciesAreNotOverwritten(){
        var site=new Site();var c=tree.cells().stream().filter(v->v.block().equals("SPRUCE_LEAVES")).findFirst().orElseThrow();site.blocks.put(site.key(8+c.x(),64+c.y(),8+c.z()),"BIRCH_LEAVES");assertEquals("obstructed",check(site).reason());
    }
    @Test void originalTrunksCannotBeReplaced(){var site=new Site();site.blocks.put(site.key(8,65,8),"SPRUCE_LOG");assertEquals("obstructed",check(site).reason());}
    @Test void newTrunkMayJoinMatchingNaturalCrownButNeverAnotherSpecies(){
        var site=new Site();site.blocks.put(site.key(8,70,8),"SPRUCE_LEAVES");var r=check(site);assertTrue(r.allowed());
        assertTrue(r.writes().stream().anyMatch(c->c.x()==0&&c.y()==6&&c.z()==0&&c.block().equals("SPRUCE_LOG")));
        site.blocks.put(site.key(8,70,8),"BIRCH_LEAVES");assertFalse(check(site).allowed());
    }
    @Test void playerBlockDeniesWholeObject(){var site=new Site();site.blocks.put(site.key(8,67,8),"DIAMOND_BLOCK");var r=check(site);assertFalse(r.allowed());assertTrue(r.writes().isEmpty());}
    @Test void emptySupportRejectsTree(){assertEquals("unsupported",check(new Site(){public boolean naturalSupport(int x,int y,int z){return false;}}).reason());}
    @Test void protectedVolumeRejectsCanopyToo(){assertEquals("protected",check(new Site(){public boolean protectedBox(Box b){return true;}}).reason());}
    @Test void unloadedChunkIsNeverRead(){assertEquals("unloaded",check(new Site(){public boolean loaded(int x,int z){return false;}public String material(int x,int y,int z){throw new AssertionError();}}).reason());}
    @Test void biomeBoundaryRejectsActualBranch(){var c=tree.cells().getLast();assertEquals("biome",check(new Site(){public boolean allowedBiome(int x,int y,int z){return !(x==8+c.x()&&y==64+c.y()&&z==8+c.z());}}).reason());}
    @Test void chunkAndBudgetRemainHardLimits(){assertEquals("chunk_boundary",ForestPlacementValidator.validate(tree,new Site(),1,64,1,0,0,512,11).reason());assertEquals("block_budget",ForestPlacementValidator.validate(tree,new Site(),8,64,8,0,0,16,11).reason());}
    @Test void allVariantSizesAndHeightAreBounded(){
        Set<Integer> heights=new HashSet<>();
        for(int i=-500;i<500;i++)for(DecorationPlan p:List.of(DecorationPlan.conifer(42,i,-i),DecorationPlan.deciduous(42,i,-i,true),DecorationPlan.deciduous(42,i,-i,false))){
            assertTrue(p.cells().size()<=512);assertTrue(p.bounds(8,0,8).maxY()<=24);assertTrue(p.bounds(8,0,8).inChunk(0,0));
            if(p.cells().stream().anyMatch(c->c.block().equals("SPRUCE_LOG")))heights.add(p.bounds(8,0,8).maxY());
            assertTrue(ForestPlacementValidator.validate(p,new Site(),8,64,8,0,0,512,11).allowed());
        }
        assertTrue(heights.size()>=3);
    }
    @Test void birchesRetainSpecies(){for(var c:DecorationPlan.deciduous(42,1,2,true).cells())assertFalse(c.block().contains("SPRUCE")||c.block().contains("OAK"));}
    @Test void existingTrunkInsideNewCrownStaysUnchanged(){
        var site=new Site();var c=tree.cells().stream().filter(v->v.block().equals("SPRUCE_LEAVES")).findFirst().orElseThrow();site.blocks.put(site.key(8+c.x(),64+c.y(),8+c.z()),"SPRUCE_LOG");
        var r=check(site);assertTrue(r.allowed(),r.reason());assertFalse(r.writes().contains(c));
    }
    @Test void clearingLeavesUnusedPlantsInItsOpening(){
        var site=new Site();site.blocks.put(site.key(8,65,8),"LARGE_FERN");
        var r=ForestPlacementValidator.validateProp(DecorationPlan.clearing(true),site,8,64,8,0,0,512,11);assertTrue(r.allowed(),r.reason());assertTrue(r.writes().stream().noneMatch(c->c.x()==0&&c.y()==1&&c.z()==0));
    }
    @Test void clearingCannotFloatOverALowerPostFoot(){
        var site=new Site(){public int surface(int x,int z){return x==6&&z==9?63:64;}};
        assertEquals("terrain",ForestPlacementValidator.validateProp(DecorationPlan.clearing(true),site,8,64,8,0,0,512,11).reason());
    }
    @Test void clearingChecksSupportUnderEveryFoot(){
        var site=new Site(){public boolean naturalSupport(int x,int y,int z){return !(x==6&&z==9&&y==62);}};
        assertEquals("unsupported",ForestPlacementValidator.validateProp(DecorationPlan.clearing(true),site,8,64,8,0,0,512,11).reason());
    }

}
