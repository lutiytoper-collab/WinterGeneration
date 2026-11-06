package ru.midgard.winter.core;
import java.util.*;
/** Bounded by the caller; overlapping generation owners share physical chunk tickets. */
public final class LeaseRegistry<K> {
    private final Map<K,Set<K>> owners=new HashMap<>();private final Map<K,Integer> users=new HashMap<>();
    public boolean acquire(K owner,K item){
        if(!owners.computeIfAbsent(owner,k->new HashSet<>()).add(item))return false;
        int before=users.getOrDefault(item,0);users.put(item,before+1);return before==0;
    }
    public boolean contains(K item){return users.containsKey(item);}
    public int size(){return users.size();}
    public Set<K> release(K owner){
        Set<K> held=owners.remove(owner),free=new HashSet<>();if(held==null)return free;
        for(K item:held){int count=users.get(item);if(count==1){users.remove(item);free.add(item);}else users.put(item,count-1);}
        return free;
    }
    public Set<K> clear(){Set<K> free=Set.copyOf(users.keySet());owners.clear();users.clear();return free;}
}
