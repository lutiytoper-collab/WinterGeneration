package ru.midgard.winter.core;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class LeaseRegistryTest {
    @Test void overlappingOwnersKeepRequiredNeighbourLoaded(){var r=new LeaseRegistry<String>();assertTrue(r.acquire("a","neighbour"));assertFalse(r.acquire("b","neighbour"));assertTrue(r.release("a").isEmpty());assertTrue(r.contains("neighbour"));assertEquals(Set.of("neighbour"),r.release("b"));assertEquals(0,r.size());}
    @Test void repeatedPollingDoesNotLeakReferences(){var r=new LeaseRegistry<String>();for(int i=0;i<200;i++)r.acquire("a","self");assertEquals(Set.of("self"),r.release("a"));assertTrue(r.release("a").isEmpty());}
    @Test void disableReleasesEveryPhysicalTicketOnce(){var r=new LeaseRegistry<String>();r.acquire("a","a");r.acquire("a","b");r.acquire("b","b");assertEquals(Set.of("a","b"),r.clear());assertEquals(0,r.size());assertTrue(r.release("a").isEmpty());}
}
