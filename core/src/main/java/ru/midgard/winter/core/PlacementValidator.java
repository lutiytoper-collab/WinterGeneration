package ru.midgard.winter.core;

/** Complete preflight before the first write. Unavailable knowledge always denies placement. */
public final class PlacementValidator {
    public interface Site {
        int minHeight(); int maxHeight();
        boolean loaded(int x,int z);
        int surface(int x,int z);
        boolean naturalGround(int x,int y,int z);
        boolean naturalSupport(int x,int y,int z);
        boolean allowedBiome(int x,int y,int z);
        /** Only literal AIR, or a thin natural snow layer removed as part of this validated footprint. */
        boolean replaceable(int x,int y,int z);
        boolean protectedBox(Box box);
    }
    public record Result(boolean allowed,String reason) {}
    private PlacementValidator() {}
    public static Result validate(DecorationPlan plan,Site site,int x,int ground,int z,int cx,int cz,int budget,int size) {
        if(plan.cells().size()>budget) return new Result(false,"block_budget");
        Box box=plan.bounds(x,ground,z);
        if(!box.inChunk(cx,cz)) return new Result(false,"chunk_boundary");
        if(box.maxX()-box.minX()+1>size||box.maxZ()-box.minZ()+1>size) return new Result(false,"object_size");
        if(ground-2<site.minHeight()||box.maxY()>=site.maxHeight()) return new Result(false,"world_height");
        for(int px=box.minX();px<=box.maxX();px++) for(int pz=box.minZ();pz<=box.maxZ();pz++)
            if(!site.loaded(px,pz)) return new Result(false,"unloaded");
        if(site.protectedBox(box)) return new Result(false,"protected");
        for(int px=box.minX();px<=box.maxX();px++) for(int pz=box.minZ();pz<=box.maxZ();pz++) {
            if(site.surface(px,pz)!=ground||!site.naturalGround(px,ground,pz)) return new Result(false,"terrain");
            for(int depth=1;depth<=2;depth++) if(!site.naturalSupport(px,ground-depth,pz)) return new Result(false,"unsupported");
            for(int py=ground+1;py<=box.maxY();py++) {
                if(!site.allowedBiome(px,py,pz)) return new Result(false,"biome");
                if(!site.replaceable(px,py,pz)) return new Result(false,"obstructed");
            }
        }
        return new Result(true,"safe");
    }
}
