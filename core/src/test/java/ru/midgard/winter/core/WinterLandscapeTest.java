package ru.midgard.winter.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WinterLandscapeTest {
    @Test void trailsAreSparseAndContinuousAcrossChunkAndNegativeBoundaries(){
        for(int x=-256;x<256;x++){
            double best=999;int zBest=0;
            for(int z=-110;z<0;z++){double d=WinterLandscape.trailDistance(42,x,z);if(d<best){best=d;zBest=z;}}
            assertTrue(WinterLandscape.trail(42,x,zBest));assertTrue(WinterLandscape.trailShoulder(42,x,zBest));
            assertTrue(WinterLandscape.trailDistance(42,x+1,zBest)<2.3,"Disconnected trail at "+x);
        }
    }
    @Test void differentSeedsChangeTheRoute(){boolean changed=false;for(int x=-100;x<100;x++)changed|=WinterLandscape.trailDistance(42,x,64)!=WinterLandscape.trailDistance(43,x,64);assertTrue(changed);}
    @Test void driftsAreBoundedAndSmooth(){for(int x=-1000;x<1000;x++){int a=WinterLandscape.driftLayers(42,x,-17,6),b=WinterLandscape.driftLayers(42,x+1,-17,6);assertTrue(a>=1&&a<=6);assertTrue(Math.abs(a-b)<=1);}}
}
