package ru.midgard.winter;
import com.google.gson.*;
import ru.midgard.winter.core.DecorationPlan;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
/** Audited, sanitized built-in geometry only. No arbitrary .schem execution or external runtime dependency. */
final class SchematicCatalog {
    private SchematicCatalog(){}
    static List<DecorationPlan> load(Function<String,InputStream> resource)throws IOException{
        var plans=new ArrayList<DecorationPlan>();
        for(String id:List.of("moon-gate-7","moon-gate-9","frost-shrine","woodland-shrine","stone-ruin","timber-ruin")){
            String file="schematics/"+id+".json";JsonObject root;
            try(var stream=Objects.requireNonNull(resource.apply(file),"Missing "+file)){
                byte[] bytes=stream.readNBytes(262145);if(bytes.length>262144)throw new IOException("Template too large");
                root=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            }
            if(root.get("format").getAsInt()!=1||!root.get("license").getAsString().equals(id.startsWith("moon-gate-")?"CC0-1.0":"PROJECT"))throw new IOException("Unsupported audited template metadata");
            if(!root.get("id").getAsString().equals(id))throw new IOException("Unexpected template id");
            var cells=new ArrayList<DecorationPlan.Cell>();
            for(var value:root.getAsJsonArray("cells")){
                var c=value.getAsJsonObject();cells.add(new DecorationPlan.Cell(c.get("x").getAsInt(),c.get("y").getAsInt(),c.get("z").getAsInt(),c.get("block").getAsString(),c.get("layers").getAsInt()));
                if(cells.size()>512)throw new IOException("Template exceeds write budget");
            }
            plans.add(new DecorationPlan(root.get("id").getAsString(),cells));
        }
        return List.copyOf(plans);
    }
    static List<DecorationPlan> forBiome(List<DecorationPlan> plans,String biome){
        boolean woodland=biome.contains("forest")||biome.contains("birch"),cold=biome.contains("taiga")||biome.contains("snow")||biome.contains("peak")||biome.equals("grove");
        return plans.stream().filter(p->p.module().startsWith("moon-gate-")||(!woodland&&!cold)||
            (woodland&&(p.module().equals("woodland-shrine")||p.module().equals("timber-ruin")))||
            (cold&&(p.module().equals("frost-shrine")||p.module().equals("stone-ruin")))).toList();
    }
}
