package ru.midgard.winter.core;
import java.util.*;
/** Check actual tree cells and its root, allowing hills and keeping matching natural foliage intact. */
public final class ForestPlacementValidator {
    public interface Site extends PlacementValidator.Site { String material(int x,int y,int z); }
    public record Result(boolean allowed,String reason,List<DecorationPlan.Cell> writes) {
        public Result{writes=List.copyOf(writes);}
    }
    private static Result deny(String reason){return new Result(false,reason,List.of());}
    public static Result validate(DecorationPlan plan,Site site,int x,int ground,int z,int cx,int cz,int budget,int size){
        return resolve(plan,site,x,ground,z,cx,cz,budget,size,false);
    }
    public static Result validateProp(DecorationPlan plan,Site site,int x,int ground,int z,int cx,int cz,int budget,int size){
        return resolve(plan,site,x,ground,z,cx,cz,budget,size,true);
    }
    private static Result resolve(DecorationPlan plan,Site site,int x,int ground,int z,int cx,int cz,int budget,int size,boolean feet){
        if(plan.cells().size()>budget)return deny("block_budget");
        Box box=plan.bounds(x,ground,z);
        if(!box.inChunk(cx,cz))return deny("chunk_boundary");
        if(box.maxX()-box.minX()+1>size||box.maxZ()-box.minZ()+1>size)return deny("object_size");
        if(ground-2<site.minHeight()||box.maxY()>=site.maxHeight())return deny("world_height");
        for(int px=box.minX();px<=box.maxX();px++)for(int pz=box.minZ();pz<=box.maxZ();pz++)if(!site.loaded(px,pz))return deny("unloaded");
        if(site.protectedBox(box))return deny("protected");
        if(!site.naturalGround(x,ground,z))return deny("terrain");
        for(int d=1;d<=2;d++)if(!site.naturalSupport(x,ground-d,z))return deny("unsupported");
        Set<DecorationPlan.Position> positions=new HashSet<>();for(var c:plan.cells())positions.add(new DecorationPlan.Position(c.x(),c.y(),c.z()));
        if(feet)for(var c:plan.cells())if(c.y()<=3&&!Set.of("SNOW","SNOW_BLOCK","LANTERN","SOUL_LANTERN").contains(c.block())&&!positions.contains(new DecorationPlan.Position(c.x(),c.y()-1,c.z()))){
            int px=x+c.x(),pz=z+c.z(),foot=ground+c.y()-1;
            if(site.surface(px,pz)!=foot||!site.naturalGround(px,foot,pz))return deny("terrain");
            for(int d=1;d<=2;d++)if(!site.naturalSupport(px,foot-d,pz))return deny("unsupported");
        }
        List<DecorationPlan.Cell> writes=new ArrayList<>();Map<DecorationPlan.Position,String> future=new HashMap<>();
        Set<String> leaves=new HashSet<>();for(var c:plan.cells())if(c.block().endsWith("_LEAVES"))leaves.add(c.block());
        for(var c:plan.cells()){
            int px=x+c.x(),py=ground+c.y(),pz=z+c.z();
            if(!site.allowedBiome(px,py,pz))return deny("biome");
            String old=site.material(px,py,pz);
            if(old.equals(c.block())&&old.endsWith("_LEAVES"))continue;
            if(!feet&&c.block().endsWith("_LEAVES")&&old.endsWith("_LOG"))continue;
            // A new trunk may join matching natural foliage in this new chunk; original logs stay immutable.
            if(c.block().endsWith("_LOG")&&leaves.contains(old)){
                writes.add(c);future.put(new DecorationPlan.Position(c.x(),c.y(),c.z()),c.block());continue;
            }
            boolean snow=c.block().equals("SNOW")||c.block().equals("SNOW_BLOCK");
            if(snow&&(leaves.contains(old)||(!feet&&old.endsWith("_LOG"))))continue; // Preserve the overlapping original crown.
            if(!site.replaceable(px,py,pz))return deny("obstructed");
            writes.add(c);future.put(new DecorationPlan.Position(c.x(),c.y(),c.z()),c.block());
        }
        // Snow must rest on real/planned support even where an existing crown was preserved.
        for(var c:writes)if(c.block().equals("SNOW")||c.block().equals("SNOW_BLOCK")){
            String below=future.getOrDefault(new DecorationPlan.Position(c.x(),c.y()-1,c.z()),site.material(x+c.x(),ground+c.y()-1,z+c.z()));
            if(!(below.endsWith("_LEAVES")||below.endsWith("_LOG")||Set.of("WARPED_WART_BLOCK","PEARLESCENT_FROGLIGHT","SNOW_BLOCK","COBBLESTONE","MOSSY_COBBLESTONE","CALCITE","AMETHYST_BLOCK","BLUE_ICE","SPRUCE_PLANKS","SPRUCE_STAIRS","ICE","PACKED_ICE").contains(below)))return deny("snow_support");
        }
        return new Result(true,"safe",writes);
    }
}
