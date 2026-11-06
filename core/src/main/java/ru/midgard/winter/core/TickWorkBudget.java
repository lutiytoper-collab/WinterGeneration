package ru.midgard.winter.core;
/** Server-thread budget for actual callbacks, independent of when async requests were issued. */
public final class TickWorkBudget {
    private int tick,started;private long used;private boolean initialized;
    public boolean tryStart(int currentTick,int maxChunks,long softNanos){
        if(maxChunks<1||softNanos<1)throw new IllegalArgumentException("Invalid tick budget");
        if(!initialized||tick!=currentTick){tick=currentTick;started=0;used=0;initialized=true;}
        if(started>=maxChunks||(started>0&&used>=softNanos))return false;
        started++;return true;
    }
    public void record(long nanos){
        if(nanos<0)throw new IllegalArgumentException("Negative elapsed time");
        used=nanos>Long.MAX_VALUE-used?Long.MAX_VALUE:used+nanos;
    }
}
