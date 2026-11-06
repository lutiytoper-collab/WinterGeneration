package ru.midgard.winter.probe;
import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
/** TEST ONLY: serial real chunk loads, preserved-geometry fingerprints and actual load request timings. */
public final class WorldgenProbe extends JavaPlugin implements CommandExecutor {
    private int quietTicks,idleWaitTicks;
    private boolean running;private JsonArray samples;private World world;private int side,startX,startZ,index;private long heapStart;
    private record Sample(Chunk chunk,long nanos,boolean existed){}
    private final List<Sample> loaded=new ArrayList<>();private int snapshotIndex;
    @Override public void onEnable(){Objects.requireNonNull(getCommand("winterprobe")).setExecutor(this);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(sender instanceof org.bukkit.command.ConsoleCommandSender&&args.length==1&&args[0].equals("visuals")){
            try{
                var winter=Bukkit.getPluginManager().getPlugin("MidgardWinterWorld");JsonObject data=new JsonObject();data.addProperty("seed",Bukkit.getWorld("winter_test").getSeed());data.addProperty("version",winter.getPluginMeta().getVersion());
                data.add("metrics",new Gson().toJsonTree(winter.getClass().getMethod("generationMetrics").invoke(winter)));data.add("samples",new Gson().toJsonTree(winter.getClass().getMethod("generationSamples").invoke(winter)));
                Files.createDirectories(getDataFolder().toPath());Files.writeString(getDataFolder().toPath().resolve("visual-metrics.json"),new GsonBuilder().setPrettyPrinting().create().toJson(data));getLogger().info("WINTERVISUAL_METRICS "+data);
            }catch(Exception ex){getLogger().log(java.util.logging.Level.SEVERE,"WINTERVISUAL_FAILED",ex);}return true;
        }
        if(sender instanceof org.bukkit.command.ConsoleCommandSender&&args.length==1&&args[0].startsWith("natural")){
            try{switch(args[0]){case "natural"->new NaturalLunaAudit(this).start();case "natural-teleport"->NaturalLunaAudit.teleport();case "natural-end"->NaturalLunaAudit.current.verifyEnd();default->throw new IllegalArgumentException();}}
            catch(Exception ex){getLogger().log(java.util.logging.Level.SEVERE,"NATURALLUNA_FAILED",ex);}return true;
        }
        if(sender instanceof org.bukkit.command.ConsoleCommandSender&&args.length==1&&args[0].equals("terrain-audit")){new TerrainAudit(this).start();return true;}
        if(sender instanceof org.bukkit.command.ConsoleCommandSender&&args.length==1&&args[0].equals("fixtures")){
            new RuntimeAudit(this).startFixtures();return true;
        }
        if(sender instanceof org.bukkit.command.ConsoleCommandSender&&args.length==1&&args[0].equals("verify")){
            try{new BiomeVerify(this).next();}catch(Exception ex){getLogger().log(java.util.logging.Level.SEVERE,"BIOMEVERIFY_FAILED",ex);}return true;
        }
        if(sender instanceof org.bukkit.command.ConsoleCommandSender&&args.length==1&&args[0].equals("teleport")){
            var player=Bukkit.getPlayerExact("WinterQA");if(player!=null)player.teleport(new Location(Bukkit.getWorld("winter_fixture"),200011,161,200008));return true;
        }
        if(sender instanceof org.bukkit.command.ConsoleCommandSender&&args.length==1&&args[0].equals("audit")){
            new RuntimeAudit(this).start();return true;
        }
        if(!(sender instanceof org.bukkit.command.ConsoleCommandSender)||args.length!=4||!args[0].equals("run")||running){sender.sendMessage("Console: winterprobe run <side 1..16> <chunkX> <chunkZ>");return true;}
        try{side=Integer.parseInt(args[1]);startX=Integer.parseInt(args[2]);startZ=Integer.parseInt(args[3]);if(side<1||side>16||Math.abs((long)startX)>100000||Math.abs((long)startZ)>100000)throw new IllegalArgumentException();}
        catch(IllegalArgumentException e){sender.sendMessage("Invalid bounded area");return true;}
        world=Bukkit.getWorld("winter_test");if(world==null){sender.sendMessage("winter_test required");return true;}
        world.setGameRule(GameRules.RANDOM_TICK_SPEED,0);world.setGameRule(GameRules.ADVANCE_TIME,false);world.setGameRule(GameRules.ADVANCE_WEATHER,false);
        world.setStorm(false);world.setThundering(false);world.setTime(6000);
        samples=new JsonArray();loaded.clear();index=0;snapshotIndex=0;running=true;heapStart=usedHeap();next();return true;
    }
    private void next(){
        if(index>=side*side){quietTicks=0;idleWaitTicks=0;Bukkit.getScheduler().runTaskLater(this,this::awaitDecorationIdle,20);return;}
        int x=startX+index%side,z=startZ+index/side;
        boolean existed=world.isChunkGenerated(x,z);long begin=System.nanoTime();
        world.getChunkAtAsync(x,z,true).whenComplete((chunk,error)->{
            long elapsed=System.nanoTime()-begin;
            Runnable complete=()->{
                if(error!=null){running=false;getLogger().severe("WINTERPROBE_FAILED "+error);return;}
                try{chunk.addPluginChunkTicket(this);loaded.add(new Sample(chunk,elapsed,existed));index++;Bukkit.getScheduler().runTask(this,this::next);}
                catch(Exception ex){running=false;getLogger().log(java.util.logging.Level.SEVERE,"WINTERPROBE_FAILED",ex);}
            };
            if(Bukkit.isPrimaryThread())complete.run();else Bukkit.getScheduler().runTask(this,complete);
        });
    }
    private void awaitDecorationIdle(){
        try{
            var winter=Bukkit.getPluginManager().getPlugin("MidgardWinterWorld");int pending=0;
            if(winter!=null)try{pending=(int)winter.getClass().getMethod("pendingDecorationCount").invoke(winter);}catch(NoSuchMethodException legacy){pending=0;}
            idleWaitTicks+=20;quietTicks=pending==0?quietTicks+20:0;
            if(quietTicks>=40){collect();return;}
            if(idleWaitTicks>=800)throw new IllegalStateException("Decoration did not become idle; pending="+pending);
            Bukkit.getScheduler().runTaskLater(this,this::awaitDecorationIdle,20);
        }catch(Exception ex){running=false;release();getLogger().log(java.util.logging.Level.SEVERE,"WINTERPROBE_FAILED",ex);}
    }
    private void collect(){
        if(snapshotIndex>=loaded.size()){finish();return;}
        Sample s=loaded.get(snapshotIndex++);
        try{samples.add(snapshot(s.chunk,s.nanos,s.existed));Bukkit.getScheduler().runTask(this,this::collect);}
        catch(Exception ex){running=false;release();getLogger().log(java.util.logging.Level.SEVERE,"WINTERPROBE_FAILED",ex);}
    }
    private void release(){loaded.forEach(s->s.chunk.removePluginChunkTicket(this));loaded.clear();}
    @Override public void onDisable(){release();}
    private JsonObject snapshot(Chunk chunk,long nanos,boolean existed)throws NoSuchAlgorithmException{
        JsonObject item=new JsonObject();item.addProperty("x",chunk.getX());item.addProperty("z",chunk.getZ());item.addProperty("existed",existed);item.addProperty("requestMillis",nanos/1e6);
        var rawGeometry=MessageDigest.getInstance("SHA-256");var geometry=MessageDigest.getInstance("SHA-256");Map<String,Integer> undergroundMaterials=new TreeMap<>();Map<String,MessageDigest> surfaces=new TreeMap<>();int snow=0;
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            int wx=chunk.getX()*16+x,wz=chunk.getZ()*16+z,y=world.getHighestBlockYAt(wx,wz,HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if(chunk.getBlock(x,y,z).getType()==Material.SNOW)y--;
            String biome=chunk.getBlock(x,y,z).getBiome().getKey().toString();
            MessageDigest surface=surfaces.computeIfAbsent(biome,k->{try{return MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new IllegalStateException(e);}});
            for(int py=world.getMinHeight();py<=Math.min(31,y-8);py++){
                Block block=chunk.getBlock(x,py,z);String pos=x+","+py+","+z+":";
                rawGeometry.update((pos+block.getBlockData().getAsString()+";").getBytes(StandardCharsets.UTF_8));
                geometry.update((pos+block.getType().name()+";").getBytes(StandardCharsets.UTF_8));
                undergroundMaterials.merge(block.getType().name(),1,Integer::sum);
            }
            for(int py=Math.max(world.getMinHeight(),y-2);py<=Math.min(world.getMaxHeight()-1,y+12);py++){
                Block b=chunk.getBlock(x,py,z);if(b.getType()==Material.SNOW)snow++;
                String material=b.getType().name();if(b.getBlockData() instanceof org.bukkit.block.data.type.Snow data)material+="[layers="+data.getLayers()+"]";
                surface.update((x+","+py+","+z+":"+material+";").getBytes(StandardCharsets.UTF_8));
            }
        }
        item.add("undergroundMaterials",new Gson().toJsonTree(undergroundMaterials));item.addProperty("rawUndergroundBlockStateHash",HexFormat.of().formatHex(rawGeometry.digest()));
        item.addProperty("belowSurfaceGeometry",HexFormat.of().formatHex(geometry.digest()));item.addProperty("snowBlocks",snow);
        JsonObject hashes=new JsonObject();surfaces.forEach((k,v)->hashes.addProperty(k,HexFormat.of().formatHex(v.digest())));item.add("surfaceByBiome",hashes);
        List<String> structures=chunk.getStructures().stream().map(s->s.getStructure().getKey()+":"+s.getBoundingBox()).sorted().toList();
        item.add("structures",new Gson().toJsonTree(structures));
        List<String> entities=Arrays.stream(chunk.getTileEntities()).map(s->s.getX()+","+s.getY()+","+s.getZ()+":"+s.getType()).sorted().toList();item.add("blockEntities",new Gson().toJsonTree(entities));
        Byte marker=chunk.getPersistentDataContainer().get(new NamespacedKey("midgardwinterworld","processed_v1"),PersistentDataType.BYTE);if(marker!=null)item.addProperty("winterMarker",marker);
        return item;
    }
    private void finish(){
        JsonObject report=new JsonObject();report.addProperty("seed",world.getSeed());report.addProperty("minecraft",Bukkit.getMinecraftVersion());report.addProperty("server",Bukkit.getVersion());
        report.addProperty("winterPluginEnabled",Bukkit.getPluginManager().isPluginEnabled("MidgardWinterWorld"));
        report.add("enabledDatapacks",new Gson().toJsonTree(Bukkit.getServer().getDatapackManager().getEnabledPacks().stream().map(p->p.getName()).sorted().toList()));
        report.addProperty("measurement","serial getChunkAtAsync request-to-completion latency; not isolated generation CPU time");report.addProperty("heapStart",heapStart);report.addProperty("heapEnd",usedHeap());report.add("chunks",samples);
        try{Files.createDirectories(getDataFolder().toPath());Files.writeString(getDataFolder().toPath().resolve("measurement.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));getLogger().info("WINTERPROBE_DONE chunks="+samples.size());}
        catch(Exception e){getLogger().log(java.util.logging.Level.SEVERE,"WINTERPROBE_FAILED",e);}finally{running=false;release();}
    }
    private long usedHeap(){return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();}
}
