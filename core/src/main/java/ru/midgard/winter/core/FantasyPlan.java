package ru.midgard.winter.core;
import java.util.*;
/** Entity-free, bounded fantasy flora; placement and all world reads remain in the main-thread validator. */
public final class FantasyPlan {
    private FantasyPlan(){}
    private static final class Sketch {
        final Map<DecorationPlan.Position,DecorationPlan.Cell> cells=new LinkedHashMap<>();
        void put(int x,int y,int z,String block){put(x,y,z,block,0);}
        void put(int x,int y,int z,String block,int layers){cells.put(new DecorationPlan.Position(x,y,z),new DecorationPlan.Cell(x,y,z,block,layers));}
        void cap(){
            for(var c:List.copyOf(cells.values()))if((c.block().endsWith("LEAVES")||c.block().equals("WARPED_WART_BLOCK")||c.block().equals("CALCITE")||c.block().equals("BLUE_ICE"))&&!cells.containsKey(new DecorationPlan.Position(c.x(),c.y()+1,c.z())))put(c.x(),c.y()+1,c.z(),"SNOW",3);
        }
        DecorationPlan done(String name){return new DecorationPlan(name,List.copyOf(cells.values()));}
    }
    public static DecorationPlan mushrooms(long seed,int x,int z){
        Sketch b=new Sketch();int h=4+(int)(Noise.unit(seed,x,z,940)*3);
        for(int y=1;y<=h;y++)b.put(0,y,0,"MUSHROOM_STEM");
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(Math.abs(dx)+Math.abs(dz)<=3){
            b.put(dx,h+1,dz,"WARPED_WART_BLOCK");
            if(Math.abs(dx)+Math.abs(dz)<=1)b.put(dx,h+2,dz,"WARPED_WART_BLOCK");
        }
        b.put(0,h,0,"PEARLESCENT_FROGLIGHT");b.put(2,h,0,"LIGHT_BLUE_STAINED_GLASS");b.put(-2,h,0,"PURPLE_STAINED_GLASS");
        b.cap();return b.done("moon-mushrooms");
    }
    public static DecorationPlan willow(boolean birch){
        Sketch b=new Sketch();String log=birch?"BIRCH_LOG":"PALE_OAK_LOG";
        for(int y=1;y<=4;y++)b.put(0,y,0,log);
        for(int y=4;y<=6;y++)b.put(1,y,0,log);
        for(int y=6;y<=8;y++)b.put(2,y,0,log);
        b.put(-1,4,0,log);b.put(-2,4,0,log);b.put(-2,5,0,log);
        for(int[] center:new int[][]{{2,7,0},{-2,6,0},{0,6,-2}}){
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=-1;dy<=1;dy++){
                if(dx*dx+dz*dz+dy*dy*3>5)continue;
                int px=center[0]+dx,py=center[1]+dy,pz=center[2]+dz;
                var key=new DecorationPlan.Position(px,py,pz);
                if(!b.cells.containsKey(key))b.put(px,py,pz,"CHERRY_LEAVES");
            }
        }
        b.put(2,6,1,"CHERRY_LEAVES");b.put(2,5,1,"LANTERN");
        b.put(-2,5,-1,"CHERRY_LEAVES");b.put(-2,4,-1,"SOUL_LANTERN");
        b.cap();return b.done("lantern-willows");
    }
    public static DecorationPlan moonstones(long seed,int x,int z){
        Sketch b=new Sketch();int h=3+(int)(Noise.unit(seed,x,z,941)*2);
        for(int[] p:new int[][]{{0,0,h},{-2,1,2},{2,-1,2}}){
            for(int y=1;y<=p[2];y++)b.put(p[0],y,p[1],y==1?"PEARLESCENT_FROGLIGHT":y==2?"CALCITE":"BLUE_ICE");
            b.put(p[0],p[2]+1,p[1],"AMETHYST_CLUSTER");
        }
        return b.done("moonstones");
    }
    public static DecorationPlan choose(long seed,int x,int z,boolean birch){
        double v=Noise.unit(seed,x,z,942);
        return v<.4?mushrooms(seed,x,z):v<.8?willow(birch):moonstones(seed,x,z);
    }
}
