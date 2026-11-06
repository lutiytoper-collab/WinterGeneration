package ru.midgard.winter.core;
import java.util.*;
/** Raise a small clearing above uneven soil and extend its feet; never flatten or replace terrain. */
public final class FoundationFitter {
    public record Fitted(DecorationPlan plan,int ground){}
    private FoundationFitter(){}
    public static Fitted fit(DecorationPlan plan,PlacementValidator.Site site,int x,int z,int maxSlope){
        List<DecorationPlan.Cell> feet=plan.cells().stream().filter(c->c.y()==1).toList();if(feet.isEmpty())return null;
        int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
        for(var c:feet){int y=site.surface(x+c.x(),z+c.z());low=Math.min(low,y);high=Math.max(high,y);}
        if(high-low>maxSlope)return null;
        List<DecorationPlan.Cell> cells=new ArrayList<>();int raise=high-low;
        for(var c:plan.cells()){
            var foot=feet.stream().filter(v->v.x()==c.x()&&v.z()==c.z()).findFirst().orElse(null);
            boolean local=foot!=null&&!foot.block().equals("SPRUCE_LOG");
            int offset=local?site.surface(x+c.x(),z+c.z())-low:raise;
            cells.add(new DecorationPlan.Cell(c.x(),c.y()+offset,c.z(),c.block(),c.layers()));
        }
        for(var c:feet){if(!c.block().equals("SPRUCE_LOG"))continue;int local=site.surface(x+c.x(),z+c.z())-low;String support=c.block();
            for(int y=local+1;y<=raise;y++)cells.add(new DecorationPlan.Cell(c.x(),y,c.z(),support,0));
        }
        return new Fitted(new DecorationPlan(plan.module(),cells),low);
    }
    /** Raise a mineral cluster as one piece and extend every original ground foot down to its own soil. */
    public static Fitted fitMineral(DecorationPlan plan,PlacementValidator.Site site,int x,int z,int maxSlope){
        var feet=plan.cells().stream().filter(c->c.y()==1).toList();if(feet.isEmpty())return null;
        int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
        for(var c:feet){int y=site.surface(x+c.x(),z+c.z());low=Math.min(low,y);high=Math.max(high,y);}
        if(high-low>maxSlope)return null;
        int raise=high-low;var cells=new ArrayList<DecorationPlan.Cell>();
        for(var c:plan.cells())cells.add(new DecorationPlan.Cell(c.x(),c.y()+raise,c.z(),c.block(),c.layers()));
        for(var c:feet)for(int y=site.surface(x+c.x(),z+c.z())-low+1;y<=raise;y++)cells.add(new DecorationPlan.Cell(c.x(),y,c.z(),c.block(),c.layers()));
        return new Fitted(new DecorationPlan(plan.module(),cells),low);
    }
}
