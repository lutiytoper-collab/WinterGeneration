package ru.midgard.winter;
import org.junit.jupiter.api.Test;
import ru.midgard.winter.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class SchematicCatalogTest {
    @Test void everyAuditedGateAndRotationIsBoundedAndGroundSupported()throws Exception{
        var plans=SchematicCatalog.load(n->getClass().getResourceAsStream("/"+n));assertEquals(6,plans.size());
        var flat=new ForestPlacementValidator.Site(){
            public int minHeight(){return -64;}public int maxHeight(){return 320;}public int surface(int x,int z){return 64;}
            public boolean loaded(int x,int z){return true;}public boolean naturalGround(int x,int y,int z){return y==64;}public boolean naturalSupport(int x,int y,int z){return y<64;}
            public boolean allowedBiome(int x,int y,int z){return true;}public boolean replaceable(int x,int y,int z){return y>64;}public String material(int x,int y,int z){return y<=64?"STONE":"AIR";}public boolean protectedBox(Box b){return false;}
        };
        for(var plan:plans)for(int turn=0;turn<4;turn++){
            var rotated=plan.rotated(turn);assertEquals(plan,plan.rotated(4));assertEquals(plan.cells().size(),rotated.cells().size());
            assertTrue(rotated.cells().size()<=512);assertTrue(rotated.bounds(8,64,8).inChunk(0,0));
            var v=ForestPlacementValidator.validateProp(rotated,flat,8,64,8,0,0,512,11);assertTrue(v.allowed(),v.reason());
        }
    }
    @Test void biomePoolsKeepWoodlandAndColdLandmarksDistinct()throws Exception{
        var plans=SchematicCatalog.load(n->getClass().getResourceAsStream("/"+n));
        assertEquals(4,SchematicCatalog.forBiome(plans,"snowy_taiga").size());
        assertTrue(SchematicCatalog.forBiome(plans,"birch_forest").stream().noneMatch(p->p.module().equals("frost-shrine")));
        assertEquals(6,SchematicCatalog.forBiome(plans,"plains").size());
    }
    @Test void unsafePaletteCannotBecomeRuntimeBlocks(){
        String bad="{\"format\":1,\"id\":\"moon-gate-7\",\"license\":\"CC0-1.0\",\"cells\":[{\"x\":0,\"y\":1,\"z\":0,\"block\":\"COMMAND_BLOCK\",\"layers\":0}]}";
        assertThrows(IllegalArgumentException.class,()->SchematicCatalog.load(n->new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8))));
    }
    @Test void truncatedOrMissingTemplatesFailClosed(){assertThrows(Exception.class,()->SchematicCatalog.load(n->null));}
}
