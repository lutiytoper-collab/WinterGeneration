package ru.midgard.winter;
import java.lang.reflect.*;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
/** Optional bridge against the public WorldGuard/WorldEdit API. Any API failure denies placement. */
final class WorldGuardBridge {
    private WorldGuardBridge() {}
    static String protection(Plugin plugin,World world,BoundingBox target) {
        try {
            ClassLoader loader=plugin.getClass().getClassLoader();
            Class<?> wg=Class.forName("com.sk89q.worldguard.WorldGuard",true,loader);
            Object instance=wg.getMethod("getInstance").invoke(null);
            Object platform=wg.getMethod("getPlatform").invoke(instance);
            Object container=platform.getClass().getMethod("getRegionContainer").invoke(platform);
            Class<?> adapter=Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter",true,loader);
            Object adapted=adapter.getMethod("adapt",World.class).invoke(null,world);
            Class<?> weWorld=Class.forName("com.sk89q.worldedit.world.World",true,loader);
            Object manager=container.getClass().getMethod("get",weWorld).invoke(container,adapted);
            if(manager==null) return "WorldGuard region manager unavailable";
            Map<?,?> regions=(Map<?,?>)manager.getClass().getMethod("getRegions").invoke(manager);
            if(regions.size()>4096) return "WorldGuard region scan limit";
            for(var entry:regions.entrySet()) {
                if(entry.getKey().equals("__global__")) return "WorldGuard global region (conservative)";
                Object region=entry.getValue();
                Object min=region.getClass().getMethod("getMinimumPoint").invoke(region);
                Object max=region.getClass().getMethod("getMaximumPoint").invoke(region);
                BoundingBox box=new BoundingBox(axis(min,"X"),axis(min,"Y"),axis(min,"Z"),axis(max,"X")+1,axis(max,"Y")+1,axis(max,"Z")+1);
                if(box.overlaps(target)) return "WorldGuard region "+entry.getKey();
            }
            return null;
        } catch(ReflectiveOperationException|LinkageError|RuntimeException ex) {
            return "WorldGuard query unavailable: "+ex.getClass().getSimpleName();
        }
    }
    private static int axis(Object vector,String axis) throws ReflectiveOperationException {
        return ((Number)vector.getClass().getMethod("getBlock"+axis).invoke(vector)).intValue();
    }
}
