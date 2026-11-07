package ru.midgard.winter;
import org.bukkit.*;
import org.bukkit.block.data.type.Snow;
import org.bukkit.event.*;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.*;
import java.util.*;
/** Public API adapter for the supplied LunaAirDrop 1.0.3-MidgardFix. No binary patches or NMS. */
final class LunaBridge implements Listener {
    private final WinterPlugin winter;
    private final Map<Object,UUID> active=new IdentityHashMap<>();
    private final Map<Object,Preparation> preparations=new IdentityHashMap<>();
    private final Map<Object,Object> originals=new IdentityHashMap<>();private Plugin lunaPlugin;private Class<?> generatorType;
    private final Map<Object,long[]> searches=new IdentityHashMap<>();
    private final Map<Object,Long> pauseUntil=new IdentityHashMap<>();
    private org.bukkit.scheduler.BukkitTask cleanup;
    private int diagnostics;
    private void diagnostic(String text){if(winter.getConfig().getBoolean("debug")&&diagnostics++<12)winter.getLogger().info("Luna search: "+text);}
    private static final class Preparation {
        final Location location;final List<Chunk> tickets=new ArrayList<>();final long started=System.nanoTime();int left=9,positions;boolean valid=true,ready;
        Preparation(Location l,int count){location=l.clone();left=count;}
    }
    LunaBridge(WinterPlugin winter){this.winter=winter;}
    @SuppressWarnings("unchecked") void register(Plugin luna)throws ReflectiveOperationException{
        ClassLoader loader=luna.getClass().getClassLoader();
        Class<? extends Event> start=(Class<? extends Event>)Class.forName("org.by1337.bairdrop.api.event.AirDropStartEvent",true,loader).asSubclass(Event.class);
        Class<? extends Event> end=(Class<? extends Event>)Class.forName("org.by1337.bairdrop.api.event.AirDropEndEvent",true,loader).asSubclass(Event.class);
        var pm=Bukkit.getPluginManager();
        pm.registerEvent(start,this,EventPriority.HIGHEST,(listener,event)->{
            if(!(event instanceof Cancellable c))return;
            try{
                if(!Bukkit.isPrimaryThread()){c.setCancelled(true);return;}
                Object drop=drop(event);Location loc=location(drop);
                if(loc==null||loc.getWorld()==null){c.setCancelled(true);return;}
                if(!winter.isWinterWorld(loc.getWorld()))return;
                if(active.size()>=16&&!active.containsKey(drop)){c.setCancelled(true);return;}
                var result=winter.checkAirDrop(loc.getWorld(),loc.getBlockX(),loc.getBlockZ());
                if(result.safe()&&loc.getBlockY()+1==result.y()){
                    var block=loc.getWorld().getBlockAt(loc.getBlockX(),loc.getBlockY(),loc.getBlockZ());
                    if(block.getType()==Material.SNOW&&((Snow)block.getBlockData()).getLayers()<=2){
                        Location raised=loc.clone();raised.setY(result.y());drop.getClass().getMethod("setAirDropLocation",Location.class).invoke(drop,raised);loc=raised;
                    }
                }
                if(!result.safe()||loc.getBlockY()!=result.y()){
                    c.setCancelled(true);winter.getLogger().info("LunaAirDrop denied: "+result.reason()+"; expected y="+result.y());
                }
            }catch(ReflectiveOperationException|RuntimeException ex){c.setCancelled(true);winter.getLogger().warning("Luna validation failed closed: "+ex.getClass().getSimpleName());}
        },winter,true);
        pm.registerEvent(start,this,EventPriority.MONITOR,(listener,event)->{
            try{
                if(!Bukkit.isPrimaryThread())return;
                Object drop=drop(event);
                if(event instanceof Cancellable c&&c.isCancelled()){release(drop);expire(drop);return;}
                Location loc=location(drop);if(loc==null||loc.getWorld()==null||!winter.isWinterWorld(loc.getWorld()))return;
                UUID previous=active.remove(drop);if(previous!=null)winter.releaseAirDrop(previous);
                active.put(drop,winter.reserveAirDrop(loc.getWorld(),loc.getBlockX(),loc.getBlockZ(),16));
            }catch(ReflectiveOperationException|RuntimeException ex){winter.getLogger().severe("Luna reservation failed: "+ex.getClass().getSimpleName());}
        },winter,false);
        pm.registerEvent(end,this,EventPriority.MONITOR,(listener,event)->{
            try{Object requested=drop(event);release(requested);pauseUntil.put(requested,System.nanoTime()+30_000_000_000L);}
            catch(ReflectiveOperationException ex){winter.getLogger().warning("Luna release failed; reservation retained safely");}
        },winter,true);
        wrapGenerators(luna,loader);
    }
    private void wrapGenerators(Plugin luna,ClassLoader loader)throws ReflectiveOperationException{
        Class<?> generator=Class.forName("org.by1337.bairdrop.locationGenerator.Generator",true,loader);lunaPlugin=luna;generatorType=generator;
        Map<?,?> drops=(Map<?,?>)luna.getClass().getField("airDrops").get(null);
        for(Object drop:drops.values()){
            Object original=drop.getClass().getMethod("getGenerator").invoke(drop);
            // Custom generators keep their own contract. Only replace Luna's default selector.
            if(!original.getClass().getName().equals("org.by1337.bairdrop.locationGenerator.CGenerator"))continue;
            Object proxy=Proxy.newProxyInstance(loader,new Class<?>[]{generator},(p,method,args)->{
                if(method.getDeclaringClass()==Object.class)return method.invoke(original,args);
                if(Bukkit.isPrimaryThread())return invokeGenerator(original,method,args);
                try{return Bukkit.getScheduler().callSyncMethod(winter,()->invokeGenerator(original,method,args)).get(10,java.util.concurrent.TimeUnit.SECONDS);}
                catch(InterruptedException ex){Thread.currentThread().interrupt();return method.getReturnType()==boolean.class?false:null;}
                catch(java.util.concurrent.TimeoutException|java.util.concurrent.ExecutionException|java.util.concurrent.CancellationException ex){
                    if(diagnostics++<12)winter.getLogger().warning("Luna generator deferred/failed: "+(ex.getCause()==null?ex:ex.getCause()));
                    return method.getReturnType()==boolean.class?false:null;
                }
            });
            originals.put(proxy,original);
            drop.getClass().getMethod("setGenerator",generator).invoke(drop,proxy);
        }
    }
    private Object invokeGenerator(Object original,Method method,Object[] args)throws ReflectiveOperationException{
        if(!method.getName().equals("getLocation"))return method.invoke(original,args);
        Object requested=args[0];World world=(World)requested.getClass().getMethod("getWorld").invoke(requested);
        if(world==null)return null;
        if(!winter.isWinterWorld(world))return method.invoke(original,args);
        // Luna's periodic task prefetches unconditionally, including during/just after an active drop.
        if(active.containsKey(requested)||System.nanoTime()<pauseUntil.getOrDefault(requested,0L))return null;
        Preparation state=preparations.get(requested);
        if(state!=null){
            if(!state.valid||System.nanoTime()-state.started>30_000_000_000L){expire(requested);return null;}
            if(!state.ready)return null;
            var loc=state.location;var result=winter.checkAirDrop(world,loc.getBlockX(),loc.getBlockZ());
            if(result.safe()){
                loc.setY(result.y());
                var settings=generatorSettings();String profile=(String)requested.getClass().getMethod("getGeneratorSettings").invoke(requested);
                int maxY=settings.getInt("settings."+profile+".world-NORMAL.max-y",85);
                String support=world.getBlockAt(loc.getBlockX(),result.y()-1,loc.getBlockZ()).getType().name();
                ClassLoader loader=lunaPlugin.getClass().getClassLoader();
                boolean regionEmpty=(boolean)Class.forName("org.by1337.bairdrop.locationGenerator.GeneratorUtils",true,loader)
                    .getMethod("isRegionEmpty",Class.forName("org.by1337.bairdrop.AirDrop",true,loader),Location.class).invoke(null,requested,loc);
                if(result.y()-1<=maxY&&!excludedByLuna(loc)&&!settings.getStringList("black-List").contains(support)&&regionEmpty)return loc.clone();
            }
            diagnostic(result.reason()+" at "+loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ());
            if(++state.positions<16){
                // Reuse the prepared rectangle; one bounded surface check per Luna retry.
                var random=java.util.concurrent.ThreadLocalRandom.current();
                int nextX=Math.floorDiv(loc.getBlockX(),16)*16+2+random.nextInt(12),nextZ=Math.floorDiv(loc.getBlockZ(),16)*16+2+random.nextInt(12);
                int min=(int)requested.getClass().getMethod("getSpawnRadiusMin").invoke(requested),max=(int)requested.getClass().getMethod("getSpawnRadiusMax").invoke(requested);
                var retrySettings=generatorSettings();String retryProfile=(String)requested.getClass().getMethod("getGeneratorSettings").invoke(requested);
                String offsets="settings."+retryProfile+".world-NORMAL.offsets.";
                long offsetX=retrySettings.getInt(offsets+"x"),offsetZ=retrySettings.getInt(offsets+"z");
                if(nextX>=(long)min+offsetX&&nextX<(long)max+offsetX&&nextZ>=(long)min+offsetZ&&nextZ<(long)max+offsetZ){loc.setX(nextX);loc.setZ(nextZ);return null;}
            }
            releasePreparation(requested);return null;
        }
        if(preparations.size()>=16)return null;
        long now=System.nanoTime();long[] budget=searches.computeIfAbsent(requested,k->new long[]{now,0});
        if(now-budget[0]>300_000_000_000L){budget[0]=now;budget[1]=0;}
        if(budget[1]>=32){if(budget[1]++==32)winter.getLogger().warning("Luna safe search reached 32 prepared areas; cooling down until the 5 minute window resets");return null;}
        var settings=generatorSettings();String profile=(String)requested.getClass().getMethod("getGeneratorSettings").invoke(requested);
        String path="settings."+profile+".world-NORMAL.offsets.";
        if(settings.getInt(path+"y",1)!=1){diagnostic("only surface offset y=1 is supported");return null;}
        int min=(int)requested.getClass().getMethod("getSpawnRadiusMin").invoke(requested),max=(int)requested.getClass().getMethod("getSpawnRadiusMax").invoke(requested);
        if(min>=max||min< -29_000_000||max>29_000_000)return null;
        var random=java.util.concurrent.ThreadLocalRandom.current();
        long x=(long)random.nextInt(min,max)+settings.getInt(path+"x"),z=(long)random.nextInt(min,max)+settings.getInt(path+"z");
        if(Math.abs(x)>29_000_000||Math.abs(z)>29_000_000)return null;
        Location candidate=new Location(world,x,world.getSeaLevel(),z);
        if(!world.getWorldBorder().isInside(candidate))return null;
        budget[1]++;prepare(requested,candidate);return null; // Luna's public retry consumes a prepared candidate.
    }
    private org.bukkit.configuration.file.FileConfiguration generatorSettings()throws ReflectiveOperationException{
        Object config=lunaPlugin.getClass().getMethod("getiConfig").invoke(null);
        Class<?> api=Class.forName("org.by1337.bairdrop.configManager.Config",true,lunaPlugin.getClass().getClassLoader());
        return (org.bukkit.configuration.file.FileConfiguration)api.getMethod("getGeneratorSettings").invoke(config);
    }
    private boolean excludedByLuna(Location loc)throws ReflectiveOperationException{
        var settings=generatorSettings();
        String id=loc.getBlock().getBiome().getKey().toString();
        return settings.getStringList("black-List-biome").stream().map(x->x.trim().toLowerCase(Locale.ROOT)).map(x->x.contains(":")?x:"minecraft:"+x).anyMatch(id::equals);
    }
    private void prepare(Object drop,Location candidate){
        World world=candidate.getWorld();int radius=winter.getConfig().getInt("airdrop.platform-radius",2),pad=(winter.getConfig().getInt("structure-margin",8)+15)/16;
        int minX=Math.floorDiv(candidate.getBlockX()-radius,16)-pad,maxX=Math.floorDiv(candidate.getBlockX()+radius,16)+pad;
        int minZ=Math.floorDiv(candidate.getBlockZ()-radius,16)-pad,maxZ=Math.floorDiv(candidate.getBlockZ()+radius,16)+pad;
        Preparation state=new Preparation(candidate,(maxX-minX+1)*(maxZ-minZ+1));preparations.put(drop,state);
        if(cleanup==null)cleanup=Bukkit.getScheduler().runTaskTimer(winter,()->{
            for(Object key:new ArrayList<>(preparations.keySet()))if(!active.containsKey(key)&&System.nanoTime()-preparations.get(key).started>30_000_000_000L)expire(key);
            if(preparations.isEmpty()){cleanup.cancel();cleanup=null;}
        },20,20);
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)world.getChunkAtAsync(x,z,true).whenComplete((chunk,error)->{
            if(!winter.isEnabled())return;
            Bukkit.getScheduler().runTask(winter,()->{
                if(!state.valid)return;
                if(error!=null){releasePreparation(drop);return;}
                if(!winter.acquireChunkLease(state,chunk)){releasePreparation(drop);return;}
                state.tickets.add(chunk);if(--state.left==0)state.ready=true;
            });
        });
    }
    private void releasePreparation(Object drop){Preparation state=preparations.remove(drop);if(state!=null){state.valid=false;winter.releaseChunkLease(state);}}
    private void expire(Object drop){
        releasePreparation(drop);
        try{if(!(boolean)drop.getClass().getMethod("isAirDropStarted").invoke(drop)){
            drop.getClass().getMethod("setFutureLocation",Location.class).invoke(drop,new Object[]{null});
            drop.getClass().getMethod("setAirDropLocation",Location.class).invoke(drop,new Object[]{null});
        }}catch(ReflectiveOperationException ex){winter.getLogger().warning("Luna expired candidate reset failed: "+ex.getClass().getSimpleName());}
    }
    private void release(Object drop){UUID token=active.remove(drop);if(token!=null)winter.releaseAirDrop(token);releasePreparation(drop);}
    void close(){
        new ArrayList<>(active.keySet()).forEach(this::release);new ArrayList<>(preparations.keySet()).forEach(this::releasePreparation);
        if(lunaPlugin!=null)try{
            for(Object drop:((Map<?,?>)lunaPlugin.getClass().getField("airDrops").get(null)).values()){
                Object current=drop.getClass().getMethod("getGenerator").invoke(drop);Object original=originals.get(current);
                if(original!=null)drop.getClass().getMethod("setGenerator",generatorType).invoke(drop,original);
            }
        }catch(ReflectiveOperationException ex){winter.getLogger().warning("Luna generator restore: "+ex.getClass().getSimpleName());}
        if(cleanup!=null){cleanup.cancel();cleanup=null;}searches.clear();pauseUntil.clear();originals.clear();lunaPlugin=null;
    }
    private static Object drop(Event e)throws ReflectiveOperationException{return e.getClass().getMethod("getAirDrop").invoke(e);}
    private static Location location(Object d)throws ReflectiveOperationException{return (Location)d.getClass().getMethod("getAirDropLocation").invoke(d);}
}
