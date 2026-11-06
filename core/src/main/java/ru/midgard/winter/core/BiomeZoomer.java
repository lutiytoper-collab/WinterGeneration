package ru.midgard.winter.core;
import java.nio.*;
import java.security.*;
/** Minecraft 1.21.11 biome zoom mathematics; independent of NMS and of chunk loading. */
public final class BiomeZoomer {
    public record Quart(int x,int y,int z){}
    private BiomeZoomer(){}
    public static long seed(long worldSeed){
        try{byte[] bytes=ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(worldSeed).array();return ByteBuffer.wrap(MessageDigest.getInstance("SHA-256").digest(bytes)).order(ByteOrder.LITTLE_ENDIAN).getLong();}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public static Quart sample(long seed,int x,int y,int z){
        int px=x-2,py=y-2,pz=z-2,qx=px>>2,qy=py>>2,qz=pz>>2;
        double fx=(px&3)/4.0,fy=(py&3)/4.0,fz=(pz&3)/4.0,best=Double.POSITIVE_INFINITY;Quart result=null;
        for(int i=0;i<8;i++){
            boolean bx=(i&4)==0,by=(i&2)==0,bz=(i&1)==0;
            int sx=qx+(bx?0:1),sy=qy+(by?0:1),sz=qz+(bz?0:1);long s=seed;
            for(int value:new int[]{sx,sy,sz,sx,sy,sz})s=next(s,value);
            double dx=(bx?fx:fx-1)+fiddle(s);s=next(s,seed);
            double dy=(by?fy:fy-1)+fiddle(s);s=next(s,seed);
            double dz=(bz?fz:fz-1)+fiddle(s),distance=dz*dz+dy*dy+dx*dx;
            if(distance<best){best=distance;result=new Quart(sx,sy,sz);}
        }
        return result;
    }
    private static long next(long seed,long salt){return seed*(seed*6364136223846793005L+1442695040888963407L)+salt;}
    private static double fiddle(long seed){return (Math.floorMod(seed>>24,1024)/1024.0-.5)*.9;}
    /** Conservative original warm-climate bound; uncertain high-altitude cases are not rewritten. */
    public static boolean certainlyWarm(double original,int y,int seaLevel){
        return original-(y>seaLevel+17?(y-(seaLevel+17)+16)*.05/40:0)>.151;
    }
}
