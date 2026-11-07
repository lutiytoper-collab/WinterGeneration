package ru.midgard.winter.probe;
import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import java.nio.file.*;
import java.util.*;
/** QA ONLY. Public native locate/metadata + actual village floor/door support scans, in a fresh loopback world. */
final class TerrainAudit {
    private final JavaPlugin owner;private final World world;private final JsonObject report=new JsonObject();
    private final JsonArray rows=new JsonArray(),checks=new JsonArray();private final List<Chunk> held=new ArrayList<>();private int index;
    private static final List<String> IDS=List.of("village_plains","village_desert","stronghold","mineshaft","ancient_city","trial_chambers");
    TerrainAudit(JavaPlugin owner){this.owner=owner;world=Objects.requireNonNull(Bukkit.getWorld("winter_test"));}
    void start(){report.addProperty("seed",world.getSeed());report.addProperty("minecraft",Bukkit.getMinecraftVersion());report.add("structures",rows);report.add("checks",checks);next();}
    private void check(String name,boolean ok,String detail){var c=new JsonObject();c.addProperty("name",name);c.addProperty("status",ok?"PASS":"FAIL");c.addProperty("detail",detail);checks.add(c);}
    private void next(){
        release();if(index==IDS.size()){caves();return;}String id=IDS.get(index++);var row=new JsonObject();row.addProperty("type",id);rows.add(row);
        try{
            Location origin=new Location(world,0,90,0);
            if(id.equals("village_desert")){var biome=world.locateNearestBiome(origin,12000,Registry.BIOME.get(NamespacedKey.minecraft("desert")));if(biome==null)throw new IllegalStateException("No desert biome in bounded search");origin=biome.getLocation();}
            var found=world.locateNearestStructure(origin,Registry.STRUCTURE.get(NamespacedKey.minecraft(id)),128,false);
            if(found==null)throw new IllegalStateException("Not found in bounded native search");Location at=found.getLocation();row.addProperty("x",at.getBlockX());row.addProperty("z",at.getBlockZ());
            world.getChunkAtAsync(at,true).whenComplete((chunk,error)->Bukkit.getScheduler().runTask(owner,()->{
                try{
                    if(error!=null)throw new IllegalStateException(error);chunk.addPluginChunkTicket(owner);held.add(chunk);
                    var structure=world.getStructures(chunk.getX(),chunk.getZ()).stream().filter(s->s.getStructure().getKey().getKey().equals(id)).findFirst().orElseThrow();
                    var box=structure.getBoundingBox();row.addProperty("bbox",box.toString());row.addProperty("pieces",structure.getPieces().size());
                    check("native_"+id+"_retained",!structure.getPieces().isEmpty(),"Native structure start and pieces at generated FULL chunk; village blocks scanned separately");
                    if(id.startsWith("village_"))loadVillage(row,structure,box);else Bukkit.getScheduler().runTask(owner,this::next);
                }catch(Exception e){failed(id,e);}
            }));
        }catch(Exception e){failed(id,e);}
    }
    private void failed(String id,Exception error){check(id,false,error.toString());owner.getLogger().warning("TERRAINAUDIT diagnostic "+id+" "+error);save();Bukkit.getScheduler().runTask(owner,this::next);}
    private void loadVillage(JsonObject row,GeneratedStructure structure,BoundingBox box){
        int minx=Math.floorDiv((int)Math.floor(box.getMinX()),16),maxx=Math.floorDiv((int)Math.ceil(box.getMaxX()),16);
        int minz=Math.floorDiv((int)Math.floor(box.getMinZ()),16),maxz=Math.floorDiv((int)Math.ceil(box.getMaxZ()),16);
        var queue=new ArrayDeque<int[]>();for(int x=minx;x<=maxx;x++)for(int z=minz;z<=maxz;z++)queue.add(new int[]{x,z});
        if(queue.size()>625)throw new IllegalStateException("Village exceeds bounded 625-chunk audit");row.addProperty("loadedChunks",queue.size());loadOne(row,structure,queue);
    }
    private void loadOne(JsonObject row,GeneratedStructure structure,ArrayDeque<int[]> queue){
        if(queue.isEmpty()){scanVillage(row,structure);save();Bukkit.getScheduler().runTask(owner,this::next);return;}
        int[] p=queue.removeFirst();world.getChunkAtAsync(p[0],p[1],true).whenComplete((chunk,error)->Bukkit.getScheduler().runTask(owner,()->{
            if(error!=null){failed(row.get("type").getAsString(),new IllegalStateException(error));return;}
            chunk.addPluginChunkTicket(owner);held.add(chunk);loadOne(row,structure,queue);
        }));
    }
    private void scanVillage(JsonObject row,GeneratedStructure structure){
        int doors=0,unsupported=0,footSamples=0,maxGap=0;Set<String> seen=new HashSet<>();
        for(var piece:structure.getPieces()){
            var b=piece.getBoundingBox();int low=Math.max(world.getMinHeight(),(int)Math.floor(b.getMinY())),high=Math.min(world.getMaxHeight()-1,(int)Math.ceil(b.getMaxY()));
            for(int x=(int)Math.floor(b.getMinX());x<=(int)Math.ceil(b.getMaxX());x++)for(int z=(int)Math.floor(b.getMinZ());z<=(int)Math.ceil(b.getMaxZ());z++){
                for(int y=low;y<=high;y++){
                    var block=world.getBlockAt(x,y,z);if(block.getBlockData() instanceof Door d&&d.getHalf()==Bisected.Half.BOTTOM&&seen.add(x+","+y+","+z)){
                        doors++;if(!world.getBlockAt(x,y-1,z).getType().isSolid())unsupported++;
                    }
                }
                // A jigsaw piece's bounding minY can contain air below its first actual floor.
                // Inspect the lowest masonry/plank block in its bottom nine layers instead of assuming minY is a block.
                for(int by=low;by<=Math.min(high,low+8);by++){
                    String material=world.getBlockAt(x,by,z).getType().name();
                    if(material.endsWith("_PLANKS")||Set.of("COBBLESTONE","MOSSY_COBBLESTONE","SANDSTONE","SMOOTH_SANDSTONE","CUT_SANDSTONE").contains(material)){
                        int gap=0;for(int y=by-1;y>=Math.max(world.getMinHeight(),by-16)&&world.getBlockAt(x,y,z).isEmpty();y--)gap++;
                        footSamples++;maxGap=Math.max(maxGap,gap);break;
                    }
                }
            }
        }
        row.addProperty("doors",doors);row.addProperty("unsupportedDoors",unsupported);row.addProperty("foundationSamplesNearPieceBase",footSamples);row.addProperty("foundationSearchBandAbovePieceMinY",8);row.addProperty("maximumAirGapBelowSampledFoundation",maxGap);
        String id=row.get("type").getAsString();check(id+"_door_floors",doors>0&&unsupported==0,"Actual bottom-half doors and solid floor immediately below; doors="+doors+", unsupported="+unsupported);
        check(id+"_foundation_gap",footSamples>0&&maxGap<=8,"Lowest masonry/plank in bottom nine piece layers; maximum consecutive air gap <=8; samples="+footSamples+", max="+maxGap+". Not an all-building traversal proof.");
        if(id.equals("village_desert")){
            int x=row.get("x").getAsInt(),z=row.get("z").getAsInt(),y=world.getHighestBlockYAt(x,z);row.addProperty("surfaceY",y);row.addProperty("surfaceBiome",world.getBiome(x,y,z).getKey().toString());row.addProperty("temperature",world.getTemperature(x,y,z));
            check("desert_climate_kept",world.getTemperature(x,y,z)>.5,"Warm native temperature at actual generated village terrain");
        }
    }
    private void caves(){
        var caves=new JsonArray();report.add("caveBiomes",caves);
        for(String id:List.of("dripstone_caves","lush_caves","deep_dark")){
            var result=world.locateNearestBiome(new Location(world,0,-20,0),8192,Registry.BIOME.get(NamespacedKey.minecraft(id)));var row=new JsonObject();row.addProperty("id",id);
            if(result!=null){var p=result.getLocation();row.addProperty("x",p.getBlockX());row.addProperty("y",p.getBlockY());row.addProperty("z",p.getBlockZ());}caves.add(row);check("cave_biome_"+id,result!=null,"Bounded native 3D biome locate; exact cave void geometry checked independently in saved regions");
        }
        save();owner.getLogger().info("TERRAINAUDIT_DONE checks="+checks.size());release();
    }
    private void save(){try{Files.createDirectories(owner.getDataFolder().toPath());Files.writeString(owner.getDataFolder().toPath().resolve("terrain-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){throw new IllegalStateException(e);}}
    private void release(){for(var c:held)c.removePluginChunkTicket(owner);held.clear();}
}
