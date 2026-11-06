package ru.midgard.winter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;
/** Static contract checks of the user's actual JAR. No claims of runtime integration. */
class LunaContractTest {
    Path jar(){String p=System.getProperty("luna.jar");Assumptions.assumeTrue(p!=null&&Files.isRegularFile(Path.of(p)),"Supply -PlunaJar=/path/to/LunaAirDrop.jar");return Path.of(p);}
    ClassNode read(JarFile jar,String name)throws Exception{
        try(var in=jar.getInputStream(jar.getJarEntry(name+".class"))){ClassNode node=new ClassNode();new ClassReader(in).accept(node,0);return node;}
    }
    @Test void publicCancellableStartAndEndEvents()throws Exception{
        try(var j=new JarFile(jar().toFile())){
            for(String event:new String[]{"AirDropStartEvent","AirDropEndEvent"}){
                var c=read(j,"org/by1337/bairdrop/api/event/"+event);assertTrue(c.interfaces.contains("org/bukkit/event/Cancellable"));
                assertTrue(c.methods.stream().anyMatch(m->m.name.equals("getAirDrop")&&(m.access&Opcodes.ACC_PUBLIC)!=0));
            }
        }
    }
    @Test void cancellationIsCheckedBeforeRegionOrBlockMutation()throws Exception{
        try(var j=new JarFile(jar().toFile())){
            var cls=read(j,"org/by1337/bairdrop/CAirDrop");var method=cls.methods.stream().filter(m->m.name.equals("start")&&m.desc.equals("()V")).findFirst().orElseThrow();
            int event=-1,cancel=-1,mutation=Integer.MAX_VALUE,index=0;boolean returnAfterCancel=false;
            for(var insn:method.instructions){
                if(insn instanceof MethodInsnNode m){
                    if(m.owner.endsWith("AirDropStartEvent")&&m.name.equals("<init>"))event=index;
                    if(m.owner.endsWith("AirDropStartEvent")&&m.name.equals("isCancelled"))cancel=index;
                    if(m.name.equals("SetRegion")||(m.owner.equals("org/bukkit/block/Block")&&m.name.equals("setType")))mutation=Math.min(mutation,index);
                }
                if(cancel>=0&&index>cancel&&index<mutation&&insn.getOpcode()==Opcodes.RETURN)returnAfterCancel=true;
                index++;
            }
            assertTrue(event>=0&&cancel>event&&mutation>cancel&&mutation<Integer.MAX_VALUE);assertTrue(returnAfterCancel);
        }
    }
}
