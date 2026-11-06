package ru.midgard.winter;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.type.Snow;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import ru.midgard.winter.core.*;
import java.util.*;

/** All world access is synchronous and confined to newly populated, already loaded chunks. */
public final class WinterPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private record Runtime(Settings settings, BiomePolicy biomes, Set<String> worlds,
                           Set<String> modules, double schematicChance, boolean enabled, boolean freeze, boolean log,
                           int reservationRadius,int radius,int headroom,int support,int slope,int maxObjectSize,
                           int pendingLimit,int pendingTicks,int chunksPerTick,int millisPerTick,int shallowSnowLayers) {}
    private volatile Runtime runtime;
    private NamespacedKey marker,climateMarker;
    private final AirDropCompatibilityManager airDrop = new AirDropCompatibilityManager();
    private long seen, decorated, writes, skipped, boundaryRestorations;
    private final Map<String,Long> features=new LinkedHashMap<>();
    private final Map<String,List<String>> featureSamples=new LinkedHashMap<>();
    public Map<String,List<String>> generationSamples(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Server thread required");Map<String,List<String>> copy=new LinkedHashMap<>();featureSamples.forEach((key,value)->copy.put(key,List.copyOf(value)));return Map.copyOf(copy);}
    private void featureAt(String key,int x,int y,int z){feature(key,1);var values=featureSamples.computeIfAbsent(key,k->new ArrayList<>());if(values.size()<32)values.add(x+","+y+","+z);}
    public int pendingDecorationCount(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Server thread required");return pending.size()+loading.size();}
    public Map<String,Long> generationMetrics(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Server thread required");return Map.copyOf(features);}
    private void feature(String name,long n){if(n>0)features.merge(name,n,Long::sum);}
    private ClimateBoundaryGuard boundaryGuard;
    private final TimingStats timings=new TimingStats(4096);
    private final TickWorkBudget actualTickBudget=new TickWorkBudget();
    private boolean debug;
    private LunaBridge lunaBridge;
    private List<DecorationPlan> schematicTemplates=List.of();
    private record ChunkKey(UUID world,int x,int z){}
    private final LinkedHashMap<ChunkKey,Integer> pending=new LinkedHashMap<>();
    private final LeaseRegistry<Object> decorationLeases=new LeaseRegistry<>();
    private final Map<ChunkKey,Chunk> leasedChunks=new HashMap<>();
    private final Map<ChunkKey,Integer> loading=new HashMap<>();
    private org.bukkit.scheduler.BukkitTask pendingTask;
    private static final Set<Material> GROUND = Set.of(Material.GRASS_BLOCK,Material.DIRT,
        Material.COARSE_DIRT,Material.PODZOL,Material.ROOTED_DIRT,Material.STONE,
        Material.ANDESITE,Material.GRANITE,Material.DIORITE,Material.SNOW_BLOCK,Material.MOSS_BLOCK);
    private static final Set<Material> SUPPORT = Set.of(Material.GRASS_BLOCK,Material.DIRT,
        Material.COARSE_DIRT,Material.PODZOL,Material.ROOTED_DIRT,Material.STONE,
        Material.ANDESITE,Material.GRANITE,Material.DIORITE,Material.DEEPSLATE);

    @Override public void onEnable() {
        marker = new NamespacedKey(this,"processed_v1");climateMarker=new NamespacedKey(this,"climate_v1");
        saveDefaultConfig();
        boundaryGuard=new ClimateBoundaryGuard(getResource("original-temperatures.json"));
        try { runtime = readSettings(); debug=getConfig().getBoolean("debug"); }
        catch (RuntimeException ex) { getLogger().severe("Invalid config; disabled safely: "+ex.getMessage());
            getServer().getPluginManager().disablePlugin(this); return; }
        if(!Bukkit.getMinecraftVersion().equals("1.21.11")) {
            getLogger().severe("Only Minecraft 1.21.11 is supported");
            getServer().getPluginManager().disablePlugin(this); return;
        }
        try{
            schematicTemplates=SchematicCatalog.load(this::getResource);
            for(var plan:schematicTemplates)for(var cell:plan.cells())Bukkit.createBlockData(Material.valueOf(cell.block()));
        }catch(Exception e){getLogger().severe("Audited schematics disabled: "+e.getMessage());schematicTemplates=List.of();}
        Bukkit.getPluginManager().registerEvents(this,this);
        hookLuna();
        getLifecycleManager().registerEventHandler(io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS,event->
            event.registrar().register("winterworld","Управление зимней генерацией",(io.papermc.paper.command.brigadier.BasicCommand)(source,args)->onCommand(source.getSender(),null,"winterworld",args)));
        Bukkit.getServicesManager().register(AirDropCompatibilityManager.class,airDrop,this,
            org.bukkit.plugin.ServicePriority.Normal);
        getLogger().info("Fantasy build 0.6.0; embedded native pack; protected late decoration; no retrogen.");
    }
    @Override public void onDisable() {
        if(pendingTask!=null)pendingTask.cancel();pending.clear();loading.clear();for(Object item:decorationLeases.clear()){ChunkKey key=(ChunkKey)item;Chunk ch=leasedChunks.remove(key);if(ch!=null)ch.removePluginChunkTicket(this);}if(lunaBridge!=null)lunaBridge.close();
        Bukkit.getServicesManager().unregisterAll(this);
    }
    private Runtime readSettings() {
        FileConfiguration c=getConfig();
        for(String key:List.of("enabled","new-chunks-only","modify-existing-chunks","protect-player-builds",
                "protect-worldguard-regions","protect-structures","protect-biome-boundaries","logging","debug"))
            if(!c.isBoolean(key)) throw new IllegalArgumentException("Expected boolean: "+key);
        if(!c.getBoolean("new-chunks-only")||c.getBoolean("modify-existing-chunks")||!c.getBoolean("protect-player-builds")
            ||!c.getBoolean("protect-structures")||!c.getBoolean("protect-worldguard-regions")||!c.getBoolean("protect-biome-boundaries"))
            throw new IllegalArgumentException("Safety protections cannot be disabled in this version");
        if(c.contains("overrides")&&!c.isConfigurationSection("overrides")) throw new IllegalArgumentException("overrides must be a map");
        var overrides=c.getConfigurationSection("overrides");
        if(overrides!=null&&!Set.of("snow-density","snow-layers","forest-density","clearing-chance","cabin-chance","ice-chance","light-chance","max-changes").containsAll(overrides.getKeys(false)))
            throw new IllegalArgumentException("Unknown override setting");
        Settings p=Settings.preset(c.getString("preset",""));
        Settings s=new Settings(number(c,"snow-density",p.snowDensity()),integer(c,"snow-layers",p.snowLayers()),
            number(c,"forest-density",p.forestDensity()),number(c,"clearing-chance",p.clearingChance()),
            number(c,"cabin-chance",p.cabinChance()),number(c,"ice-chance",p.iceChance()),
            number(c,"light-chance",p.lightChance()),integer(c,"max-changes",p.maxChanges()),boundedInt(c,"structure-margin",0,32));
        int maxObjectSize=boundedInt(c,"max-object-size",1,11);
        Set<String> modules=new HashSet<>();
        for(String m:List.of("forests","snowdrifts","frozen-water","clearings","cabins","ice","lights")) {
            if(!c.isBoolean("modules."+m)) throw new IllegalArgumentException("Expected boolean modules."+m);
            if(c.getBoolean("modules."+m)) modules.add(m);
        }
        for(String m:List.of("canopy","trails","wonders","schematics")){
            if(c.contains("modules."+m)&&!c.isBoolean("modules."+m))throw new IllegalArgumentException("Expected boolean modules."+m);
            if(c.getBoolean("modules."+m,true))modules.add(m);
        }
        List<String> worlds=c.getStringList("allowed-worlds");
        if(worlds.isEmpty()||worlds.stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Explicit worlds required");
        Set<String> allow=new HashSet<>(c.getStringList("allowed-biomes"));
        // Biome IDs are also checked against the actual server registry, not an invented enum list.
        Set<String> exclude=new HashSet<>(c.getStringList("excluded-biomes"));
        for(String id:allow) if(Registry.BIOME.get(NamespacedKey.minecraft(id))==null)
            throw new IllegalArgumentException("Missing biome "+id);
        for(String id:exclude) if(Registry.BIOME.get(NamespacedKey.minecraft(id))==null)
            throw new IllegalArgumentException("Missing excluded biome "+id);
        return new Runtime(s,new BiomePolicy(allow,exclude),Set.copyOf(worlds),Set.copyOf(modules),
            schematicChance(c),c.getBoolean("enabled"),modules.contains("frozen-water"),c.getBoolean("logging"),
            boundedInt(c,"airdrop.minimum-reservation-radius",1,512),boundedInt(c,"airdrop.platform-radius",1,16),
            boundedInt(c,"airdrop.headroom",2,32),boundedInt(c,"airdrop.support-depth",1,16),
            boundedInt(c,"airdrop.max-slope",0,2),maxObjectSize,
            boundedInt(c,"max-pending-chunks",16,2048),boundedInt(c,"max-pending-ticks",1,200),
            boundedInt(c,"max-chunks-per-tick",1,16),boundedInt(c,"max-millis-per-tick",1,10),boundedInt(c,"airdrop.max-shallow-snow-layers",0,2));
    }
    private static double schematicChance(FileConfiguration c){
        if(c.contains("schematics")&&!c.isConfigurationSection("schematics"))throw new IllegalArgumentException("schematics must be a map");
        Object value=c.get("schematics.chance",.0125);if(!(value instanceof Number n))throw new IllegalArgumentException("schematics.chance must be numeric");
        double p=n.doubleValue();if(!Double.isFinite(p)||p<0||p>1)throw new IllegalArgumentException("schematics.chance must be in [0,1]");return p;
    }
    private static int boundedInt(FileConfiguration c,String k,int min,int max) {
        if(!c.isInt(k)) throw new IllegalArgumentException("Expected integer: "+k);
        int v=c.getInt(k); if(v<min||v>max) throw new IllegalArgumentException("Out of bounds: "+k); return v;
    }
    private static double number(FileConfiguration c,String key,double fallback) {
        String k="overrides."+key;
        if(!c.contains(k)) return fallback;
        Object v=c.get(k); if(!(v instanceof Number n)) throw new IllegalArgumentException("Expected number: "+k);
        return n.doubleValue();
    }
    private static int integer(FileConfiguration c,String key,int fallback) {
        String k="overrides."+key;
        if(!c.contains(k)) return fallback;
        if(!c.isInt(k)) throw new IllegalArgumentException("Expected integer: "+k); return c.getInt(k);
    }
    private boolean allowed(Runtime r,World w,int x,int y,int z) {
        return y>=w.getMinHeight() && y<w.getMaxHeight() && r.biomes.permits(w.getEnvironment().name(),w.getBiome(x,y,z).getKey().toString());
    }
    private boolean worldGuardPresent() { return Bukkit.getPluginManager().getPlugin("WorldGuard")!=null; }

    /** Population only claims a new chunk; decoration runs later under Paper's transient FULL request. */
    @EventHandler(priority=EventPriority.MONITOR)
    public void populate(ChunkPopulateEvent event) {
        if(!Bukkit.isPrimaryThread()){getLogger().warning("Async population skipped safely");return;}
        Chunk ch=event.getChunk();World w=ch.getWorld();if(!isWinterWorld(w)||ch.getPersistentDataContainer().has(marker,PersistentDataType.BYTE))return;
        seen++;ch.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1);
        boundaryRestorations+=boundaryGuard.repair(ch);ch.getPersistentDataContainer().set(climateMarker,PersistentDataType.BYTE,(byte)1);
        if(pending.size()+loading.size()>=runtime.pendingLimit){ch.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)3);skipped++;feature("skip-queue-limit",1);return;}
        pending.put(new ChunkKey(w.getUID(),ch.getX(),ch.getZ()),Bukkit.getCurrentTick());
        if(pendingTask==null)pendingTask=Bukkit.getScheduler().runTaskTimer(this,this::drainPending,1,1);
    }
    // Only Luna's explicit bounded preparation holds plugin tickets; decoration never promotes a halo.
    boolean acquireChunkLease(Object owner,Chunk ch){
        if(!Bukkit.isPrimaryThread()||!ch.getWorld().isChunkLoaded(ch.getX(),ch.getZ()))return false;
        ChunkKey key=new ChunkKey(ch.getWorld().getUID(),ch.getX(),ch.getZ());
        if(!decorationLeases.contains(key)&&decorationLeases.size()>=runtime.pendingLimit*4)return false;
        if(decorationLeases.acquire(owner,key)){ch.addPluginChunkTicket(this);leasedChunks.put(key,ch);}return true;
    }
    void releaseChunkLease(Object owner){for(Object item:decorationLeases.release(owner)){ChunkKey key=(ChunkKey)item;Chunk ch=leasedChunks.remove(key);if(ch!=null)ch.removePluginChunkTicket(this);}}
    private void drainPending(){
        Runtime r=runtime;long begin=System.nanoTime();List<Map.Entry<ChunkKey,Integer>> requests=new ArrayList<>();
        var iterator=pending.entrySet().iterator();
        while(iterator.hasNext()&&requests.size()+loading.size()<r.chunksPerTick){var next=iterator.next();requests.add(Map.entry(next.getKey(),next.getValue()));iterator.remove();}
        int issued=0;
        for(var request:requests){
            ChunkKey key=request.getKey();int tick=request.getValue();World w=Bukkit.getWorld(key.world);
            if(issued>0&&System.nanoTime()-begin>r.millisPerTick*1_000_000L){pending.put(key,tick);continue;}
            if(w==null||Bukkit.getCurrentTick()-tick>r.pendingTicks){markSkippedIfLoaded(key);skipped++;feature("skip-expired",1);continue;}
            loading.put(key,tick);issued++;
            w.getChunkAtAsync(key.x,key.z,false).whenComplete((chunk,error)->{
                if(!isEnabled())return;
                Runnable finish=()->finishDecorationLoad(key,chunk,error);
                if(Bukkit.isPrimaryThread())finish.run();else Bukkit.getScheduler().runTask(this,finish);
            });
        }
        if(pending.isEmpty()&&loading.isEmpty()){pendingTask.cancel();pendingTask=null;}
    }
    private void finishDecorationLoad(ChunkKey key,Chunk ch,Throwable error){
        Integer tick=loading.remove(key);if(tick==null||!isEnabled())return;
        Runtime r=runtime;boolean expired=Bukkit.getCurrentTick()-tick>r.pendingTicks;
        if(ch==null||error!=null||!ch.getWorld().isChunkLoaded(key.x,key.z)){
            if(!expired&&error==null){pending.put(key,tick);return;}skipped++;feature("skip-load",1);return;
        }
        if(ch.getPersistentDataContainer().getOrDefault(marker,PersistentDataType.BYTE,(byte)3)!=1)return;
        if(expired||!isWinterWorld(ch.getWorld())||nearPlayer(ch.getWorld(),key.x,key.z)){
            ch.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)3);skipped++;feature(expired?"skip-expired":"skip-player",1);return;
        }
        if(!actualTickBudget.tryStart(Bukkit.getCurrentTick(),r.chunksPerTick,r.millisPerTick*1_000_000L)){
            pending.put(key,tick);feature("budget-deferred",1);return;
        }
        long start=System.nanoTime(),before=writes;
        try{
            String reason=decorationGate(ch,r.settings.margin());
            if(reason!=null){skipped++;feature("skip-"+reason,1);ch.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)3);return;}
            generate(ch,r);ch.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)2);if(writes>before)decorated++;
        }catch(RuntimeException ex){skipped++;ch.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)3);getLogger().log(java.util.logging.Level.SEVERE,"Decoration aborted; marker retained",ex);}
        finally{long elapsed=System.nanoTime()-start;timings.record(elapsed);actualTickBudget.record(elapsed);}
    }
    private void markSkippedIfLoaded(ChunkKey key){
        World world=Bukkit.getWorld(key.world);if(world==null||!world.isChunkLoaded(key.x,key.z))return;
        var pdc=world.getChunkAt(key.x,key.z).getPersistentDataContainer();
        if(pdc.getOrDefault(marker,PersistentDataType.BYTE,(byte)3)==1)pdc.set(marker,PersistentDataType.BYTE,(byte)3);
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void discardOrphanedMarker(org.bukkit.event.world.ChunkLoadEvent event){
        Chunk chunk=event.getChunk();if(!isWinterWorld(chunk.getWorld()))return;
        var key=new ChunkKey(chunk.getWorld().getUID(),chunk.getX(),chunk.getZ());
        if(!pending.containsKey(key)&&!loading.containsKey(key)&&chunk.getPersistentDataContainer().getOrDefault(marker,PersistentDataType.BYTE,(byte)3)==1)chunk.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)3);
    }
    private static boolean nearPlayer(World w,int cx,int cz){
        return w.getPlayers().stream().anyMatch(p->Math.abs(p.getLocation().getX()-(cx*16+8))<48&&Math.abs(p.getLocation().getZ()-(cz*16+8))<48);
    }
    private void dirty(Block block){
        ChunkKey key=new ChunkKey(block.getWorld().getUID(),Math.floorDiv(block.getX(),16),Math.floorDiv(block.getZ(),16));
        Integer queued=pending.remove(key),active=loading.remove(key);if(queued!=null||active!=null){markSkippedIfLoaded(key);skipped++;}
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void placed(org.bukkit.event.block.BlockPlaceEvent e){dirty(e.getBlock());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void broken(org.bukkit.event.block.BlockBreakEvent e){dirty(e.getBlock());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void bucket(org.bukkit.event.player.PlayerBucketEmptyEvent e){dirty(e.getBlock());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void bucketFill(org.bukkit.event.player.PlayerBucketFillEvent e){dirty(e.getBlock());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void entityExplosion(org.bukkit.event.entity.EntityExplodeEvent e){e.blockList().forEach(this::dirty);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void blockExplosion(org.bukkit.event.block.BlockExplodeEvent e){e.blockList().forEach(this::dirty);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void piston(org.bukkit.event.block.BlockPistonExtendEvent e){e.getBlocks().forEach(this::dirty);e.getBlocks().forEach(b->dirty(b.getRelative(e.getDirection())));}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void pistonRetract(org.bukkit.event.block.BlockPistonRetractEvent e){
        dirty(e.getBlock());
        for(Block block:e.getBlocks()){
            dirty(block);dirty(block.getRelative(e.getDirection()));dirty(block.getRelative(e.getDirection().getOppositeFace()));
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void grow(org.bukkit.event.world.StructureGrowEvent e){e.getBlocks().forEach(b->dirty(b.getBlock()));}
    /** Fail closed if any query chunk is unavailable; getChunkAt is called only after isChunkLoaded. */
    private String protection(Chunk ch,int margin) {
        World w=ch.getWorld(); int x=ch.getX()*16,z=ch.getZ()*16;
        BoundingBox target=new BoundingBox(x,w.getMinHeight(),z,x+16,w.getMaxHeight(),z+16).expand(margin);
        if(worldGuardPresent()) {
            String wg=WorldGuardBridge.protection(Bukkit.getPluginManager().getPlugin("WorldGuard"),w,target);
            if(wg!=null) return wg;
        }
        int minX=Math.floorDiv(x-margin,16),maxX=Math.floorDiv(x+15+margin,16);
        int minZ=Math.floorDiv(z-margin,16),maxZ=Math.floorDiv(z+15+margin,16);
        for(int cx=minX;cx<=maxX;cx++) for(int cz=minZ;cz<=maxZ;cz++) {
            if(!w.isChunkLoaded(cx,cz)) return "structure neighborhood unloaded";
            for(var structure:w.getChunkAt(cx,cz).getStructures())
                if(structure.getBoundingBox().overlaps(target)) return "vanilla structure with margin";
        }
        // Also protect unregistered features with block entities anywhere in the new chunk.
        if(ch.getTileEntities().length!=0) return "block entities in chunk";
        return null;
    }
    @EventHandler public void pluginEnabled(org.bukkit.event.server.PluginEnableEvent e) {
        if(e.getPlugin().getName().equals("LunaAirDrop")) hookLuna();
    }
    private void hookLuna() {
        var luna=Bukkit.getPluginManager().getPlugin("LunaAirDrop");
        if(lunaBridge!=null||luna==null||!luna.isEnabled()) return;
        try { LunaBridge bridge=new LunaBridge(this); bridge.register(luna); lunaBridge=bridge;
            getLogger().info("LunaAirDrop public event gate registered; runtime compatibility still UNVERIFIED"); }
        catch(ReflectiveOperationException|LinkageError ex) { getLogger().severe("Luna bridge unavailable: "+ex.getClass().getSimpleName()); }
    }
    private String decorationGate(Chunk ch,int margin){
        World w=ch.getWorld();int x=ch.getX()*16,z=ch.getZ()*16;
        if(worldGuardPresent()){
            String reason=WorldGuardBridge.protection(Bukkit.getPluginManager().getPlugin("WorldGuard"),w,new BoundingBox(x,w.getMinHeight(),z,x+16,w.getMaxHeight(),z+16).expand(margin));
            if(reason!=null)return reason;
        }
        if(ch.getTileEntities().length!=0)return "block entities in chunk";
        return null;
    }
    private List<BoundingBox> decorationObstacles(Chunk ch,int margin){
        World w=ch.getWorld();int pad=(margin+15)/16;Map<String,BoundingBox> boxes=new HashMap<>();
        for(int cx=ch.getX()-pad;cx<=ch.getX()+pad;cx++)for(int cz=ch.getZ()-pad;cz<=ch.getZ()+pad;cz++)for(var structure:w.getStructures(cx,cz)){
            var b=structure.getBoundingBox();String id=structure.getStructure().getKey().getKey();
            // Surface structures retain full-height protection, including vanilla's stale shipwreck Y.
            boolean deep=StructureProtection.canUseVerticalBounds(id);
            var protectedBox=deep?b.clone():new BoundingBox(b.getMinX(),w.getMinHeight(),b.getMinZ(),b.getMaxX(),w.getMaxHeight(),b.getMaxZ());
            protectedBox.expand(margin);boxes.put(id+protectedBox,protectedBox);
        }
        return List.copyOf(boxes.values());
    }
    private static boolean intersects(Box box,List<BoundingBox> areas){
        BoundingBox actual=new BoundingBox(box.minX(),box.minY(),box.minZ(),box.maxX()+1,box.maxY()+1,box.maxZ()+1);
        return areas.stream().anyMatch(b->b.overlaps(actual));
    }
    private final class DecorationSite implements ForestPlacementValidator.Site {
        final World w;final Runtime r;final List<BoundingBox> obstacles;final Map<Long,Integer> heights=new HashMap<>();
        DecorationSite(World w,Runtime r,List<BoundingBox> obstacles){this.w=w;this.r=r;this.obstacles=obstacles;}
        public int minHeight(){return w.getMinHeight();}public int maxHeight(){return w.getMaxHeight();}
        public boolean loaded(int x,int z){return w.isChunkLoaded(Math.floorDiv(x,16),Math.floorDiv(z,16));}
        public int surface(int x,int z){return heights.computeIfAbsent(((long)x<<32)^(z&0xffffffffL),k->forestFloor(w,x,z));}
        public boolean naturalGround(int x,int y,int z){return GROUND.contains(w.getBlockAt(x,y,z).getType());}
        public boolean naturalSupport(int x,int y,int z){return SUPPORT.contains(w.getBlockAt(x,y,z).getType());}
        public boolean allowedBiome(int x,int y,int z){return allowed(r,w,x,y,z);}
        public String material(int x,int y,int z){return w.getBlockAt(x,y,z).getType().name();}
        public boolean replaceable(int x,int y,int z){Material m=w.getBlockAt(x,y,z).getType();return m==Material.AIR||thinSnow(w.getBlockAt(x,y,z))||SMALL_PLANTS.contains(m);}
        public boolean protectedBox(Box box){return airDrop.reserved(w.getUID(),box)||intersects(box,obstacles);}
    }
    private static final Set<Material> SOFT=Set.of(Material.SHORT_GRASS,Material.TALL_GRASS,Material.FERN,Material.LARGE_FERN);
    private static final Set<Material> SMALL_PLANTS=Set.of(Material.SHORT_GRASS,Material.FERN);
    private static int forestFloor(World w,int x,int z){
        int top=w.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES);
        for(int y=top;y>=Math.max(w.getMinHeight(),top-48);y--){
            Material m=w.getBlockAt(x,y,z).getType();
            if(m==Material.SNOW_BLOCK&&y>w.getMinHeight()&&w.getBlockAt(x,y-1,z).getType().name().endsWith("_LEAVES"))continue;
            if(m==Material.AIR||m==Material.SNOW||SOFT.contains(m)||m.name().endsWith("_LOG")||m.name().endsWith("_LEAVES"))continue;
            return y;
        }
        return top;
    }
    private void generate(Chunk ch,Runtime r) {
        World w=ch.getWorld();long seed=w.getSeed();int ox=ch.getX()*16,oz=ch.getZ()*16;
        int columnOffset=(int)Noise.hash(seed,ch.getX(),ch.getZ(),700)&255;
        Settings settings=r.settings;DecorationSite site=new DecorationSite(w,r,decorationObstacles(ch,settings.margin()));int used=0;
        int x=ox+8,z=oz+8,y=site.surface(x,z);String biome=w.getBiome(x,y,z).getKey().getKey();
        boolean light=r.modules.contains("lights")&&Noise.unit(seed,ch.getX(),ch.getZ(),203)<settings.lightChance();
        List<DecorationPlan> candidates=new ArrayList<>();
        if(r.modules.contains("schematics")&&!schematicTemplates.isEmpty()&&Candidates.rare(seed,ch.getX(),ch.getZ(),8,950,r.schematicChance)){
            var pool=SchematicCatalog.forBiome(schematicTemplates,biome);
            int variant=(int)(Noise.unit(seed,ch.getX(),ch.getZ(),951)*pool.size());int turn=(int)(Noise.unit(seed,ch.getX(),ch.getZ(),952)*4);
            candidates.add(pool.get(variant).rotated(turn));
        }
        if(r.modules.contains("cabins")&&Candidates.rare(seed,ch.getX(),ch.getZ(),8,201,settings.cabinChance()))candidates.add(DecorationPlan.cabin(light));
        if(r.modules.contains("clearings")&&Candidates.rare(seed,ch.getX(),ch.getZ(),4,202,settings.clearingChance()))candidates.add(DecorationPlan.clearing(light));
        // Offset the mineral grid: clearings must not consume every crystal candidate.
        if(r.modules.contains("ice")&&Candidates.rare(seed,ch.getX()+1,ch.getZ()+1,4,204,settings.iceChance()))candidates.add(DecorationPlan.ice(seed,ch.getX(),ch.getZ()));
        if(r.modules.contains("wonders")&&Noise.unit(seed,ch.getX(),ch.getZ(),884)<.38)candidates.add(FantasyPlan.choose(seed,ch.getX(),ch.getZ(),biome.contains("birch")));
        objectLoop:for(DecorationPlan special:candidates){
            Box shape=special.bounds(0,0,0);int minX=Math.max(1,-shape.minX()),maxX=Math.min(14,15-shape.maxX()),minZ=Math.max(1,-shape.minZ()),maxZ=Math.min(14,15-shape.maxZ());
            for(int attempt=0;attempt<10;attempt++){
                int px=ox+minX+(int)(Noise.unit(seed,ch.getX(),ch.getZ(),410+attempt)*(maxX-minX+1)),pz=oz+minZ+(int)(Noise.unit(seed,ch.getX(),ch.getZ(),430+attempt)*(maxZ-minZ+1)),py=site.surface(px,pz);
                if(!allowed(r,w,px,py,pz))continue;
                if(special.module().equals("cabins")){
                    var valid=PlacementValidator.validate(special,site,px,py,pz,ch.getX(),ch.getZ(),settings.maxChanges(),r.maxObjectSize);
                    if(valid.allowed()){used+=writePlan(w,special,special.cells(),px,py,pz);featureAt(special.module()+"-objects",px,py,pz);break objectLoop;}
                }else{
                    DecorationPlan prop=special;
                    if(prop.module().equals("clearings")||prop.module().equals("ice")||prop.module().equals("moonstones")||prop.module().startsWith("moon-gate-")||prop.module().endsWith("-shrine")||prop.module().endsWith("-ruin")){
                        var fitted=prop.module().equals("clearings")?FoundationFitter.fit(prop,site,px,pz,2):FoundationFitter.fitMineral(prop,site,px,pz,2);
                        if(fitted==null){feature("denied-"+prop.module()+"-steep",1);continue;}prop=fitted.plan();py=fitted.ground();
                    }
                    var valid=ForestPlacementValidator.validateProp(prop,site,px,py,pz,ch.getX(),ch.getZ(),settings.maxChanges(),r.maxObjectSize);
                    if(valid.allowed()){used+=writePlan(w,prop,valid.writes(),px,py,pz);featureAt(prop.module()+"-objects",px,py,pz);break objectLoop;}else feature("denied-"+prop.module()+"-"+valid.reason(),1);
                }
            }
        }
        if(used==0&&r.modules.contains("forests")&&Noise.unit(seed,ch.getX(),ch.getZ(),205)<settings.forestDensity()){
            for(int attempt=0;attempt<10;attempt++){
                int px=ox+5+(int)(Noise.unit(seed,ch.getX(),ch.getZ(),450+attempt)*6),pz=oz+5+(int)(Noise.unit(seed,ch.getX(),ch.getZ(),470+attempt)*6),py=site.surface(px,pz);
                String actual=w.getBiome(px,py,pz).getKey().getKey();
                if(!isForest(actual)||!allowed(r,w,px,py,pz)||(r.modules.contains("trails")&&WinterLandscape.trailShoulder(seed,px,pz)))continue;
                DecorationPlan tree=isConifer(actual)?DecorationPlan.conifer(seed,px,pz):DecorationPlan.deciduous(seed,px,pz,actual.contains("birch"));
                var valid=ForestPlacementValidator.validate(tree,site,px,py,pz,ch.getX(),ch.getZ(),settings.maxChanges(),r.maxObjectSize);
                if(valid.allowed()){used+=writePlan(w,tree,valid.writes(),px,py,pz);featureAt(isConifer(actual)?"snow-conifers":"snow-deciduous",px,py,pz);break;}
            }
        }
        // Small natural details occupy gaps without competing with a full tree's budget.
        if(r.modules.contains("forests")&&settings.forestDensity()>.1&&used<settings.maxChanges()-40)for(int attempt=0;attempt<2;attempt++){
            int px=ox+3+(int)(Noise.unit(seed,ch.getX(),ch.getZ(),490+attempt)*10),pz=oz+3+(int)(Noise.unit(seed,ch.getX(),ch.getZ(),500+attempt)*10),py=site.surface(px,pz);
            String actual=w.getBiome(px,py,pz).getKey().getKey();if(!isForest(actual)||WinterLandscape.trailShoulder(seed,px,pz))continue;
            DecorationPlan small=Noise.unit(seed,px,pz,501)<.25?DecorationPlan.fallenLog(actual.contains("birch")):DecorationPlan.shrub(actual.contains("birch"));
            var valid=ForestPlacementValidator.validateProp(small,site,px,py,pz,ch.getX(),ch.getZ(),settings.maxChanges()-used,r.maxObjectSize);
            if(valid.allowed()){used+=writePlan(w,small,valid.writes(),px,py,pz);feature("understory",1);}
        }
        if(r.modules.contains("trails"))for(int dx=0;dx<16&&used<settings.maxChanges()-1;dx++)for(int dz=0;dz<16&&used<settings.maxChanges()-1;dz++){
            int px=ox+dx,pz=oz+dz,py=site.surface(px,pz);
            if(!WinterLandscape.trail(seed,px,pz)||!landColumn(site,px,py,pz)||site.protectedBox(new Box(px,py,pz,px,py+2,pz)))continue;
            Material ground=w.getBlockAt(px,py,pz).getType();Block top=w.getBlockAt(px,py+1,pz);
            if(!Set.of(Material.GRASS_BLOCK,Material.DIRT,Material.PODZOL,Material.COARSE_DIRT).contains(ground)||!(top.getType()==Material.AIR||thinSnow(top)||SMALL_PLANTS.contains(top.getType())))continue;
            if((site.loaded(px-1,pz)&&Math.abs(site.surface(px-1,pz)-py)>1)||(site.loaded(px+1,pz)&&Math.abs(site.surface(px+1,pz)-py)>1)||(site.loaded(px,pz-1)&&Math.abs(site.surface(px,pz-1)-py)>1)||(site.loaded(px,pz+1)&&Math.abs(site.surface(px,pz+1)-py)>1))continue;
            if(top.getType()!=Material.AIR){top.setType(Material.AIR,false);used++;}
            w.getBlockAt(px,py,pz).setType(Noise.unit(seed,px,pz,512)<.25?Material.PODZOL:Material.COARSE_DIRT,false);used++;feature("trail-blocks",1);
        }
        // A bounded share goes to existing crowns, making the entire forest visibly wintry.
        int canopyBudget=Math.min(settings.maxChanges()-used,Math.max(24,settings.maxChanges()/5));
        if(r.modules.contains("canopy"))for(int index=0;index<256&&canopyBudget>0;index++){
            int column=(index*73+columnOffset)&255,px=ox+(column&15),pz=oz+(column>>>4),top=w.getHighestBlockYAt(px,pz,HeightMap.MOTION_BLOCKING);
            if(w.getBlockAt(px,top,pz).getType()==Material.SNOW||w.getBlockAt(px,top,pz).getType()==Material.SNOW_BLOCK)top--;
            Block leaf=w.getBlockAt(px,top,pz),snow=w.getBlockAt(px,top+1,pz);
            if(!leaf.getType().name().endsWith("_LEAVES")||!(snow.getType()==Material.AIR||thinSnow(snow))||!allowed(r,w,px,top+1,pz)||site.protectedBox(new Box(px,top,pz,px,top+1,pz)))continue;
            int layers=2+(int)(Noise.smooth(seed^612,px,pz,6)*4);apply(w,px,top+1,pz,Material.SNOW,layers);used++;canopyBudget--;feature("canopy-snow",1);
        }
        if(r.modules.contains("snowdrifts")||r.freeze)for(int index=0;index<256&&used<settings.maxChanges();index++){
            int column=(index*73+columnOffset)&255,px=ox+(column&15),pz=oz+(column>>>4),py=site.surface(px,pz);
            if(!landColumn(site,px,py,pz)||site.protectedBox(new Box(px,py,pz,px,py+2,pz)))continue;
            Block ground=w.getBlockAt(px,py,pz),above=w.getBlockAt(px,py+1,pz);
            if(r.modules.contains("snowdrifts")&&GROUND.contains(ground.getType())&&(above.getType()==Material.AIR||thinSnow(above)||SOFT.contains(above.getType()))
                    &&site.naturalSupport(px,py-1,pz)&&site.naturalSupport(px,py-2,pz)&&Noise.unit(seed,px,pz,210)<settings.snowDensity()
                    &&!(r.modules.contains("trails")&&WinterLandscape.trail(seed,px,pz))){
                if(above.getType()==Material.TALL_GRASS||above.getType()==Material.LARGE_FERN)continue;
                int layers=WinterLandscape.driftLayers(seed,px,pz,settings.snowLayers());
                if(thinSnow(above))layers=Math.max(layers,((Snow)above.getBlockData()).getLayers());
                apply(w,px,py+1,pz,Material.SNOW,layers);used++;feature("drift-blocks",1);
            }else if(r.freeze&&ground.getType()==Material.WATER&&above.getType()==Material.AIR&&Set.of("river","frozen_river").contains(w.getBiome(px,py,pz).getKey().getKey())
                    &&ground.getBlockData() instanceof org.bukkit.block.data.Levelled level&&level.getLevel()==0){ground.setType(Material.ICE,false);used++;feature("river-ice",1);}
        }
        writes+=used;
    }
    private boolean landColumn(DecorationSite site,int x,int y,int z){return y-2>=site.minHeight()&&y+2<site.maxHeight()&&site.allowedBiome(x,y,z)&&site.allowedBiome(x,y+1,z)&&site.allowedBiome(x,y+2,z);}
    private int writePlan(World w,DecorationPlan plan,List<DecorationPlan.Cell> cells,int x,int ground,int z){
        for(var cell:cells){
            apply(w,x+cell.x(),ground+cell.y(),z+cell.z(),Material.valueOf(cell.block()),cell.layers());
            if(plan.module().equals("fallen-logs")&&cell.block().endsWith("_LOG")){
                var block=w.getBlockAt(x+cell.x(),ground+cell.y(),z+cell.z());var data=(org.bukkit.block.data.Orientable)block.getBlockData();data.setAxis(Axis.X);block.setBlockData(data,false);
            }

        }
        return cells.size();
    }
    private static boolean thinSnow(Block b){return b.getType()==Material.SNOW&&((Snow)b.getBlockData()).getLayers()<=6;}
    private static int surface(World w,int x,int z) {
        int y=w.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES);
        return thinSnow(w.getBlockAt(x,y,z))?y-1:y;
    }
    private static boolean isConifer(String b){return b.contains("taiga")||b.equals("grove");}
    private static boolean isForest(String b){return isConifer(b)||b.contains("forest");}
    private static void apply(World w,int x,int y,int z,Material material,int layers) {
        Block b=w.getBlockAt(x,y,z);b.setType(material,false);
        if(material==Material.SNOW){Snow data=(Snow)b.getBlockData();data.setLayers(layers);b.setBlockData(data,false);}
        if(material.name().endsWith("_LEAVES")){
            var data=(org.bukkit.block.data.type.Leaves)b.getBlockData();data.setPersistent(true);b.setBlockData(data,false);
        }
    }

    /** Validate and reserve atomically on the main thread; LunaAirDrop must call before choosing spawn. */
    public AirDropCompatibilityManager.Result checkAirDrop(World w,int x,int z) {
        if(!Bukkit.isPrimaryThread()) throw new IllegalStateException("Server thread required");
        Runtime r=runtime;
        Map<Long,Boolean> protectionCache=new HashMap<>();Map<Long,Integer> heights=new HashMap<>();
        return airDrop.validate(new AirDropCompatibilityManager.Surface() {
            public boolean loaded(int px,int pz) { return w.isChunkLoaded(Math.floorDiv(px,16),Math.floorDiv(pz,16)); }
            public int height(int px,int pz) {
                long key=((long)px<<32)^(pz&0xffffffffL);
                return heights.computeIfAbsent(key,k->{int py=w.getHighestBlockYAt(px,pz,HeightMap.MOTION_BLOCKING_NO_LEAVES);
                    return py+1<w.getMaxHeight()&&w.getBlockAt(px,py+1,pz).getType()==Material.SNOW?py+1:py;});
            }
            public boolean safeGround(int px,int py,int pz) {
                if(py<w.getMinHeight()||py>=w.getMaxHeight())return false;
                Block b=w.getBlockAt(px,py,pz);
                return SUPPORT.contains(b.getType())||(py==height(px,pz)&&b.getType()==Material.SNOW&&((Snow)b.getBlockData()).getLayers()<=r.shallowSnowLayers);
            }
            public boolean air(int px,int py,int pz) { return py>=w.getMinHeight()&&py<w.getMaxHeight()&&w.getBlockAt(px,py,pz).getType()==Material.AIR; }
            public boolean protectedAt(int px,int py,int pz) {
                if(!w.getWorldBorder().isInside(new Location(w,px,py,pz))||airDrop.reserved(w.getUID(),new Box(px,py,pz,px,py+r.headroom,pz))) return true;
                int cx=Math.floorDiv(px,16),cz=Math.floorDiv(pz,16);long key=((long)cx<<32)^(cz&0xffffffffL);
                return protectionCache.computeIfAbsent(key,k->protection(w.getChunkAt(cx,cz),r.settings.margin())!=null);
            }
        },x,z,r.radius,r.headroom,r.support,r.slope);
    }
    public boolean isWinterWorld(World w){return runtime.enabled&&runtime.worlds.contains(w.getName())&&w.getEnvironment()==World.Environment.NORMAL;}
    public UUID reserveAirDrop(World w,int x,int z,int radius) {
        if(!Bukkit.isPrimaryThread()) throw new IllegalStateException("Server thread required");
        if(!checkAirDrop(w,x,z).safe()) throw new IllegalArgumentException("Unsafe airdrop site");
        return airDrop.reserve(w.getUID(),x,z,Math.max(radius,runtime.reservationRadius));
    }
    public void releaseAirDrop(UUID token) { airDrop.release(token); }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        String sub=args.length==0?"help":args[0].toLowerCase(Locale.ROOT);
        if(!Set.of("help","reload","status","info","debug","inspect","benchmark").contains(sub)) { sender.sendMessage("Неизвестная подкоманда"); return true; }
        if(!sender.hasPermission("winterworld."+sub)) { sender.sendMessage("Нет разрешения winterworld."+sub); return true; }
        switch(sub) {
            case "help" -> sender.sendMessage("/winterworld help|reload|status|info|debug|inspect|benchmark");
            case "reload" -> {
                Runtime previous=runtime;
                try { reloadConfig(); Runtime next=readSettings(); runtime=next; debug=getConfig().getBoolean("debug"); sender.sendMessage("Настройки применены атомарно; только новые чанки"); }
                catch(RuntimeException ex) { runtime=previous; sender.sendMessage("Отклонено; прежние настройки сохранены: "+ex.getMessage()); }
            }
            case "debug" -> { debug=!debug; sender.sendMessage("Debug="+debug); }
            case "status", "info" -> sender.sendMessage("MidgardWinterWorld 0.6.0 DEVELOPMENT; enabled="+runtime.enabled+
                "; worlds="+runtime.worlds+"; processed="+seen+" decorated="+decorated+" skipped="+skipped+" boundary-restored="+boundaryRestorations+" pending="+(pending.size()+loading.size())+
                "; leased-chunks="+decorationLeases.size()+"; features="+features+"; WorldGuard="+(worldGuardPresent()?"public API bridge, fail closed on query failure":"absent")+"; LunaAirDrop=UNVERIFIED");
            case "inspect" -> {
                if(!(sender instanceof Player p)) { sender.sendMessage("Требуется игрок"); break; }
                Location l=p.getLocation(); World w=p.getWorld(); Chunk ch=p.getChunk();
                boolean permitted=runtime.worlds.contains(w.getName())&&allowed(runtime,w,l.getBlockX(),l.getBlockY(),l.getBlockZ());
                sender.sendMessage("Biome="+p.getLocation().getBlock().getBiome().getKey()+" allowed="+permitted+
                    "; reason="+(permitted?"allowlist":"world/dimension/biome denied")+"; protection="+protection(ch,runtime.settings.margin())+
                    "; marker="+ch.getPersistentDataContainer().get(marker,PersistentDataType.BYTE)+"; LunaAirDrop=UNVERIFIED");
            }
            case "benchmark" -> {
                var t=timings.snapshot();
                sender.sendMessage("Реальные callback-наблюдения: n="+t.count()+" samples="+t.samples()+
                    " avg-ms="+t.meanMillis()+" median-ms="+t.medianMillis()+" p95-ms="+t.p95Millis()+
                    " max-ms="+t.maximumMillis()+" writes="+writes+"; полная генерация/vanilla baseline NOT MEASURED");
            }
        }
        return true;
    }
}
