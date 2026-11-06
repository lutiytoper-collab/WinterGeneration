package ru.midgard.winter.reference;
import java.lang.reflect.*;
import ru.midgard.winter.core.BiomeZoomer;
/** Test-only comparison with the checksum-verified original Minecraft 1.21.11 bytecode. No NMS in plugin. */
public class BiomeZoomReference {
 public static void main(String[] args)throws Exception{
  Class<?> ref=Class.forName("dxq"),source=Class.forName("dxq$a"),holder=Class.forName("jd"),pos=Class.forName("is");
  Method direct=holder.getMethod("a",Object.class),value=holder.getMethod("a"),sample=ref.getMethod("a",pos),hash=ref.getMethod("a",long.class);
  Object callback=Proxy.newProxyInstance(source.getClassLoader(),new Class<?>[]{source},(p,m,a)->direct.invoke(null,a[0]+","+a[1]+","+a[2]));
  int checked=0;
  for(long seed:new long[]{0,42,825180311,-892041,Long.MIN_VALUE,Long.MAX_VALUE}){
   long zoom=BiomeZoomer.seed(seed);if(zoom!=(long)hash.invoke(null,seed))throw new AssertionError("seed hash");
   Object manager=ref.getConstructor(source,long.class).newInstance(callback,zoom);
   for(int x=-500;x<500;x++){
    int y=31+Math.floorMod(x,100),z=-x*17;
    Object p=pos.getConstructor(int.class,int.class,int.class).newInstance(x,y,z);
    String actual=(String)value.invoke(sample.invoke(manager,p));var q=BiomeZoomer.sample(zoom,x,y,z);
    if(!actual.equals(q.x()+","+q.y()+","+q.z()))throw new AssertionError(actual+" != "+q);checked++;
   }
  }
  System.out.println("PASS: "+checked+" biome zoom samples and 6 seed hashes match original Mojang 1.21.11 implementation");
 }
}
