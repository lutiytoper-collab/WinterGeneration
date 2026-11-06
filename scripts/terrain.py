#!/usr/bin/env python3
"""Native landforms for 1.21.11. Only final terrain density changes; vanilla cave/ore/structure resources remain.
The named original functions are not overridden: biome climate and original deep density keep their inputs.
"""
from pathlib import Path
import argparse,copy,hashlib,json,zipfile
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
 for name,d in f.items():resources['data/midgard/worldgen/density_function/terrain/'+name+'.json']=d
 for name,octave,amps in [('fantasy_massifs',-9,[1,1,.5,.25]),('fantasy_terraces',-8,[1,.3]),('fantasy_ravines',-8,[1,.6,.2])]:
  resources['data/midgard/worldgen/noise/'+name+'.json']={'firstOctave':octave,'amplitudes':amps}
 settings=load(SOURCE/'noise_settings/overworld.json')
 settings['noise_router']['final_density']=replace(settings['noise_router']['final_density'],'minecraft:overworld/sloped_cheese',field('sloped_cheese'))
 # Keep the vanilla preliminary_surface_level: it feeds aquifers. Structures query native final density/heightmaps.
 # This also keeps every cave, aquifer and ore router input outside the final surface-shaping branch unchanged.
 resources['data/minecraft/worldgen/noise_settings/overworld.json']=settings
 return resources

def validate():
 sources();expected=content();actual={p.relative_to(OUT).as_posix():load(p) for p in OUT.rglob('*.json')};assert actual==expected,'Terrain resources modified/missing/outside allowlist'
 old=load(SOURCE/'noise_settings/overworld.json');new=actual['data/minecraft/worldgen/noise_settings/overworld.json']
 assert {k for k in old if old[k]!=new[k]}=={'noise_router'}
 assert {k for k in old['noise_router'] if old['noise_router'][k]!=new['noise_router'][k]}=={'final_density'}
 for key in ['noise','surface_rule','sea_level','spawn_target','ore_veins_enabled','aquifers_enabled']:assert old[key]==new[key]
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
 write(ROOT/'reports/terrain-static-validation.json',{'status':'PASS','minecraft':'1.21.11','noise_settings_override':'minecraft:overworld','changed_router_inputs':['final_density'],'native_caves_aquifers_ore_veins_preliminary_surface_kept':True,'biome_router_inputs_kept':True,'surface_rules_kept':True,'no_ore_carver_or_structure_definition_overrides':True,'deep_offset_contribution_zero_through_y':48,'resources':len(actual),'runtime_geometry_deep_cave_structure_acceptance':'Separate real server tests required; static comparison does not prove equal generated blocks.'})
 print('PASS: native terrain density, acyclic references, only final_density changed, cave/aquifer/ore/biome inputs kept')
def build():
 sources();expected=content();OUT.mkdir(exist_ok=True)
 for p in OUT.rglob('*.json'):
  if p.relative_to(OUT).as_posix() not in expected:p.unlink()
 for name,d in expected.items():write(OUT/name,d)
 write(OUT/'pack.mcmeta',{'pack':{'description':'Midgard Fantasy Terrain 0.6.0 — native ridges, terraced plateaus and ravines; new worlds only','min_format':[94,1],'max_format':[94,1]}})
 validate()
 with zipfile.ZipFile(ROOT/'dist/fantasy-terrain-datapack.zip','w',zipfile.ZIP_DEFLATED) as z:
  for p in sorted(OUT.rglob('*')):
   if p.is_file():z.write(p,p.relative_to(OUT))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--pin-official',action='store_true');p.add_argument('--validate',action='store_true');a=p.parse_args()
 if a.pin_official:pin()
 if a.validate:validate()
 else:build()
