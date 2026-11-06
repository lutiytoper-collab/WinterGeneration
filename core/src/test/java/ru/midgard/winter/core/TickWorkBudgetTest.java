package ru.midgard.winter.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TickWorkBudgetTest {
    @Test void delayedCallbacksAreLimitedWhenTheyActuallyRun(){var b=new TickWorkBudget();assertTrue(b.tryStart(12,4,2_000_000));b.record(3_000_000);assertFalse(b.tryStart(12,4,2_000_000));assertFalse(b.tryStart(12,4,2_000_000));assertTrue(b.tryStart(13,4,2_000_000));}
    @Test void aSingleSlowChunkDoesNotPreventNextTickProgress(){var b=new TickWorkBudget();assertTrue(b.tryStart(1,4,1));b.record(999999);assertFalse(b.tryStart(1,4,1));assertTrue(b.tryStart(2,4,1));}
    @Test void chunkCountCapsCheapAndSynchronouslyCompletedWork(){var b=new TickWorkBudget();for(int i=0;i<4;i++){assertTrue(b.tryStart(1,4,999));b.record(0);}assertFalse(b.tryStart(1,4,999));assertTrue(b.tryStart(2,4,999));}
    @Test void equalityAtTheSoftLimitDefersFurtherWork(){var b=new TickWorkBudget();assertTrue(b.tryStart(1,4,100));b.record(100);assertFalse(b.tryStart(1,4,100));}
    @Test void budgetDoesNotOverflow(){var b=new TickWorkBudget();assertTrue(b.tryStart(1,4,2));b.record(Long.MAX_VALUE);b.record(5);assertFalse(b.tryStart(1,4,2));}
    @Test void invalidLimitsAreRejected(){var b=new TickWorkBudget();assertThrows(IllegalArgumentException.class,()->b.tryStart(1,0,2));assertThrows(IllegalArgumentException.class,()->b.tryStart(1,1,0));assertThrows(IllegalArgumentException.class,()->b.record(-1));}
}
