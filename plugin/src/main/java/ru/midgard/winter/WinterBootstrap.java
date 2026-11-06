package ru.midgard.winter;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.datapack.Datapack;
import net.kyori.adventure.text.Component;
import java.util.Objects;

/** Public Paper bootstrap: bundled worldgen is available before the first world is generated. */
@SuppressWarnings("UnstableApiUsage")
public final class WinterBootstrap implements PluginBootstrap {
    @Override public void bootstrap(BootstrapContext context) {
        var config=context.getDataDirectory().resolve("config.yml");
        if(!BootstrapSettings.nativePackEnabled(config)){
            context.getLogger().info("Embedded Fantasy pack disabled by configuration");return;
        }
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY,event->{
            try{
                var uri=Objects.requireNonNull(WinterBootstrap.class.getResource("/fantasy-pack"),"Missing bundled Fantasy worldgen").toURI();
                var pack=event.registrar().discoverPack(uri,"fantasy",options->options
                    .title(Component.text("Midgard Fantasy 0.5.0"))
                    .autoEnableOnServerStart(true).position(true,Datapack.Position.TOP));
                if(pack==null)throw new IllegalStateException("Paper rejected embedded Fantasy worldgen");
                context.getLogger().info("FANTASY_PACK_DISCOVERED "+pack.getName());
            }catch(Exception e){throw new IllegalStateException("Cannot register embedded Fantasy pack",e);}
        });
    }
}
