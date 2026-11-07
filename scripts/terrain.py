#!/usr/bin/env python3
"""Native landforms for 1.21.11. Final density + a bounded cold-field surface prefix.
The named original functions are not overridden: biome climate and original deep density keep their inputs.
"""
from pathlib import Path
import argparse,copy,hashlib,importlib.util,json,zipfile
ROOT=Path(__file__).resolve().parents[1];SOURCE=ROOT/'vendor/terrain';OUT=ROOT/'terrain'
def load(p):return json.loads(p.read_text())
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
def pin():
 jar=ROOT/'.runtime/downloads/mojang-inner.jar';expected=load(ROOT/'vendor/storybook/provenance.json')['official_inner_sha256'];assert hashlib.sha256(jar.read_bytes()).hexdigest()==expected
 names=['noise_settings/overworld.json','density_function/overworld/sloped_cheese.json']
 with zipfile.ZipFile(jar) as z:
  files=[]
  for n in names:
   data=z.read('data/minecraft/worldgen/'+n);p=SOURCE/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data);files.append({'file':str(p.relative_to(ROOT)),'sha256':hashlib.sha256(data).hexdigest()})
 write(SOURCE/'provenance.json',{'minecraft':'1.21.11','official_inner_sha256':expected,'sources':files})
def sources():
 p=load(SOURCE/'provenance.json');assert p['minecraft']=='1.21.11'
 for row in p['sources']:assert hashlib.sha256((ROOT/row['file']).read_bytes()).hexdigest()==row['sha256'],row

def unary(kind,arg):return {'type':'minecraft:'+kind,'argument':arg}
def binary(kind,a,b):return {'type':'minecraft:'+kind,'argument1':a,'argument2':b}
def add(a,b):return binary('add',a,b)
def mul(a,b):return binary('mul',a,b)
def clamp(v,low=0,high=1):return {'type':'minecraft:clamp','input':v,'min':low,'max':high}
def gradient(a,b,lo,hi):return {'type':'minecraft:y_clamped_gradient','from_y':a,'to_y':b,'from_value':lo,'to_value':hi}
def field(name):return 'midgard:terrain/'+name
def cached(v):return unary('cache_2d',v)
def noise(name):return cached({'type':'minecraft:noise','noise':'midgard:'+name,'xz_scale':1.0,'y_scale':0.0})
def spline(coordinate,points):return {'type':'minecraft:spline','spline':{'coordinate':coordinate,'points':[{'location':x,'value':y,'derivative':0.0} for x,y in points]}}
def replace(v,old,new):
 if isinstance(v,str):return new if v==old else v
 if isinstance(v,list):return [replace(a,old,new) for a in v]
 if isinstance(v,dict):return {k:replace(a,old,new) for k,a in v.items()}
 return v

def content():
 resources={};f={}
 f['massifs']=noise('fantasy_massifs');f['terraces']=noise('fantasy_terraces');f['ravines']=noise('fantasy_ravines')
 f['ridge']=cached(clamp(add(1,mul(-1.6,unary('abs',field('massifs'))))))
 f['ridge_shape']=cached(spline(field('ridge'),[(0,0),(.30,.04),(.55,.18),(.75,.50),(.92,1),(1,1.12)]))
 f['plateaus']=cached(spline(field('terraces'),[(-1,-.10),(-.45,-.10),(-.34,.08),(-.05,.08),(.06,.34),(.34,.34),(.44,.66),(1,.66)]))
 f['gorge']=cached(unary('square',clamp(add(1,mul(-10,unary('abs',field('ravines')))))))
 f['land_mask']=cached(spline('minecraft:overworld/continents',[(-1.2,0),(-.20,0),(-.13,0),(.02,.65),(.15,1),(1.5,1)]))
 # y=128+128*offset at zero depth. Broad shelves + narrow ridges/gorges.
 f['target_offset']=cached(add(-.40,add(mul(1.05,field('ridge_shape')),add(mul(.65,field('plateaus')),mul(-.43,field('gorge'))))))
 f['displacement']=cached(mul(field('land_mask'),mul(.9,add(field('target_offset'),mul(-1,'minecraft:overworld/offset')))))
 # Exactly zero offset contribution through Y=48. No post-generation terrain writes.
 f['depth']=add('minecraft:overworld/depth',mul(gradient(48,88,0,1),mul({'type':'minecraft:blend_alpha'},field('displacement'))))
 original=load(SOURCE/'density_function/overworld/sloped_cheese.json')
 f['sloped_cheese']=replace(original,'minecraft:overworld/depth',field('depth'))
 # Broad, organic lowland clearings. Blend BEFORE carvers/features/structures, never flatten saved chunks.
 # Low, broad hills keep a walkable landscape with cover, not a flattened arena.
 f['plains_noise']=noise('fantasy_plains')
 f['plains_mask']=cached(spline(field('plains_noise'),[(-2,1),(-.05,1),(.20,0),(2,0)]))
 f['lowland_mask']=cached(spline('minecraft:overworld/offset',[(-2,1),(-.22,1),(-.10,0),(2,0)]))
 # Small meadows belong in existing fantasy lowlands; do not cut a flat pocket through a high massif.
 f['meadow_relief_mask']=cached(spline(field('target_offset'),[(-2,0),(-.65,0),(-.50,1),(-.10,1),(.05,0),(2,0)]))
 f['plains_weight']=mul(gradient(48,64,0,1),mul(field('land_mask'),mul(field('meadow_relief_mask'),mul(field('lowland_mask'),field('plains_mask')))))
 f['meadow_offset']=cached(add(-.34375,mul(.025,noise('fantasy_plains_detail'))))
 f['base_depth_shift']=mul(gradient(48,88,0,1),mul({'type':'minecraft:blend_alpha'},field('displacement')))
 f['meadow_depth_shift']=mul({'type':'minecraft:blend_alpha'},add(field('meadow_offset'),mul(-1,'minecraft:overworld/offset')))
 # Blend offsets before vanilla sloped_cheese, retaining its natural surface detail and cave branch.
 # Blending a flat final-density plane made short transition bands too steep.
 f['depth']=add('minecraft:overworld/depth',add(mul(add(1,mul(-1,field('plains_weight'))),field('base_depth_shift')),mul(field('plains_weight'),field('meadow_depth_shift'))))
 settings=load(SOURCE/'noise_settings/overworld.json')
 f['base_density']=replace(settings['noise_router']['final_density'],'minecraft:overworld/sloped_cheese',field('sloped_cheese'))
 settings['noise_router']['final_density']=field('base_density')
 # Ordinary rooted soil marks gentle cold lowlands without changing caves or sealing their entrances.
 # Sparse trees, ruins, trails and natural grass remain; only dense native forest planting is filtered here.
 def condition(test,then):return {'type':'minecraft:condition','if_true':test,'then_run':then}
 def above(y):return {'type':'minecraft:y_above','anchor':{'absolute':y},'surface_depth_multiplier':0,'add_stone_depth':False}
 tests=[{'type':'minecraft:biome','biome_is':['minecraft:'+p.stem for p in sorted((ROOT/'vendor/vanilla/biome').glob('*.json'))]},
        {'type':'minecraft:noise_threshold','noise':'midgard:fantasy_plains','min_threshold':-100,'max_threshold':-.08},
        above(72),{'type':'minecraft:not','invert':above(97)},
        {'type':'minecraft:stone_depth','offset':0,'add_surface_depth':False,'secondary_depth_range':0,'surface_type':'floor'}]
 prefix={'type':'minecraft:block','result_state':{'Name':'minecraft:rooted_dirt'}}
 for test in reversed(tests):prefix=condition(test,prefix)
 settings['surface_rule']={'type':'minecraft:sequence','sequence':[prefix,settings['surface_rule']]}
 for name,d in f.items():resources['data/midgard/worldgen/density_function/terrain/'+name+'.json']=d
 for name,octave,amps in [('fantasy_massifs',-9,[1,1,.5,.25]),('fantasy_terraces',-8,[1,.3]),('fantasy_ravines',-8,[1,.6,.2]),('fantasy_plains',-7,[1,.15]),('fantasy_plains_detail',-6,[1,.5,.25])]:
  resources['data/midgard/worldgen/noise/'+name+'.json']={'firstOctave':octave,'amplitudes':amps}
 # Keep the vanilla preliminary_surface_level: it feeds aquifers. Structures query native final density/heightmaps.
 # This also keeps every cave, aquifer and ore router input outside the final surface-shaping branch unchanged.
 resources['data/minecraft/worldgen/noise_settings/overworld.json']=settings
 # Derive the same twelve isolated forest placements from their pinned Storybook sources.
 # The terrain-off profile keeps its unfiltered copies. Native sparse plains trees are retained.
 spec=importlib.util.spec_from_file_location('terrain_forest_source',Path(__file__).with_name('storybook.py'));forest=importlib.util.module_from_spec(spec);spec.loader.exec_module(forest)
 forest.ROOT=ROOT;forest.SOURCE=ROOT/'vendor/storybook';forest.OUT=ROOT/'storybook';forest.source_check();base=forest.content()
 for name in forest.TARGETS:
  path='data/minecraft/worldgen/placed_feature/'+name+'.json';d=copy.deepcopy(base[path])
  d['placement'].insert(-1,{'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:not','predicate':{'type':'minecraft:matching_blocks','offset':[0,-1,0],'blocks':'minecraft:rooted_dirt'}}})
  resources[path]=d
 return resources

def validate():
 sources();expected=content();actual={p.relative_to(OUT).as_posix():load(p) for p in OUT.rglob('*.json')};assert actual==expected,'Terrain resources modified/missing/outside allowlist'
 old=load(SOURCE/'noise_settings/overworld.json');new=actual['data/minecraft/worldgen/noise_settings/overworld.json']
 assert {k for k in old if old[k]!=new[k]}=={'noise_router','surface_rule'}
 assert {k for k in old['noise_router'] if old['noise_router'][k]!=new['noise_router'][k]}=={'final_density'}
 for key in ['noise','sea_level','spawn_target','ore_veins_enabled','aquifers_enabled']:assert old[key]==new[key]
 assert new['surface_rule']['sequence'][1]==old['surface_rule']
 # Every custom density reference resolves and the graph is acyclic.
 funcs={p.removeprefix('data/midgard/worldgen/density_function/').removesuffix('.json'):v for p,v in actual.items() if '/density_function/' in p}
 def strings(v):
  if isinstance(v,str):yield v
  elif isinstance(v,dict):
   for a in v.values():yield from strings(a)
  elif isinstance(v,list):
   for a in v:yield from strings(a)
 graph={k:{v.removeprefix('midgard:') for v in strings(d) if v.startswith('midgard:terrain/')} for k,d in funcs.items()}
 def visit(k,path):
  assert k in funcs,k;assert k not in path,('cycle',path,k)
  for ref in graph[k]:visit(ref,path|{k})
 for k in funcs:visit(k,set())
 assert load(OUT/'pack.mcmeta')['pack']['min_format']==[94,1]
 write(ROOT/'reports/terrain-static-validation.json',{'status':'PASS','minecraft':'1.21.11','noise_settings_override':'minecraft:overworld','changed_router_inputs':['final_density'],'native_caves_aquifers_ore_veins_preliminary_surface_kept':True,'biome_router_inputs_kept':True,'original_surface_sequence_kept_after_cold_field_prefix':True,'no_ore_carver_tag_or_structure_definition_overrides':True,'deep_offset_contribution_zero_through_y':48,'pvp_core_surface':'gentle hills with ordinary rooted soil, native snow/grass, sparse trees and normal decorations; no player protection','dense_native_forest_placements_filtered_on_field_soil':12,'resources':len(actual),'runtime_geometry_deep_cave_structure_acceptance':'Separate real server tests required; static comparison does not prove equal generated blocks.'})
 print('PASS: native terrain + broad lowland fields, acyclic references, cave/aquifer/ore/biome inputs kept; cold field surface prefix')
def build():
 sources();expected=content();OUT.mkdir(exist_ok=True)
 for p in OUT.rglob('*.json'):
  if p.relative_to(OUT).as_posix() not in expected:p.unlink()
 for name,d in expected.items():write(OUT/name,d)
 write(OUT/'pack.mcmeta',{'pack':{'description':'Midgard Fantasy Terrain 0.7.0 — broad PvP lowlands, ridges, plateaus and ravines; new worlds only','min_format':[94,1],'max_format':[94,1]}})
 validate()
 with zipfile.ZipFile(ROOT/'dist/fantasy-terrain-datapack.zip','w',zipfile.ZIP_DEFLATED) as z:
  # Manual datapack distribution is a complete profile, like the merged pack inside the JAR.
  # Its forest placements must not reference configured trees missing from the standalone ZIP.
  for p in sorted((ROOT/'storybook').rglob('*')):
   if p.is_file():
    name=p.relative_to(ROOT/'storybook').as_posix()
    if name!='pack.mcmeta' and name not in expected:z.write(p,name)
  for p in sorted(OUT.rglob('*')):
   if p.is_file():z.write(p,p.relative_to(OUT))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--pin-official',action='store_true');p.add_argument('--validate',action='store_true');a=p.parse_args()
 if a.pin_official:pin()
 if a.validate:validate()
 else:build()
