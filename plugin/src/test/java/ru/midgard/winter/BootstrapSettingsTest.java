package ru.midgard.winter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class BootstrapSettingsTest {
    @TempDir Path dir;
    boolean read(String text)throws Exception{Path p=dir.resolve("config.yml");Files.writeString(p,text);return BootstrapSettings.nativePackEnabled(p);}
    @Test void emptyNewInstallEnablesNativePack(){assertTrue(BootstrapSettings.nativePackEnabled(dir.resolve("absent.yml")));}
    @Test void globalDisableAlsoDisablesNativeRegistryChanges()throws Exception{assertFalse(read("enabled: false\nfantasy-pack-enabled: true\n"));}
    @Test void nativeSwitchStillWorksIndependently()throws Exception{assertFalse(read("enabled: true\nfantasy-pack-enabled: false\n"));}
    @Test void existingConfigWithoutNewKeyKeepsCompatibility()throws Exception{assertTrue(read("enabled: true\n"));}
    @Test void malformedSwitchFailsBeforeWorldLoad(){assertThrows(IllegalStateException.class,()->read("enabled: please\n"));}
}
