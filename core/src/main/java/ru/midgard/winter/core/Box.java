package ru.midgard.winter.core;
public record Box(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
    public Box { if(minX>maxX || minY>maxY || minZ>maxZ) throw new IllegalArgumentException("inverted box"); }
    public boolean intersects(Box b, int margin) {
        if(margin<0) throw new IllegalArgumentException("margin");
        return (long)maxX+margin>=b.minX && (long)b.maxX+margin>=minX
            && (long)maxY+margin>=b.minY && (long)b.maxY+margin>=minY
            && (long)maxZ+margin>=b.minZ && (long)b.maxZ+margin>=minZ;
    }
    public boolean inChunk(int x,int z) {
        return Math.floorDiv(minX,16)==x && Math.floorDiv(maxX,16)==x
            && Math.floorDiv(minZ,16)==z && Math.floorDiv(maxZ,16)==z;
    }
}
