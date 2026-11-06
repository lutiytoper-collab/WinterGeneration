package ru.midgard.winter.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FoundationFitterTest {
    static class Hill extends ForestPlacementTest.Site {
        public int surface(int x,int z){return x<8?64:65;}
        public boolean naturalGround(int x,int y,int z){return y<=surface(x,z);}
        public boolean naturalSupport(int x,int y,int z){return y<surface(x,z);}
    }
    @Test void clearingAdaptsToASmallSlopeWithoutChangingTheSoil(){
        var site=new Hill();var fitted=FoundationFitter.fit(DecorationPlan.clearing(true),site,8,8,2);assertNotNull(fitted);
        var r=ForestPlacementValidator.validateProp(fitted.plan(),site,8,fitted.ground(),8,0,0,512,11);assertTrue(r.allowed(),r.reason());
        for(var c:r.writes())assertTrue(fitted.ground()+c.y()>site.surface(8+c.x(),8+c.z()));assertTrue(site.blocks.isEmpty());
    }
    @Test void noFoundationIsExtendedAcrossASteepCliff(){assertNull(FoundationFitter.fit(DecorationPlan.clearing(true),new Hill(){public int surface(int x,int z){return x<8?64:68;}},8,8,2));}
    @Test void everyFittedFootStillRequiresSolidSupport(){var site=new Hill(){public boolean naturalSupport(int x,int y,int z){return !(x==6&&z==9)&&super.naturalSupport(x,y,z);}};var f=FoundationFitter.fit(DecorationPlan.clearing(true),site,8,8,2);assertNotNull(f);assertFalse(ForestPlacementValidator.validateProp(f.plan(),site,8,f.ground(),8,0,0,512,11).allowed());}
    @Test void benchesStayAtLocalSittingHeightRatherThanOnTallStilts(){
        var site=new Hill();var f=FoundationFitter.fit(DecorationPlan.clearing(true),site,8,8,2);
        assertNotNull(f);for(var c:f.plan().cells())if(c.block().equals("SPRUCE_STAIRS"))assertEquals(site.surface(8+c.x(),8+c.z())+1,f.ground()+c.y());
    }

}
