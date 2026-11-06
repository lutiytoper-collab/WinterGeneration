package ru.midgard.winter.core;
import java.util.*;
/** Adapter must use loaded chunks only, on the server thread. Never changes terrain. */
public final class AirDropCompatibilityManager {
    public interface Surface {
        boolean loaded(int x,int z);
        int height(int x,int z);
        boolean safeGround(int x,int y,int z);
        boolean air(int x,int y,int z);
        boolean protectedAt(int x,int y,int z);
    }
    public record Result(boolean safe,String reason,int y) {}
    public record Reservation(UUID world,int x,int z,int radius) {
        public Reservation { if(radius<0||radius>512) throw new IllegalArgumentException("radius"); }
        public boolean intersects(UUID w,Box b) {
            return world.equals(w) && (long)b.maxX()>= (long)x-radius && (long)b.minX()<= (long)x+radius
                && (long)b.maxZ()>=(long)z-radius && (long)b.minZ()<=(long)z+radius;
        }
    }
    private final Map<UUID,Reservation> reservations = new HashMap<>();
    public synchronized UUID reserve(UUID world,int x,int z,int radius) {
        UUID token=UUID.randomUUID(); reservations.put(token,new Reservation(world,x,z,radius)); return token;
    }
    public synchronized void release(UUID token) { reservations.remove(token); }
    public synchronized boolean reserved(UUID world,Box b) {
        return reservations.values().stream().anyMatch(r->r.intersects(world,b));
    }
    public Result validate(Surface s,int x,int z,int radius,int headroom,int supportDepth,int maxSlope) {
        if(radius<1||radius>16||headroom<2||headroom>32||supportDepth<1||supportDepth>16||maxSlope<0||maxSlope>2)
            throw new IllegalArgumentException("Unsafe airdrop settings");
        int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
        for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++) {
            int px=Math.addExact(x,dx),pz=Math.addExact(z,dz);
            if(!s.loaded(px,pz)) return new Result(false,"unloaded",0);
            int y=s.height(px,pz); min=Math.min(min,y); max=Math.max(max,y);
            if(s.protectedAt(px,y,pz)) return new Result(false,"protected",y);
            for(int d=0;d<supportDepth;d++) if(!s.safeGround(px,y-d,pz)) return new Result(false,"unsafe_support",y);
            for(int h=1;h<=headroom;h++) if(!s.air(px,y+h,pz)) return new Result(false,"obstructed",y);
        }
        if(max-min>maxSlope) return new Result(false,"slope",max);
        if(s.height(x,z)!=max) return new Result(false,"center_below_platform",max);
        // Check a common headroom plane as well as each local surface.
        for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++)
            for(int y=max+1;y<=max+headroom;y++) if(!s.air(x+dx,y,z+dz)) return new Result(false,"obstructed",max);
        return new Result(true,"safe",max+1);
    }
}
