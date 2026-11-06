package ru.midgard.winter.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StorybookPropsTest {
    @Test void crystalClusterIsBoundedAndHasNaturalFeet(){
        for(int i=-100;i<100;i++){
            var p=DecorationPlan.ice(42,i,-i);assertTrue(p.cells().size()<256);assertTrue(p.bounds(8,64,8).inChunk(0,0));
            var r=ForestPlacementValidator.validateProp(p,new ForestPlacementTest.Site(),8,64,8,0,0,512,11);
            assertTrue(r.allowed(),r.reason());assertTrue(p.bounds(8,0,8).maxY()>=8);
        }
    }
    @Test void lightAndAmethystDoNotBypassProtection(){
        var p=DecorationPlan.ice(42,2,2);
        var s=new ForestPlacementTest.Site(){public boolean protectedBox(Box b){return true;}};
        var r=ForestPlacementValidator.validateProp(p,s,8,64,8,0,0,512,11);assertEquals("protected",r.reason());assertTrue(r.writes().isEmpty());
    }
    @Test void icyShelterCanopyKeepsSnowSupported(){
        var p=DecorationPlan.clearing(true);assertTrue(p.cells().stream().anyMatch(c->c.block().equals("PALE_OAK_LEAVES")));
        var s=new ForestPlacementTest.Site();var f=FoundationFitter.fit(p,s,8,8,2);assertNotNull(f);
        assertTrue(ForestPlacementValidator.validateProp(f.plan(),s,8,f.ground(),8,0,0,512,11).allowed());
    }
}
