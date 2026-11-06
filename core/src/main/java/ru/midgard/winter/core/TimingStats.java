package ru.midgard.winter.core;
import java.util.Arrays;
/** Bounded rolling samples. Percentiles cover the latest window; count/mean/max cover all observations. */
public final class TimingStats {
    public record Snapshot(long count,double meanMillis,double medianMillis,double p95Millis,double maximumMillis,int samples) {}
    private final long[] samples;private long count,maximum;private double total;
    public TimingStats(int capacity){if(capacity<1||capacity>65536)throw new IllegalArgumentException("capacity");samples=new long[capacity];}
    public synchronized void record(long nanos){if(nanos<0)throw new IllegalArgumentException("negative time");samples[(int)(count%samples.length)]=nanos;count++;total+=nanos;maximum=Math.max(maximum,nanos);}
    public synchronized Snapshot snapshot(){
        int size=(int)Math.min(count,samples.length);if(size==0)return new Snapshot(0,Double.NaN,Double.NaN,Double.NaN,Double.NaN,0);
        long[] sorted=Arrays.copyOf(samples,size);Arrays.sort(sorted);
        double median=size%2==0?(sorted[size/2-1]/2.0+sorted[size/2]/2.0):sorted[size/2];
        return new Snapshot(count,total/count/1e6,median/1e6,sorted[(int)Math.ceil(size*.95)-1]/1e6,maximum/1e6,size);
    }
}
