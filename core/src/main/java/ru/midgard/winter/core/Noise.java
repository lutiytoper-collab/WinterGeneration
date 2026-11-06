package ru.midgard.winter.core;
/** Stateless coordinate hashing and smooth value noise; independent of generation order. */
public final class Noise {
    private Noise() {}
    public static long hash(long seed, int x, int z, long salt) {
        long h = seed ^ (x * 0x632be59bd9b4e019L) ^ (z * 0x9e3779b97f4a7c15L) ^ salt;
        h = (h ^ (h >>> 30)) * 0xbf58476d1ce4e5b9L;
        h = (h ^ (h >>> 27)) * 0x94d049bb133111ebL;
        return h ^ (h >>> 31);
    }
    public static double unit(long seed, int x, int z, long salt) {
        return (hash(seed,x,z,salt) >>> 11) * 0x1.0p-53;
    }
    public static double smooth(long seed, int x, int z, int scale) {
        if(scale < 1) throw new IllegalArgumentException("scale");
        int gx = Math.floorDiv(x,scale), gz = Math.floorDiv(z,scale);
        double fx = Math.floorMod(x,scale)/(double)scale, fz = Math.floorMod(z,scale)/(double)scale;
        fx=fx*fx*(3-2*fx); fz=fz*fz*(3-2*fz);
        double a=unit(seed,gx,gz,19), b=unit(seed,gx+1,gz,19);
        double c=unit(seed,gx,gz+1,19), d=unit(seed,gx+1,gz+1,19);
        return (a+(b-a)*fx)*(1-fz)+(c+(d-c)*fx)*fz;
    }
}
