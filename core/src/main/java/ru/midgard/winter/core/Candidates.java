package ru.midgard.winter.core;
/** One rare candidate at the centre of each spacing cell; prevents adjacent cabins/clearings. */
public final class Candidates {
    private Candidates() {}
    public static boolean rare(long seed,int chunkX,int chunkZ,int spacing,long salt,double perChunkChance) {
        if(spacing<2||spacing>64||!Double.isFinite(perChunkChance)||perChunkChance<0||perChunkChance>1)
            throw new IllegalArgumentException("candidate settings");
        int gx=Math.floorDiv(chunkX,spacing),gz=Math.floorDiv(chunkZ,spacing);
        return Math.floorMod(chunkX,spacing)==spacing/2 && Math.floorMod(chunkZ,spacing)==spacing/2
            && Noise.unit(seed,gx,gz,salt)<Math.min(1,perChunkChance*spacing*spacing);
    }
    public static int snowLayers(long seed,int x,int z,int maximum) {
        if(maximum<1||maximum>6) throw new IllegalArgumentException("snow layers");
        return Math.min(maximum,1+(int)(Noise.smooth(seed,x,z,8)*maximum));
    }
}
