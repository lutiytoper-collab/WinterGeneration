package ru.midgard.winter;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
/** Shared validation of the global switch before a pack can change the world registry. */
final class BootstrapSettings {
    private BootstrapSettings(){}
    static boolean nativePackEnabled(Path path){
        if(!Files.exists(path))return true;
        var yaml=new YamlConfiguration();
        try{yaml.load(path.toFile());}catch(Exception e){throw new IllegalStateException("Cannot read Fantasy bootstrap config",e);}
        for(String key:new String[]{"enabled","fantasy-pack-enabled"})if(yaml.contains(key)&&!yaml.isBoolean(key))throw new IllegalStateException(key+" must be boolean");
        return yaml.getBoolean("enabled",true)&&yaml.getBoolean("fantasy-pack-enabled",true);
    }
    static boolean terrainPackEnabled(Path path){
        if(!nativePackEnabled(path))return false;
        if(!Files.exists(path))return true;
        var yaml=new YamlConfiguration();
        try{yaml.load(path.toFile());}catch(Exception e){throw new IllegalStateException("Cannot read Fantasy terrain config",e);}
        String key="fantasy-terrain-enabled";
        if(yaml.contains(key)&&!yaml.isBoolean(key))throw new IllegalStateException(key+" must be boolean");
        return yaml.getBoolean(key,true);
    }
}
