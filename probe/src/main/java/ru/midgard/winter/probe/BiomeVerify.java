package ru.midgard.winter.probe;
import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
final class BiomeVerify {
    private final JavaPlugin owner;private final World world;private final JsonArray positions,results=new JsonArray();private int index;
    BiomeVerify(JavaPlugin p)throws Exception{owner=p;world=Bukkit.getWorld("winter_test");positions=JsonParser.parseString(Files.readString(p.getDataFolder().toPath().resolve("verify-positions.json"))).getAsJsonArray();}
    void next(){
        if(index>=positions.size()){
            try{Files.writeString(owner.getDataFolder().toPath().resolve("biome-verification.json"),new GsonBuilder().setPrettyPrinting().create().toJson(results));owner.getLogger().info("BIOMEVERIFY_DONE positions="+results.size());}catch(Exception e){throw new IllegalStateException(e);}return;
        }
        JsonObject row=positions.get(index++).getAsJsonObject();int x=row.get("x").getAsInt(),y=row.get("y").getAsInt(),z=row.get("z").getAsInt();
        world.getChunkAtAsync(x>>4,z>>4,true).thenAccept(c->{Runnable work=()->{
            row.addProperty("effectiveBiome",world.getBiome(x,y,z).getKey().toString());row.addProperty("temperature",world.getTemperature(x,y,z));row.addProperty("actualMaterial",world.getBlockAt(x,y,z).getType().name());results.add(row);Bukkit.getScheduler().runTask(owner,this::next);
        };if(Bukkit.isPrimaryThread())work.run();else Bukkit.getScheduler().runTask(owner,work);});
    }
}
