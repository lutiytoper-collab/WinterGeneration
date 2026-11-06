package ru.midgard.winter.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FantasySafetyTest {
    static List<DecorationPlan> plans(int i){return List.of(FantasyPlan.mushrooms(42,i,-i),FantasyPlan.willow(false),FantasyPlan.willow(true),FantasyPlan.moonstones(42,i,-i));}
    @Test void preciousBlocksAndRareLampsCannotEnterAnyBlueprint(){
        for(String block:List.of("DIAMOND_BLOCK","EMERALD_BLOCK","GOLD_BLOCK","IRON_BLOCK","COPPER_BLOCK","NETHERITE_BLOCK","LAPIS_BLOCK","REDSTONE_BLOCK","AMETHYST_BLOCK","AMETHYST_CLUSTER","SEA_LANTERN","PEARLESCENT_FROGLIGHT","PACKED_ICE","BLUE_ICE","LANTERN","SOUL_LANTERN","END_ROD","CHEST","SPAWNER"))
            assertThrows(IllegalArgumentException.class,()->new DecorationPlan.Cell(0,1,0,block,0),block);
    }
    @Test void everyVariantIsBoundedAndPassesTheFullPreflight(){
        for(int i=-200;i<200;i++)for(var p:plans(i)){
            assertTrue(p.cells().size()<=512);assertTrue(p.bounds(8,64,8).inChunk(0,0));
            var r=ForestPlacementValidator.validateProp(p,new ForestPlacementTest.Site(),8,64,8,0,0,512,11);assertTrue(r.allowed(),p.module()+":"+r.reason());
        }
    }
    @Test void noWonderCanOverwritePlayerBlocks(){for(var p:plans(1)){var s=new ForestPlacementTest.Site();s.blocks.put(s.key(8,65,8),"DIAMOND_BLOCK");assertFalse(ForestPlacementValidator.validateProp(p,s,8,64,8,0,0,512,11).allowed());}}
    @Test void protectedVolumeRejectsEveryWonder(){for(var p:plans(1)){var s=new ForestPlacementTest.Site(){public boolean protectedBox(Box b){return true;}};var v=ForestPlacementValidator.validateProp(p,s,8,64,8,0,0,512,11);assertEquals("protected",v.reason());assertTrue(v.writes().isEmpty());}}
    @Test void mineralFeetFollowSlopesWithoutCarvingGround(){
        var s=new FoundationFitterTest.Hill();for(var p:List.of(DecorationPlan.ice(42,1,1),FantasyPlan.moonstones(42,1,1))){
            var f=FoundationFitter.fitMineral(p,s,8,8,2);assertNotNull(f);
            var v=ForestPlacementValidator.validateProp(f.plan(),s,8,f.ground(),8,0,0,512,11);assertTrue(v.allowed(),v.reason());
            for(var c:v.writes())assertTrue(f.ground()+c.y()>s.surface(8+c.x(),8+c.z()));assertTrue(s.blocks.isEmpty());
        }
    }
    @Test void mineralCannotBridgeCliffsOrUnsupportedFeet(){
        var p=DecorationPlan.ice(42,1,1);assertNull(FoundationFitter.fitMineral(p,new FoundationFitterTest.Hill(){public int surface(int x,int z){return x<8?64:70;}},8,8,2));
        var s=new FoundationFitterTest.Hill(){public boolean naturalSupport(int x,int y,int z){return false;}};var f=FoundationFitter.fitMineral(p,s,8,8,2);assertNotNull(f);assertFalse(ForestPlacementValidator.validateProp(f.plan(),s,8,f.ground(),8,0,0,512,11).allowed());
    }
    @Test void luminousCrystalsContainNoMeltingIceOrFluids(){for(int i=-100;i<100;i++)for(var c:DecorationPlan.ice(42,i,i).cells())assertFalse(Set.of("ICE","WATER","LAVA").contains(c.block()));}
    @Test void birchWillowRetainsBirchWood(){var p=FantasyPlan.willow(true);assertTrue(p.cells().stream().anyMatch(c->c.block().equals("BIRCH_LOG")));assertFalse(p.cells().stream().anyMatch(c->c.block().equals("PALE_OAK_LOG")));}
    @Test void independentCrystalGridDoesNotCollideWithTheClearingGrid(){
        int clearings=0,crystals=0;
        for(int x=-40;x<40;x++)for(int z=-40;z<40;z++){
            boolean a=Candidates.rare(42,x,z,4,202,1),b=Candidates.rare(42,x+1,z+1,4,204,1);
            if(a)clearings++;if(b)crystals++;assertFalse(a&&b);
        }assertEquals(400,clearings);assertEquals(400,crystals);
    }
    @Test void wonderKindsAndCoordinatesAreDeterministic(){var kinds=new HashSet<String>();for(int i=0;i<100;i++){var a=FantasyPlan.choose(42,i,-i,false);assertEquals(a,FantasyPlan.choose(42,i,-i,false));kinds.add(a.module());}assertEquals(Set.of("moon-mushrooms","lantern-willows","moonstones"),kinds);}
}
