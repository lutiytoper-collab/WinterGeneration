package ru.midgard.winter.probe;
import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Real server audit. Fixture block writes are explicitly confined to this isolated QA world. */
final class RuntimeAudit {
    private final JavaPlugin owner;private World world;private final JsonObject report=new JsonObject();
    private final JsonArray biomes=new JsonArray(),checks=new JsonArray(),structures=new JsonArray();
    private int structureCursor;
    private static final List<String> STRUCTURES=List.of("village_plains","village_snowy","pillager_outpost","mansion","ruined_portal","stronghold","mineshaft","ancient_city","trial_chambers","shipwreck","ocean_ruin_cold");private int cursor;private final List<Chunk> tickets=new ArrayList<>();
    private static final List<String> BIOMES=List.of("plains","sunflower_plains","forest","flower_forest","birch_forest","old_growth_birch_forest","dark_forest","taiga","old_growth_pine_taiga","old_growth_spruce_taiga","snowy_taiga","snowy_plains","meadow","grove","snowy_slopes","frozen_peaks","jagged_peaks","stony_peaks","windswept_hills","windswept_forest","windswept_gravelly_hills","river","frozen_river","desert","savanna","savanna_plateau","windswept_savanna","jungle","sparse_jungle","bamboo_jungle","badlands","eroded_badlands","wooded_badlands","warm_ocean","lukewarm_ocean","deep_lukewarm_ocean","mangrove_swamp");
    private static final int X=200008,Z=200008,Y=160;
    RuntimeAudit(JavaPlugin owner){this.owner=owner;this.world=Objects.requireNonNull(Bukkit.getWorld("winter_test"));}
    void startFixtures(){report.addProperty("seed",world.getSeed());report.addProperty("minecraft",Bukkit.getMinecraftVersion());prepareFixtureWorld();}
    void start(){report.addProperty("seed",world.getSeed());report.addProperty("minecraft",Bukkit.getMinecraftVersion());nextBiome();}
    private void nextBiome(){
        if(cursor>=BIOMES.size()){nextStructure();return;}
        String id=BIOMES.get(cursor++);Biome biome=Registry.BIOME.get(NamespacedKey.minecraft(id));JsonObject row=new JsonObject();row.addProperty("requested",id);biomes.add(row);
        try{
            var search=world.locateNearestBiome(new Location(world,0,80,0),8192,biome);
            if(search==null){row.addProperty("status","NOT FOUND WITHIN 8192");Bukkit.getScheduler().runTask(owner,this::nextBiome);return;}
            Location l=search.getLocation();
            world.getChunkAtAsync(l,true).thenAccept(chunk->{
                Runnable work=()->{
                    try{
                        int x=l.getBlockX(),y=l.getBlockY(),z=l.getBlockZ();int surface=world.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES);
                        row.addProperty("x",x);row.addProperty("y",y);row.addProperty("z",z);row.addProperty("found",world.getBiome(x,y,z).getKey().toString());row.addProperty("temperature",world.getTemperature(x,y,z));
                        row.addProperty("surfaceY",surface);row.addProperty("surfaceBiome",world.getBiome(x,surface,z).getKey().toString());row.addProperty("surfaceTemperature",world.getTemperature(x,surface,z));
                        row.addProperty("status",world.getBiome(x,y,z).getKey().getKey().equals(id)?"PASS":"FAIL");
                    }catch(Exception ex){row.addProperty("status","FAIL");row.addProperty("error",ex.toString());}
                    Bukkit.getScheduler().runTask(owner,this::nextBiome);
                };if(Bukkit.isPrimaryThread())work.run();else Bukkit.getScheduler().runTask(owner,work);
            });
        }catch(Exception ex){row.addProperty("status","FAIL");row.addProperty("error",ex.toString());Bukkit.getScheduler().runTask(owner,this::nextBiome);}
    }
    private void nextStructure(){
        if(structureCursor>=STRUCTURES.size()){prepareFixtureWorld();return;}
        String id=STRUCTURES.get(structureCursor++);JsonObject row=new JsonObject();row.addProperty("requested",id);structures.add(row);
        try{
            var structure=Registry.STRUCTURE.get(NamespacedKey.minecraft(id));
            var result=world.locateNearestStructure(new Location(world,0,80,0),structure,128,false);
            if(result==null){row.addProperty("status","NOT FOUND IN SEARCH");Bukkit.getScheduler().runTask(owner,this::nextStructure);return;}
            Location l=result.getLocation();row.addProperty("x",l.getBlockX());row.addProperty("y",l.getBlockY());row.addProperty("z",l.getBlockZ());
            world.getChunkAtAsync(l,true).thenAccept(chunk->{Runnable work=()->{
                var boxes=chunk.getStructures().stream().map(g->g.getStructure().getKey()+":"+g.getBoundingBox()).sorted().toList();
                row.add("observedBoxes",new Gson().toJsonTree(boxes));row.addProperty("status",boxes.stream().anyMatch(b->b.startsWith("minecraft:"+id+":"))?"PASS":"LOCATED; API REFERENCE NOT EXPOSED");
                Bukkit.getScheduler().runTask(owner,this::nextStructure);
            };if(Bukkit.isPrimaryThread())work.run();else Bukkit.getScheduler().runTask(owner,work);});
        }catch(Exception ex){row.addProperty("status","FAIL");row.addProperty("error",ex.toString());Bukkit.getScheduler().runTask(owner,this::nextStructure);}
    }
    private void prepareFixtureWorld(){
        try{
            world=Objects.requireNonNull(new WorldCreator("winter_fixture").type(WorldType.FLAT).generateStructures(false).createWorld());
            var winter=Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("MidgardWinterWorld"));
            var config=org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.File(winter.getDataFolder(),"config.yml"));
            var names=new ArrayList<>(config.getStringList("allowed-worlds"));if(!names.contains("winter_fixture"))names.add("winter_fixture");config.set("allowed-worlds",names);config.save(new java.io.File(winter.getDataFolder(),"config.yml"));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"winterworld reload");loadFixture(0);
        }catch(Exception ex){check("fixture_world_creation",false,ex.toString());finish();}
    }
    private void loadFixture(int index){
        if(index==9){Bukkit.getScheduler().runTaskLater(owner,this::fixtures,120);return;}
        int cx=Math.floorDiv(X,16)+index%3-1,cz=Math.floorDiv(Z,16)+index/3-1;
        world.getChunkAtAsync(cx,cz,true).thenAccept(chunk->{Runnable work=()->{chunk.addPluginChunkTicket(owner);tickets.add(chunk);loadFixture(index+1);};if(Bukkit.isPrimaryThread())work.run();else Bukkit.getScheduler().runTask(owner,work);});
    }
    private Object winter;private Method validate;private Object validate()throws Exception{return validate.invoke(winter,world,X,Z);}
    private static boolean safe(Object result)throws Exception{return (boolean)result.getClass().getMethod("safe").invoke(result);}
    private void check(String name,boolean passed,String detail){JsonObject c=new JsonObject();c.addProperty("name",name);c.addProperty("status",passed?"PASS":"FAIL");c.addProperty("detail",detail);checks.add(c);}
    private void fixtures(){
        try{
            winter=Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("MidgardWinterWorld"));validate=winter.getClass().getMethod("checkAirDrop",World.class,int.class,int.class);
            for(int x=X-5;x<=X+5;x++)for(int z=Z-5;z<=Z+5;z++)for(int y=Y-4;y<=Y+10;y++){
                world.getBlockAt(x,y,z).setType(y<=Y?Material.STONE:Material.AIR,false);world.setBiome(x,y,z,Registry.BIOME.get(NamespacedKey.minecraft("plains")));
            }
            Object result=validate();
            var protection=winter.getClass().getDeclaredMethod("protection",Chunk.class,int.class);protection.setAccessible(true);
            check("safe_stone_platform",safe(result),result.toString()+"; protection="+protection.invoke(winter,world.getChunkAt(X>>4,Z>>4),8));
            if(!safe(result))throw new IllegalStateException("Unsafe QA base; abort hazard cases to avoid masked passes");
            for(Material hazard:List.of(Material.WATER,Material.LAVA,Material.POWDER_SNOW,Material.PACKED_ICE)){
                world.getBlockAt(X,Y,Z).setType(hazard,false);result=validate();check("reject_"+hazard,!safe(result),result.toString());world.getBlockAt(X,Y,Z).setType(Material.STONE,false);
            }
            world.getBlockAt(X,Y-2,Z).setType(Material.AIR,false);result=validate();check("cavity_under_surface",!safe(result),result.toString());world.getBlockAt(X,Y-2,Z).setType(Material.STONE,false);
            for(Material obstacle:List.of(Material.SPRUCE_LOG,Material.PACKED_ICE,Material.SPRUCE_PLANKS,Material.SNOW)){
                world.getBlockAt(X,Y+1,Z).setType(obstacle,false);
                if(obstacle==Material.SNOW){var block=world.getBlockAt(X,Y+1,Z);var data=(org.bukkit.block.data.type.Snow)block.getBlockData();data.setLayers(4);block.setBlockData(data,false);}
                result=validate();check("obstruction_"+obstacle,!safe(result),result.toString());world.getBlockAt(X,Y+1,Z).setType(Material.AIR,false);
            }
            world.getBlockAt(X,Y+1,Z).setType(Material.SNOW,false);result=validate();
            check("thin_snow_preserved",safe(result)&&world.getBlockAt(X,Y+1,Z).getType()==Material.SNOW,result.toString());world.getBlockAt(X,Y+1,Z).setType(Material.AIR,false);
            Object token=winter.getClass().getMethod("reserveAirDrop",World.class,int.class,int.class,int.class).invoke(winter,world,X,Z,16);
            result=validate();check("active_reservation_blocks_second_drop",!safe(result),result.toString());winter.getClass().getMethod("releaseAirDrop",UUID.class).invoke(winter,token);
            result=validate();check("release_reservation_restores_safety",safe(result),result.toString());
            generationRegressionFixtures();worldGuard();luna();
        }catch(Exception ex){check("fixture_execution",false,ex.toString());finish();}
    }
    @SuppressWarnings("unchecked") private void generationRegressionFixtures()throws Exception{
        for(int size:new int[]{7,9}){
            var structure=Bukkit.getStructureManager().loadStructure(new NamespacedKey("midgard","moon_gate_"+size),false);
            check("native_moon_gate_"+size,structure!=null&&structure.getSize().getBlockX()==size+2&&structure.getSize().getBlockZ()==3&&structure.getEntities().isEmpty()&&!structure.getPalettes().isEmpty(),"Public StructureManager load from embedded datapack; no placement/entities");
        }
        for(String name:List.of("frost_shrine","woodland_shrine","stone_ruin","timber_ruin")){
            var structure=Bukkit.getStructureManager().loadStructure(new NamespacedKey("midgard",name),false);
            check("native_"+name,structure!=null&&structure.getEntities().isEmpty()&&!structure.getPalettes().isEmpty(),"Original landmark NBT loaded from embedded pack through public API");
        }
        Class<?> keyClass=Arrays.stream(winter.getClass().getDeclaredClasses()).filter(c->c.getSimpleName().equals("ChunkKey")).findFirst().orElseThrow();
        var ctor=keyClass.getDeclaredConstructors()[0];ctor.setAccessible(true);
        var pendingField=winter.getClass().getDeclaredField("pending");pendingField.setAccessible(true);Map<Object,Integer> pending=(Map<Object,Integer>)pendingField.get(winter);
        var loadingField=winter.getClass().getDeclaredField("loading");loadingField.setAccessible(true);Map<Object,Integer> loading=(Map<Object,Integer>)loadingField.get(winter);
        Block moved=world.getBlockAt((X&~15)+15,Y+1,Z),destination=moved.getRelative(org.bukkit.block.BlockFace.EAST),piston=moved.getRelative(org.bukkit.block.BlockFace.WEST);
        var chunks=List.of(moved.getChunk(),destination.getChunk());var marker=new NamespacedKey("midgardwinterworld","processed_v1");
        var old=new ArrayList<Byte>();var keys=new ArrayList<Object>();
        for(Chunk chunk:chunks){Object key=ctor.newInstance(world.getUID(),chunk.getX(),chunk.getZ());if(pending.containsKey(key)||loading.containsKey(key))throw new IllegalStateException("Fixture chunk not idle");keys.add(key);old.add(chunk.getPersistentDataContainer().get(marker,org.bukkit.persistence.PersistentDataType.BYTE));}
        var oldMoved=moved.getBlockData();var oldPiston=piston.getBlockData();
        try{
            for(int n=0;n<2;n++){pending.put(keys.get(n),Bukkit.getCurrentTick());chunks.get(n).getPersistentDataContainer().set(marker,org.bukkit.persistence.PersistentDataType.BYTE,(byte)1);}
            piston.setType(Material.STICKY_PISTON,false);moved.setType(Material.STONE,false);
            Bukkit.getPluginManager().callEvent(new org.bukkit.event.block.BlockPistonRetractEvent(piston,List.of(moved),org.bukkit.block.BlockFace.EAST));
            check("sticky_retract_invalidates_source_and_destination",keys.stream().noneMatch(pending::containsKey)&&chunks.stream().allMatch(c->c.getPersistentDataContainer().getOrDefault(marker,org.bukkit.persistence.PersistentDataType.BYTE,(byte)0)==3),"Public event-dispatch fixture across chunk boundary; not a physical redstone simulation");
            chunks.get(0).getPersistentDataContainer().set(marker,org.bukkit.persistence.PersistentDataType.BYTE,(byte)1);
            Bukkit.getPluginManager().callEvent(new org.bukkit.event.world.ChunkLoadEvent(chunks.get(0),false));
            check("orphan_pending_marker_finalized",chunks.get(0).getPersistentDataContainer().getOrDefault(marker,org.bukkit.persistence.PersistentDataType.BYTE,(byte)0)==3,"Existing pending marker without queued/inflight request becomes skipped; no world blocks changed");
            chunks.get(0).getPersistentDataContainer().set(marker,org.bukkit.persistence.PersistentDataType.BYTE,(byte)1);pending.put(keys.get(0),Bukkit.getCurrentTick());
            Bukkit.getPluginManager().callEvent(new org.bukkit.event.world.ChunkLoadEvent(chunks.get(0),false));
            check("live_pending_marker_retained",chunks.get(0).getPersistentDataContainer().getOrDefault(marker,org.bukkit.persistence.PersistentDataType.BYTE,(byte)0)==1,"A genuinely queued request is not mistaken for an orphan");
        }finally{
            moved.setBlockData(oldMoved,false);piston.setBlockData(oldPiston,false);
            for(int n=0;n<2;n++){pending.remove(keys.get(n));if(old.get(n)==null)chunks.get(n).getPersistentDataContainer().remove(marker);else chunks.get(n).getPersistentDataContainer().set(marker,org.bukkit.persistence.PersistentDataType.BYTE,old.get(n));}
        }
    }
    private void worldGuard()throws Exception{
        Plugin wg=Bukkit.getPluginManager().getPlugin("WorldGuard");if(wg==null){check("WorldGuard_present",false,"not loaded");return;}
        ClassLoader cl=wg.getClass().getClassLoader();Class<?> vector=Class.forName("com.sk89q.worldedit.math.BlockVector3",true,cl);
        Class<?> adapter=Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter",true,cl),weWorld=Class.forName("com.sk89q.worldedit.world.World",true,cl);
        Class<?> api=Class.forName("com.sk89q.worldguard.WorldGuard",true,cl);Object platform=api.getMethod("getPlatform").invoke(api.getMethod("getInstance").invoke(null));
        Object container=platform.getClass().getMethod("getRegionContainer").invoke(platform);Object manager=container.getClass().getMethod("get",weWorld).invoke(container,adapter.getMethod("adapt",World.class).invoke(null,world));
        Class<?> region=Class.forName("com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion",true,cl),base=Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion",true,cl);
        Object min=vector.getMethod("at",int.class,int.class,int.class).invoke(null,X-4,Y-4,Z-4),max=vector.getMethod("at",int.class,int.class,int.class).invoke(null,X+4,Y+10,Z+4);
        Object protectedRegion=region.getConstructor(String.class,vector,vector).newInstance("winter_qa_fixture",min,max);manager.getClass().getMethod("addRegion",base).invoke(manager,protectedRegion);
        Object result=validate();check("real_WorldGuard_region_rejected",!safe(result),result.toString());manager.getClass().getMethod("removeRegion",String.class).invoke(manager,"winter_qa_fixture");
        result=validate();check("WorldGuard_region_removal_restores_safety",safe(result),result.toString());
    }
    private Object drop;
    private void luna()throws Exception{
        Plugin luna=Bukkit.getPluginManager().getPlugin("LunaAirDrop");if(luna==null||!luna.isEnabled()){check("LunaAirDrop_enabled",false,"missing or disabled");finish();return;}
        Map<?,?> drops=(Map<?,?>)luna.getClass().getField("airDrops").get(null);drop=drops.get("default");if(drop==null){check("Luna_default_loaded",false,"no default event");finish();return;}
        Class<?> type=drop.getClass();type.getMethod("setMinPlayersToStart",int.class).invoke(drop,0);type.getMethod("setWorld",World.class).invoke(drop,world);
        Location location=new Location(world,X,Y+1,Z);type.getMethod("setUseStaticLoc",boolean.class).invoke(drop,true);type.getMethod("setUseOnlyStaticLoc",boolean.class).invoke(drop,true);type.getMethod("setStaticLocation",Location.class).invoke(drop,location);
        type.getMethod("setAirDropLocation",Location.class).invoke(drop,location);type.getMethod("setFutureLocation",Location.class).invoke(drop,location);
        world.getBlockAt(X,Y,Z).setType(Material.WATER,false);
        type.getMethod("startCommand",org.bukkit.entity.Player.class).invoke(drop,new Object[]{null});
        Bukkit.getScheduler().runTaskLater(owner,()->{
            try{
                check("real_Luna_unsafe_start_cancelled",!(boolean)type.getMethod("isAirDropStarted").invoke(drop)&&world.getBlockAt(X,Y+1,Z).getType()==Material.AIR,"real public startCommand over WATER; no chest placed");
                world.getBlockAt(X,Y,Z).setType(Material.STONE,false);
                type.getMethod("setAirDropLocation",Location.class).invoke(drop,location);type.getMethod("setFutureLocation",Location.class).invoke(drop,location);
                startLunaLifecycle(type);
            }catch(Exception ex){check("Luna_unsafe_start",false,ex.toString());finish();}
        },20);
    }
    private void startLunaLifecycle(Class<?> type)throws Exception{
        // The real public command entry accepts null to log to console (verified in supplied bytecode).
        type.getMethod("startCommand",org.bukkit.entity.Player.class).invoke(drop,new Object[]{null});
        Bukkit.getScheduler().runTaskLater(owner,()->{
            try{
                Class<?> t=drop.getClass();boolean started=(boolean)t.getMethod("isAirDropStarted").invoke(drop);
                check("real_Luna_start",started,"block="+world.getBlockAt(X,Y+1,Z).getType());
                if(started){t.getMethod("unlock").invoke(drop);check("real_Luna_unlock",!(boolean)t.getMethod("isAirDropLocked").invoke(drop),"public unlock on real event");t.getMethod("End").invoke(drop);check("real_Luna_end",!(boolean)t.getMethod("isAirDropStarted").invoke(drop),"public End on real event");}
            }catch(Exception ex){check("Luna_lifecycle",false,ex.toString());}finally{finish();}
        },60);
    }
    private void finish(){
        report.add("biomes",biomes);report.add("structures",structures);report.add("checks",checks);report.addProperty("fixtureWorld",world.getName());report.addProperty("fixtureCoordinates",X+","+Y+","+Z);report.addProperty("client_inventory_click_tested",false);
        try{Files.createDirectories(owner.getDataFolder().toPath());Files.writeString(owner.getDataFolder().toPath().resolve("runtime-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));owner.getLogger().info("WINTERAUDIT_DONE biomes="+biomes.size()+" checks="+checks.size());}
        catch(Exception ex){owner.getLogger().log(java.util.logging.Level.SEVERE,"WINTERAUDIT_FAILED",ex);}finally{tickets.forEach(c->c.removePluginChunkTicket(owner));tickets.clear();}
    }
}
