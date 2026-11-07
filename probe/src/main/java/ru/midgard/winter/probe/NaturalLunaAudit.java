package ru.midgard.winter.probe;
import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
import java.util.*;
final class NaturalLunaAudit implements Listener {
    private final JavaPlugin owner;private final JsonObject report=new JsonObject();private Object drop;private int ticks;private long started;private String belowBefore;private Location chosen;private org.bukkit.scheduler.BukkitTask timer;
    static NaturalLunaAudit current;
    NaturalLunaAudit(JavaPlugin owner){this.owner=owner;current=this;}
    @SuppressWarnings("unchecked") void start()throws Exception{
        started=System.nanoTime();owner.getLogger().info("NATURALLUNA_BEGIN");
        Plugin luna=Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("LunaAirDrop"));World world=Objects.requireNonNull(Bukkit.getWorld("winter_test"));world.setStorm(false);world.setThundering(false);world.setGameRule(GameRules.RANDOM_TICK_SPEED,0);world.setGameRule(GameRules.ADVANCE_WEATHER,false);
        drop=((Map<?,?>)luna.getClass().getField("airDrops").get(null)).get("default");Class<?> t=drop.getClass();
        int radius=1024;Path areaFile=owner.getDataFolder().toPath().resolve("natural-luna-area.json");
        if(Files.exists(areaFile)){var area=JsonParser.parseString(Files.readString(areaFile)).getAsJsonObject();radius=area.get("spawnRadius").getAsInt();if(radius<16||radius>4096)throw new IllegalArgumentException("Bounded QA radius required");report.add("naturalArea",area);}
        t.getMethod("setWorld",World.class).invoke(drop,world);t.getMethod("setUseStaticLoc",boolean.class).invoke(drop,false);t.getMethod("setUseOnlyStaticLoc",boolean.class).invoke(drop,false);t.getMethod("setFlatnessCheck",boolean.class).invoke(drop,false);t.getMethod("setMinPlayersToStart",int.class).invoke(drop,0);t.getMethod("setSpawnRadiusMin",int.class).invoke(drop,-radius);t.getMethod("setSpawnRadiusMax",int.class).invoke(drop,radius);
        t.getMethod("setAirDropLocation",Location.class).invoke(drop,new Object[]{null});t.getMethod("setFutureLocation",Location.class).invoke(drop,new Object[]{null});
        report.addProperty("platformRadius",((JavaPlugin)Bukkit.getPluginManager().getPlugin("MidgardWinterWorld")).getConfig().getInt("airdrop.platform-radius"));
        report.addProperty("generator",t.getMethod("getGenerator").invoke(drop).getClass().getName());report.addProperty("mode","public Generator adapter: bounded random coordinates and async preparation over natural winter_test; real Luna lifecycle");
        Class<? extends Event> start=(Class<? extends Event>)Class.forName("org.by1337.bairdrop.api.event.AirDropStartEvent",true,luna.getClass().getClassLoader()).asSubclass(Event.class);
        Bukkit.getPluginManager().registerEvent(start,this,EventPriority.MONITOR,(listener,event)->{
            try{if(event instanceof Cancellable c&&c.isCancelled())return;Object actual=event.getClass().getMethod("getAirDrop").invoke(event);if(actual!=drop)return;
                chosen=((Location)drop.getClass().getMethod("getAirDropLocation").invoke(drop)).clone();belowBefore=chosen.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getBlockData().getAsString();
            }catch(Exception ex){owner.getLogger().warning(ex.toString());}
        },owner,true);
        t.getMethod("startCommand",org.bukkit.entity.Player.class).invoke(drop,new Object[]{null});timer=Bukkit.getScheduler().runTaskTimer(owner,this::poll,20,20);
    }
    private void poll(){
        try{
            ticks++;
            if((boolean)drop.getClass().getMethod("isAirDropStarted").invoke(drop)){
                timer.cancel();if(chosen==null)throw new IllegalStateException("No real StartEvent observed");
                if(!belowBefore.startsWith("minecraft:snow[")&&!belowBefore.equals("minecraft:snow_block"))throw new IllegalStateException("Snow-surface scenario selected "+belowBefore);
                drop.getClass().getMethod("unlock").invoke(drop);
                report.addProperty("status","PASS");report.addProperty("elapsedSeconds",(System.nanoTime()-started)/1e9);report.addProperty("world",chosen.getWorld().getName());report.addProperty("x",chosen.getBlockX());report.addProperty("y",chosen.getBlockY());report.addProperty("z",chosen.getBlockZ());report.addProperty("biome",chosen.getBlock().getBiome().getKey().toString());report.addProperty("belowBefore",belowBefore);report.addProperty("belowAfter",chosen.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getBlockData().getAsString());
                if(!Objects.equals(belowBefore,report.get("belowAfter").getAsString()))throw new IllegalStateException("Surface changed");save();owner.getLogger().info("NATURALLUNA_READY");HandlerList.unregisterAll(this);return;
            }
            if(System.nanoTime()-started>240_000_000_000L)throw new IllegalStateException("Natural search timed out");
        }catch(Exception ex){if(timer!=null)timer.cancel();report.addProperty("status","FAIL");report.addProperty("error",ex.toString());try{drop.getClass().getMethod("End").invoke(drop);save();}catch(Exception ignored){}owner.getLogger().severe("NATURALLUNA_FAILED "+ex);HandlerList.unregisterAll(this);}
    }
    static void teleport(){if(current!=null&&current.chosen!=null){var player=Bukkit.getPlayerExact("WinterQA");if(player!=null)player.teleport(current.chosen.clone().add(2.5,0,.5));}}
    void verifyEnd()throws Exception{
        Plugin winter=Bukkit.getPluginManager().getPlugin("MidgardWinterWorld");int tickets=chosen.getWorld().getPluginChunkTickets().getOrDefault(winter,List.of()).size();
        report.addProperty("ticketCountAfterEnd",tickets);report.addProperty("surfacePreservedAfterEnd",belowBefore.equals(chosen.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getBlockData().getAsString()));report.addProperty("blockAfterEnd",chosen.getBlock().getType().name());
        report.addProperty("ticketCoordinates",chosen.getWorld().getPluginChunkTickets().getOrDefault(winter,List.of()).stream().map(c->c.getX()+","+c.getZ()).toList().toString());
        var bridgeField=winter.getClass().getDeclaredField("lunaBridge");bridgeField.setAccessible(true);Object bridge=bridgeField.get(winter);
        for(String name:List.of("active","preparations")){
            var field=bridge.getClass().getDeclaredField(name);field.setAccessible(true);var map=(Map<?,?>)field.get(bridge);report.addProperty(name+"AfterEnd",map.size());
        }
        save();
        if(tickets!=0||!report.get("surfacePreservedAfterEnd").getAsBoolean()||chosen.getBlock().getType()!=Material.AIR){report.addProperty("status","FAIL");save();throw new IllegalStateException("Cleanup invariant failed: "+report);}
        owner.getLogger().info("NATURALLUNA_COMPLETE");
    }
    private void save()throws Exception{Files.writeString(owner.getDataFolder().toPath().resolve("natural-luna.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}
}
