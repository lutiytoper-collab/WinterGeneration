package ru.midgard.winter.core;
public record Settings(double snowDensity,int snowLayers,double forestDensity,double clearingChance,
                       double cabinChance,double iceChance,double lightChance,int maxChanges,int margin) {
    public Settings {
        for(double v:new double[]{snowDensity,forestDensity,clearingChance,cabinChance,iceChance,lightChance})
            if(!Double.isFinite(v)||v<0||v>1) throw new IllegalArgumentException("Probability must be finite in [0,1]");
        if(snowLayers<1||snowLayers>6||maxChanges<1||maxChanges>512||margin<0||margin>32)
            throw new IllegalArgumentException("Unsafe generation limits");
    }
    public static Settings preset(String name) {
        return switch(name) {
            case "LIGHT" -> new Settings(.28,2,.12,.003,0,.002,.20,192,8);
            case "BALANCED" -> new Settings(.58,4,.38,.012,.0003,.008,.60,384,8);
            case "FAIRYTALE" -> new Settings(.85,5,.70,.028,.0005,.018,1.0,512,8);
            default -> throw new IllegalArgumentException("Unknown preset: "+name);
        };
    }
}
