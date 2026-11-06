package ru.midgard.winter.core;
/** World-coordinate fields join across chunks without loading their neighbours. */
public final class WinterLandscape {
    private WinterLandscape(){}
    public static double trailDistance(long seed,int x,int z){
        int band=Math.floorDiv(z,128);double nearest=Double.POSITIVE_INFINITY;
        for(int offset=-1;offset<=1;offset++){
            int row=band+offset;
            double line=row*128.0+64+(Noise.smooth(seed^0x74a11L,x,row*37,64)-.5)*42;
            nearest=Math.min(nearest,Math.abs(z-line));
        }
        return nearest;
    }
    public static boolean trail(long seed,int x,int z){return trailDistance(seed,x,z)<1.15;}
    public static boolean trailShoulder(long seed,int x,int z){return trailDistance(seed,x,z)<6.5;}
    public static int driftLayers(long seed,int x,int z,int maximum){
        if(maximum<1||maximum>6)throw new IllegalArgumentException("snow layers");
        double dune=Noise.smooth(seed^0x5a0L,x,z,18),detail=Noise.smooth(seed,x,z,5);
        // Keep connected shallow areas for walking and airdrops; deep banks form coherent pockets.
        if(dune<.55)return Math.min(maximum,1+(int)(detail*2));
        return Math.min(maximum,2+(int)(Math.min(1,(dune-.55)/.35)*Math.max(0,maximum-2)));
    }
}
