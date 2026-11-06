package ru.midgard.winter.core;

import java.util.*;

/** Immutable blueprint; no server references or world writes. Coordinates are offsets from natural ground. */
public record DecorationPlan(String module,List<Cell> cells) {
    public record Position(int x,int y,int z) {}
    public record Cell(int x,int y,int z,String block,int layers) {
        public Cell {
            if(y<1||Math.abs(x)>5||Math.abs(z)>5||y>24) throw new IllegalArgumentException("Template exceeds safe bounds");
            if(!BLOCKS.contains(block)) throw new IllegalArgumentException("Unsupported block: "+block);
            if(block.equals("SNOW")?(layers<1||layers>6):layers!=0) throw new IllegalArgumentException("Invalid layers");
        }
    }
    private static final Set<String> BLOCKS=Set.of("SPRUCE_LOG","SPRUCE_LEAVES","OAK_LOG","OAK_LEAVES",
        "BIRCH_LOG","BIRCH_LEAVES","SPRUCE_PLANKS","SPRUCE_FENCE","SPRUCE_STAIRS","COBBLESTONE",
        "MOSSY_COBBLESTONE","PACKED_ICE","BLUE_ICE","ICE","CALCITE","AMETHYST_BLOCK","AMETHYST_CLUSTER",
        "SEA_LANTERN","END_ROD","PALE_OAK_LEAVES","SNOW","SNOW_BLOCK","LANTERN","SOUL_LANTERN","GLASS_PANE");
    public DecorationPlan {
        Objects.requireNonNull(module); cells=List.copyOf(cells);
        if(cells.isEmpty()) throw new IllegalArgumentException("Empty template");
        Set<Position> used=new HashSet<>();
        for(Cell c:cells) if(!used.add(new Position(c.x,c.y,c.z))) throw new IllegalArgumentException("Duplicate template cell");
    }
    public Box bounds(int x,int ground,int z) {
        return new Box(x+cells.stream().mapToInt(Cell::x).min().orElseThrow(),ground,
            z+cells.stream().mapToInt(Cell::z).min().orElseThrow(),x+cells.stream().mapToInt(Cell::x).max().orElseThrow(),
            ground+cells.stream().mapToInt(Cell::y).max().orElseThrow(),z+cells.stream().mapToInt(Cell::z).max().orElseThrow());
    }
    private static final class Builder {
        final LinkedHashMap<Position,Cell> blocks=new LinkedHashMap<>();
        void put(int x,int y,int z,String block) { put(x,y,z,block,block.equals("SNOW")?1:0); }
        void put(int x,int y,int z,String block,int layers) { blocks.put(new Position(x,y,z),new Cell(x,y,z,block,layers)); }
        void snowTops() {
            List<Cell> base=List.copyOf(blocks.values());
            for(Cell c:base) if((c.block.endsWith("LEAVES")||c.block.endsWith("ICE")||c.block.endsWith("COBBLESTONE"))
                &&c.y<12&&!blocks.containsKey(new Position(c.x,c.y+1,c.z))) put(c.x,c.y+1,c.z,"SNOW");
        }
        DecorationPlan finish(String name) { return new DecorationPlan(name,List.copyOf(blocks.values())); }
    }
    /** Tiered, drooping boughs with irregular heavy snow; tall silhouettes stand above vanilla crowns. */
    public static DecorationPlan conifer(long seed,int cx,int cz) {
        Builder b=new Builder();double variant=Noise.unit(seed,cx,cz,310);
        int height=variant<.20?14:variant<.80?18+(int)(variant*2):22;
        int tiers=height<16?4:5;
        for(int y=1;y<=height;y++) b.put(0,y,0,"SPRUCE_LOG");
        for(int level=0;level<tiers;level++) {
            int tier=(int)Math.round(3+level*(height-5.0)/(tiers-1));
            int radius=height<16?Math.max(1,3-level):5-level;
            for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++) {
                int distance=Math.abs(x)+Math.abs(z);
                if(distance>radius+1||(Math.abs(x)==radius&&Math.abs(z)==radius))continue;
                if(x==0&&z==0)continue;
                // A vaulted branch surface and low outer tips, rather than flat stacked discs.
                int y=tier+(distance<=radius/2?1:0);
                b.put(x,y,z,"SPRUCE_LEAVES");
                if(distance==radius&&radius>=2&&(x==0||z==0))b.put(x,y-1,z,"SPRUCE_LEAVES");
            }
            if(radius>=3)for(int d=1;d<radius-1;d++) {
                if(tier%2==1){b.put(d,tier,0,"SPRUCE_LOG");b.put(-d,tier,0,"SPRUCE_LOG");}
                else{b.put(0,tier,d,"SPRUCE_LOG");b.put(0,tier,-d,"SPRUCE_LOG");}
            }
        }
        b.put(0,height+1,0,"SPRUCE_LEAVES");
        winterCaps(b,seed,cx,cz,.43);return b.finish("forests");
    }
    public static DecorationPlan deciduous(long seed,int cx,int cz,boolean birch) {
        Builder b=new Builder();int height=(birch?10:12)+(int)(Noise.unit(seed,cx,cz,312)*3);
        String log=birch?"BIRCH_LOG":"OAK_LOG",leaf=birch?"BIRCH_LEAVES":"OAK_LEAVES";
        for(int y=1;y<=height-1;y++)b.put(0,y,0,log);
        int side=Noise.unit(seed,cx,cz,313)<.5?-1:1;
        b.put(side,height-4,0,log);b.put(side,height-3,0,log);
        int radius=birch?3:4;
        for(int y=height-4;y<=height+1;y++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++) {
            if(x*x+z*z+2*(y-height+1)*(y-height+1)>radius*radius+1)continue;
            Position pos=new Position(x,y,z);
            if(!b.blocks.containsKey(pos))b.put(x,y,z,leaf);
        }
        winterCaps(b,seed,cx,cz,.15);return b.finish("forests");
    }
    private static void winterCaps(Builder b,long seed,int cx,int cz,double heavy) {
        List<Cell> tops=b.blocks.values().stream().filter(c->c.block.endsWith("LEAVES")&&!b.blocks.containsKey(new Position(c.x,c.y+1,c.z))).toList();
        int heavyBudget=Math.max(0,512-b.blocks.size()-tops.size());
        for(Cell c:tops) {
            double v=Noise.smooth(seed^(c.y*317L),cx*17+c.x,cz*17+c.z,4);
            if(v<heavy&&heavyBudget>0&&c.y+2<=24&&!b.blocks.containsKey(new Position(c.x,c.y+2,c.z))){
                b.put(c.x,c.y+1,c.z,"SNOW_BLOCK");b.put(c.x,c.y+2,c.z,"SNOW",2);heavyBudget--;
            }else b.put(c.x,c.y+1,c.z,"SNOW",2+(int)(v*4));
        }
    }
    public static DecorationPlan shrub(boolean birch) {
        Builder b=new Builder();b.put(0,1,0,birch?"BIRCH_LOG":"OAK_LOG");
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) if(x!=0||z!=0) b.put(x,1,z,birch?"BIRCH_LEAVES":"OAK_LEAVES");
        b.put(0,2,0,birch?"BIRCH_LEAVES":"OAK_LEAVES"); b.snowTops(); return b.finish("forests");
    }
    public static DecorationPlan fallenLog(boolean birch) {
        Builder b=new Builder();for(int x=-2;x<=2;x++) {b.put(x,1,0,birch?"BIRCH_LOG":"SPRUCE_LOG");b.put(x,2,0,"SNOW");}
        return b.finish("fallen-logs");
    }
    public static DecorationPlan ice(long seed,int cx,int cz) {
        Builder b=new Builder();int height=7+(int)(Noise.unit(seed,cx,cz,320)*3);
        int[][] spires={{0,0,height},{-3,1,4},{2,2,5},{1,-3,3}};
        for(int[] spire:spires){
            for(int y=1;y<=spire[2];y++){
                int radius=y<=spire[2]/2?1:0;
                for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)if(Math.abs(x)+Math.abs(z)<=radius)
                    b.put(spire[0]+x,y,spire[1]+z,y<=2?"PACKED_ICE":(y%3==0?"ICE":"BLUE_ICE"));
            }
            b.put(spire[0],spire[2]+1,spire[1],"AMETHYST_CLUSTER");
        }
        b.put(0,1,-1,"SEA_LANTERN");b.put(-3,1,0,"AMETHYST_BLOCK");b.put(3,1,-2,"END_ROD");
        b.snowTops();return b.finish("ice");
    }
    public static DecorationPlan clearing(boolean light) {
        Builder b=new Builder();
        for(int x:new int[]{-2,2})for(int y=1;y<=4;y++)b.put(x,y,1,"SPRUCE_LOG");
        for(int x=-2;x<=2;x++)b.put(x,4,1,"SPRUCE_LOG");
        for(int x=-3;x<=3;x++)for(int z=0;z<=2;z++){
            int y=5+(Math.abs(x)<=1?1:0);b.put(x,y,z,"PALE_OAK_LEAVES");b.put(x,y+1,z,"SNOW",4);
        }
        if(light)b.put(0,3,1,"LANTERN");
        b.put(-2,1,-2,"SPRUCE_STAIRS");b.put(-1,1,-2,"SPRUCE_STAIRS");
        b.put(2,1,-2,"CALCITE");b.put(2,2,-2,"SNOW",4);
        b.put(2,1,-1,"COBBLESTONE");b.put(2,2,-1,"SNOW",2);
        return b.finish("clearings");
    }
    public static DecorationPlan cabin(boolean light) {
        Builder b=new Builder();
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            b.put(x,1,z,"SPRUCE_PLANKS");
            for(int y=2;y<=4;y++) if(Math.abs(x)==2||Math.abs(z)==2) {
                if(x==0&&z==-2&&y<=3) continue;
                String block=(Math.abs(x)==2&&z==0&&y==3)?"GLASS_PANE":"SPRUCE_LOG";
                b.put(x,y,z,block);
            }
        }
        for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) {
            int y=5+(3-Math.abs(x))/2; b.put(x,y,z,"SPRUCE_PLANKS");b.put(x,y+1,z,"SNOW");
        }
        b.put(0,1,-3,"SPRUCE_STAIRS");
        if(light) b.put(1,2,1,"LANTERN");
        return b.finish("cabins");
    }
}
