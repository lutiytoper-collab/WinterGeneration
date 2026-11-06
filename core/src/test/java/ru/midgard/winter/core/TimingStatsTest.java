package ru.midgard.winter.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TimingStatsTest {
 @Test void noInventedMeasurements(){var s=new TimingStats(4).snapshot();assertEquals(0,s.count());assertTrue(Double.isNaN(s.meanMillis()));assertTrue(Double.isNaN(s.p95Millis()));}
 @Test void actualStatistics(){var s=new TimingStats(100);for(int i=1;i<=100;i++)s.record(i*1_000_000L);var r=s.snapshot();assertEquals(100,r.count());assertEquals(50.5,r.meanMillis());assertEquals(50.5,r.medianMillis());assertEquals(95,r.p95Millis());assertEquals(100,r.maximumMillis());}
 @Test void boundedWindow(){var s=new TimingStats(3);for(int i=1;i<=10;i++)s.record(i*1_000_000L);var r=s.snapshot();assertEquals(3,r.samples());assertEquals(10,r.count());assertEquals(9,r.medianMillis());assertEquals(10,r.p95Millis());assertEquals(5.5,r.meanMillis());}
}
