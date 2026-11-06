package ru.midgard.winter;
import java.io.*;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** Real API YAML parsing. These are resource unit tests, not a server emulation. */
class ResourcesTest {
    YamlConfiguration load(String file) throws Exception {
        try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/"+file));var reader=new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)){
            var yaml=new YamlConfiguration();yaml.load(reader);return yaml;
        }
    }
    @Test void safeConfigDefaults() throws Exception {
        var c=load("config.yml");assertTrue(c.getBoolean("new-chunks-only"));assertFalse(c.getBoolean("modify-existing-chunks"));
        assertTrue(c.getBoolean("protect-player-builds"));assertTrue(c.getBoolean("protect-structures"));assertFalse(c.getBoolean("modules.cabins"));
        assertEquals("FAIRYTALE",c.getString("preset"));assertFalse(c.getStringList("allowed-biomes").contains("desert"));
    }
    @Test void everyAdminCommandHasOwnPermission() throws Exception {
        var c=load("plugin.yml");for(String sub:List.of("help","reload","status","info","debug","inspect","benchmark"))assertEquals("op",c.getString("permissions.winterworld."+sub+".default"));
        assertEquals("1.21.11",c.getString("api-version"));assertEquals("ru.midgard.winter.WinterPlugin",c.getString("main"));
    }
    @Test void noMandatoryPluginDependency() throws Exception {assertFalse(load("plugin.yml").contains("depend"));}
    @Test void paperBootstrapAndOptionalBridgesAreDeclared() throws Exception {
        var c=load("paper-plugin.yml");assertEquals("ru.midgard.winter.WinterBootstrap",c.getString("bootstrapper"));
        for(String dependency:List.of("WorldGuard","LunaAirDrop","FastAsyncWorldEdit"))assertFalse(c.getBoolean("dependencies.server."+dependency+".required"));
    }
    @Test void nativePackIsEmbeddedWithExactVersion() throws Exception {
        try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/fantasy-pack/pack.mcmeta"))){
            String meta=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertTrue(meta.contains("0.5.0"));assertTrue(meta.contains("94"));
        }
        assertNotNull(getClass().getResource("/fantasy-pack/data/midgard/worldgen/configured_feature/winter_blossom.json"));
    }
    @Test void newInstallUsesEmbeddedFantasyAndWondersByDefault() throws Exception {
        var c=load("config.yml");assertTrue(c.getBoolean("fantasy-pack-enabled"));assertTrue(c.getBoolean("modules.wonders"));assertFalse(c.getBoolean("modules.forests"));assertTrue(c.getStringList("allowed-worlds").containsAll(List.of("world","winter_test")));
    }
}
