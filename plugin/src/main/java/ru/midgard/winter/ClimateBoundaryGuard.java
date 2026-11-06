package ru.midgard.winter;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.generator.BiomeProvider;
import ru.midgard.winter.core.*;

/** Corrects native climate spill into non-overridden raw biome cells during NEW chunk population only. */
final class ClimateBoundaryGuard {
    private final Map<String,Double> original=new HashMap<>();
    ClimateBoundaryGuard(InputStream stream){
        try(var reader=new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)){
            JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(e->original.put(e.getKey(),e.getValue().getAsDouble()));
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    int repair(Chunk chunk){
        World world=chunk.getWorld();BiomeProvider provider=world.vanillaBiomeProvider();long seed=BiomeZoomer.seed(world.getSeed());
        Map<BiomeZoomer.Quart,String> cache=new HashMap<>();int count=0;
        for(int lx=0;lx<16;lx++)for(int lz=0;lz<16;lz++){
            int x=(chunk.getX()<<4)+lx,z=(chunk.getZ()<<4)+lz,y=world.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING);
            if(world.getBlockAt(x,y,z).getType()==Material.SNOW)y--;
            if(y<world.getMinHeight()||y+1>=world.getMaxHeight())continue;
            Block ground=world.getBlockAt(x,y,z),above=world.getBlockAt(x,y+1,z);
            boolean ice=ground.getType()==Material.ICE&&!BiomePolicy.DEFAULT.contains(ground.getBiome().getKey().getKey());
            boolean snow=above.getType()==Material.SNOW&&!BiomePolicy.DEFAULT.contains(above.getBiome().getKey().getKey());
            if(!ice&&!snow)continue;
            var quart=BiomeZoomer.sample(seed,x,y+1,z);
            String source=cache.computeIfAbsent(quart,q->provider.getBiome(world,q.x()<<2,q.y()<<2,q.z()<<2).getKey().getKey());
            Double temperature=original.get(source);
            if(!BiomePolicy.DEFAULT.contains(source)||temperature==null||!BiomeZoomer.certainlyWarm(temperature,y,world.getSeaLevel()))continue;
            // Native freeze_top_layer samples a zoomed neighbour biome; Bukkit/allowlist uses stored quart cells.
            if(snow){above.setType(Material.AIR,false);count++;}
            if(ice){ground.setType(Material.WATER,false);count++;}
        }
        return count; // At most 512 simple restoration writes; no neighbour chunk loads or retained regions.
    }
}
