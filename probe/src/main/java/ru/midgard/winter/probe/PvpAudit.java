package ru.midgard.winter.probe;

import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.block.data.type.Snow;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
import java.util.*;

/** QA ONLY. Read-only native terrain search and exhaustive 64x64 surface inspection. */
final class PvpAudit {
    private final JavaPlugin owner;private final World world;
    private final JsonObject report=new JsonObject();private final JsonArray rows=new JsonArray();
    private final List<Chunk> held=new ArrayList<>();private final ArrayDeque<int[]> queue=new ArrayDeque<>();
    private int x0,z0,index;private final int size=64;private final int[][] heights=new int[size][size];
    private int marker,wet,obstacles,unsupported,canopies,min=999,max=-999,maxSnowLayers;
    private static final Set<Material> GROUND=Set.of(Material.GRASS_BLOCK,Material.DIRT,Material.ROOTED_DIRT,Material.COARSE_DIRT,Material.PODZOL,Material.STONE,Material.DEEPSLATE,Material.ANDESITE,Material.DIORITE,Material.GRANITE,Material.GRAVEL,Material.SAND,Material.WATER,Material.LAVA,Material.ICE,Material.PACKED_ICE,Material.BLUE_ICE,Material.SNOW_BLOCK,Material.POWDER_SNOW,Material.TUFF,Material.CALCITE);
    PvpAudit(JavaPlugin owner){this.owner=owner;world=Objects.requireNonNull(Bukkit.getWorld("winter_test"));report.addProperty("seed",world.getSeed());report.addProperty("minecraft",Bukkit.getMinecraftVersion());report.addProperty("pluginVersion",Bukkit.getPluginManager().getPlugin("MidgardWinterWorld").getPluginMeta().getVersion());}
    void search(){
        report.add("samples",rows);
        int originX=0,originZ=0;Path origin=owner.getDataFolder().toPath().resolve("pvp-search-origin.json");
        try{if(Files.exists(origin)){var d=JsonParser.parseString(Files.readString(origin)).getAsJsonObject();originX=d.get("x").getAsInt();originZ=d.get("z").getAsInt();if(Math.abs((long)originX)>100000||Math.abs((long)originZ)>100000)throw new IllegalArgumentException("Bounded search origin required");}}
        catch(Exception e){fail(e);return;}
        report.addProperty("searchOriginX",originX);report.addProperty("searchOriginZ",originZ);
        // Near-origin samples first, then expanding rings. Bounded, actual FULL chunks, no synthetic noise model.
        for(int ring=0;ring<=6;ring++)for(int x=-ring;x<=ring;x++)for(int z=-ring;z<=ring;z++)if(Math.max(Math.abs(x),Math.abs(z))==ring)queue.add(new int[]{originX+x*128,originZ+z*128});
        searchNext();
    }
    private void searchNext(){
        if(queue.isEmpty()){save("pvp-search.json");owner.getLogger().info("PVPAUDIT_SEARCH_DONE samples="+rows.size());return;}
        int[] p=queue.removeFirst();world.getChunkAtAsync(p[0]>>4,p[1]>>4,true).whenComplete((chunk,error)->Bukkit.getScheduler().runTask(owner,()->{
            if(error!=null){fail(error);return;}var row=new JsonObject();int n=0,cover=0,low=999,high=-999,water=0;
            for(int dx:new int[]{2,8,14})for(int dz:new int[]{2,8,14}){int x=p[0]+dx,z=p[1]+dz,y=surface(x,z);Material floor=world.getBlockAt(x,y,z).getType();if(floor==Material.ROOTED_DIRT)n++;low=Math.min(low,y);high=Math.max(high,y);if(world.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING)>y+3)cover++;if(Set.of(Material.WATER,Material.ICE,Material.PACKED_ICE,Material.BLUE_ICE,Material.LAVA).contains(floor))water++;}
            boolean candidate=high-low<=8&&water==0&&cover<=2&&low>=65;
            row.addProperty("x",p[0]+8);row.addProperty("z",p[1]+8);row.addProperty("gentleSoilSamplesOf9",n);row.addProperty("sampleHeightRange",high-low);row.addProperty("canopySamples",cover);row.addProperty("candidate",candidate);rows.add(row);
            if(candidate)owner.getLogger().info("PVPAUDIT_CANDIDATE "+row);
            if(rows.asList().stream().filter(v->v.getAsJsonObject().get("candidate").getAsBoolean()).count()>=4)queue.clear();
            Bukkit.getScheduler().runTask(owner,this::searchNext);
        }));
    }
    void audit(){
        try{var input=JsonParser.parseString(Files.readString(owner.getDataFolder().toPath().resolve("pvp-area.json"))).getAsJsonObject();x0=input.get("xMin").getAsInt();z0=input.get("zMin").getAsInt();if(Math.abs((long)x0)>100000||Math.abs((long)z0)>100000)throw new IllegalArgumentException("Bounded coordinates required");}
        catch(Exception e){fail(e);return;}
        world.setGameRule(GameRules.RANDOM_TICK_SPEED,0);world.setGameRule(GameRules.ADVANCE_WEATHER,false);world.setStorm(false);
        report.addProperty("xMin",x0);report.addProperty("zMin",z0);report.addProperty("size",size);
        for(int x=x0>>4;x<=(x0+size-1)>>4;x++)for(int z=z0>>4;z<=(z0+size-1)>>4;z++)queue.add(new int[]{x,z});loadNext();
    }
    private void loadNext(){
        if(queue.isEmpty()){awaitIdle(0,0);return;}int[] p=queue.removeFirst();world.getChunkAtAsync(p[0],p[1],true).whenComplete((chunk,error)->Bukkit.getScheduler().runTask(owner,()->{
            if(error!=null){fail(error);return;}chunk.addPluginChunkTicket(owner);held.add(chunk);Bukkit.getScheduler().runTask(owner,this::loadNext);
        }));
    }
    private void awaitIdle(int elapsed,int quiet){
        try{var winter=Bukkit.getPluginManager().getPlugin("MidgardWinterWorld");int pending=(int)winter.getClass().getMethod("pendingDecorationCount").invoke(winter);int q=pending==0?quiet+20:0;
            if(q>=60){scan();return;}if(elapsed>=1200)throw new IllegalStateException("Decoration did not become idle");Bukkit.getScheduler().runTaskLater(owner,()->awaitIdle(elapsed+20,q),20);
        }catch(Exception e){fail(e);}
    }
    private int surface(int x,int z){
        int top=world.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES);
        for(int y=top;y>=world.getMinHeight();y--){Material m=world.getBlockAt(x,y,z).getType();if(!GROUND.contains(m))continue;
            if(m==Material.SNOW_BLOCK&&y>world.getMinHeight()&&world.getBlockAt(x,y-1,z).getType().name().endsWith("_LEAVES"))continue;return y;}
        return top;
    }
    private void scan(){
        try{
            int stop=Math.min(size*size,index+1024);
            for(;index<stop;index++){
                int dx=index%size,dz=index/size,x=x0+dx,z=z0+dz,y=surface(x,z);heights[dx][dz]=y;min=Math.min(min,y);max=Math.max(max,y);Material floor=world.getBlockAt(x,y,z).getType();
                if(y>=72&&y<=96&&floor==Material.ROOTED_DIRT)marker++;
                if(Set.of(Material.WATER,Material.LAVA,Material.ICE,Material.PACKED_ICE,Material.BLUE_ICE,Material.POWDER_SNOW).contains(floor))wet++;
                boolean obstructed=false;
                for(int py=y+1;py<=y+3;py++){var block=world.getBlockAt(x,py,z);if(block.getBlockData() instanceof Snow snow){maxSnowLayers=Math.max(maxSnowLayers,snow.getLayers());if(snow.getLayers()>2)obstructed=true;}else if(!block.isPassable())obstructed=true;}
                if(obstructed)obstacles++;
                if(world.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING)>y+3)canopies++;
                for(int py=y-1;py>=y-3;py--)if(!world.getBlockAt(x,py,z).getType().isSolid()){unsupported++;break;}
            }
            if(index<size*size){Bukkit.getScheduler().runTask(owner,this::scan);return;}finish();
        }catch(Exception e){fail(e);}
    }
    private void finish(){
        var levels=new ArrayList<Integer>();var steps=new ArrayList<Integer>();for(int x=0;x<size;x++)for(int z=0;z<size;z++){levels.add(heights[x][z]);if(x+1<size)steps.add(Math.abs(heights[x][z]-heights[x+1][z]));if(z+1<size)steps.add(Math.abs(heights[x][z]-heights[x][z+1]));}Collections.sort(levels);Collections.sort(steps);
        int p10=levels.get(levels.size()/10),p90=levels.get(levels.size()*9/10),p95step=steps.get(steps.size()*95/100);double steep=(double)steps.stream().filter(v->v>2).count()/steps.size();
        report.addProperty("columnsFullyScanned",index);report.addProperty("rootedFieldSoilColumns",marker);report.addProperty("minY",min);report.addProperty("maxY",max);report.addProperty("heightRange",max-min);report.addProperty("p10GroundY",p10);report.addProperty("p90GroundY",p90);report.addProperty("central80PercentHeightRange",p90-p10);report.addProperty("maxAdjacentStep",steps.getLast());report.addProperty("p95AdjacentStep",p95step);report.addProperty("neighborStepsOver2Fraction",steep);report.addProperty("wetOrIceOrPowderColumns",wet);report.addProperty("obstructedColumns",obstacles);report.addProperty("openHeadroomFraction",1-obstacles/(double)index);report.addProperty("unsupportedFirst3Blocks",unsupported);report.addProperty("maxSnowLayers",maxSnowLayers);
        report.addProperty("structuresInLoadedChunks",held.stream().flatMap(c->world.getStructures(c.getX(),c.getZ()).stream()).map(s->s.getStructure().getKey()+":"+s.getBoundingBox()).distinct().count());
        report.addProperty("canopyOrTallCoverColumns",canopies);report.addProperty("canopyOrTallCoverFraction",canopies/(double)index);
        boolean ok=index==size*size&&p90-p10>=1&&p90-p10<=8&&p95step<=1&&steep<=.03&&wet<=index*.02&&obstacles<=index*.12&&unsupported<=index*.05&&canopies<=index*.30;
        report.addProperty("status",ok?"PASS":"FAIL");report.addProperty("scope","Read-only 64x64 natural rolling terrain after decoration idle. All columns inspected; grass/flowers and <=2 snow layers are passable. Trees/ruins/cave entrances are retained, with outliers recorded instead of demanding an empty level arena. No placed, removed or flattened blocks by QA; no all-seed guarantee.");save("pvp-audit.json");release();owner.getLogger().info("PVPAUDIT_DONE "+report);
    }
    private void save(String name){try{Files.createDirectories(owner.getDataFolder().toPath());Files.writeString(owner.getDataFolder().toPath().resolve(name),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){throw new IllegalStateException(e);}}
    private void fail(Throwable error){release();owner.getLogger().log(java.util.logging.Level.SEVERE,"PVPAUDIT_FAILED",error);}
    private void release(){for(var c:held)c.removePluginChunkTicket(owner);held.clear();}
}
