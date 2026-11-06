#!/usr/bin/env python3
"""Build an opt-in native Storybook vegetation/atmosphere pack for exact Minecraft1.21.11.
Keep every biome feature list and its ordering; override only placed tree features whose
complete vanilla reachability is confined to the winter allowlist. Climate-only pack stays separate.
"""
from pathlib import Path
import argparse,copy,hashlib,json,zipfile,collections
ROOT=Path(__file__).resolve().parents[1];SOURCE=ROOT/'vendor/storybook';OUT=ROOT/'storybook'
TARGETS={
 'trees_taiga':('fir',5),'trees_grove':('fir',4),
 'trees_old_growth_pine_taiga':('ancient_fir',4),'trees_old_growth_spruce_taiga':('ancient_fir',4),
 'trees_windswept_forest':('fir',3),'trees_windswept_hills':('fir',1),
 'trees_birch':('birch',5),'birch_tall':('elder_birch',5),
 'trees_birch_and_oak_leaf_litter':('mixed',5),'trees_flower_forest':('mixed',4),
 'trees_meadow':('birch',1),'dark_forest_vegetation':('dark',4)}
TEMPLATES=['spruce','mega_spruce','fancy_oak','pale_oak']
def load(p):return json.loads(p.read_text())
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
def pin(server):
 expected=load(ROOT/'reports/mojang-biome-verification.json')['server_sha1']
 outer=ROOT/'.runtime/downloads/mojang-1.21.11.jar'
 assert hashlib.sha1(outer.read_bytes()).hexdigest()==expected,'Official server checksum mismatch'
 with zipfile.ZipFile(outer) as z:
  inner=z.read('META-INF/versions/1.21.11/server-1.21.11.jar')
 assert hashlib.sha256(server.read_bytes()).digest()==hashlib.sha256(inner).digest(),'Inner server does not match verified official bundle'
 with zipfile.ZipFile(server) as z:
  all_biomes={Path(n).stem:json.loads(z.read(n)) for n in z.namelist() if n.startswith('data/minecraft/worldgen/biome/') and n.endswith('.json')}
  nodes={}
  for kind in ['placed_feature','configured_feature']:
   for n in z.namelist():
    if n.startswith('data/minecraft/worldgen/'+kind+'/') and n.endswith('.json'):nodes[kind,Path(n).stem]=json.loads(z.read(n))
  def strings(v):
   if isinstance(v,str):yield v
   elif isinstance(v,dict):
    for value in v.values():yield from strings(value)
   elif isinstance(v,list):
    for value in v:yield from strings(value)
  graph={key:{(kind,s.removeprefix('minecraft:')) for s in strings(data) for kind in ['placed_feature','configured_feature'] if (kind,s.removeprefix('minecraft:')) in nodes} for key,data in nodes.items()}
  refs=collections.defaultdict(set)
  for biome,data in all_biomes.items():
   todo=[('placed_feature',f.removeprefix('minecraft:')) for group in data['features'] for f in group];visited=set()
   while todo:
    key=todo.pop()
    if key in visited:continue
    visited.add(key);refs[key].add(biome);todo.extend(graph.get(key,[])-visited)
  allow={p.stem for p in (ROOT/'vendor/vanilla/biome').glob('*.json')}
  proof={name:sorted(refs['placed_feature',name]) for name in TARGETS}
  assert all(set(v)<=allow and v for v in proof.values()),proof
  resources=[]
  for kind,names in [('placed_feature',TARGETS),('configured_feature',TEMPLATES)]:
   for name in names:
    data=z.read(f'data/minecraft/worldgen/{kind}/{name}.json');dest=SOURCE/kind/(name+'.json');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(data)
    resources.append({'file':str(dest.relative_to(ROOT)),'sha256':hashlib.sha256(data).hexdigest()})
 write(SOURCE/'provenance.json',{'minecraft':'1.21.11','official_server_sha1':expected,'official_inner_sha256':hashlib.sha256(server.read_bytes()).hexdigest(),'allowlisted_reachable_biomes_by_override':proof,'reachability':'Conservative transitive graph of all official biome/placed/configured feature references; ambiguous IDs follow both kinds.','sources':resources})
def source_check():
 proof=load(SOURCE/'provenance.json');allow={p.stem for p in (ROOT/'vendor/vanilla/biome').glob('*.json')}
 assert set(proof['allowlisted_reachable_biomes_by_override'])==set(TARGETS)
 assert all(set(v)<=allow and v for v in proof['allowlisted_reachable_biomes_by_override'].values())
 for p in proof['sources']:assert hashlib.sha256((ROOT/p['file']).read_bytes()).hexdigest()==p['sha256'],p['file']
def state(name,**props):return {'Name':'minecraft:'+name,**({'Properties':props} if props else {})}
def simple(s):return {'type':'minecraft:simple_state_provider','state':s}
def tree(template,trunk,foliage,log,lanterns=True):
 d=copy.deepcopy(load(SOURCE/'configured_feature'/(template+'.json')));c=d['config'];c['trunk_placer']=trunk;c['foliage_placer']=foliage
 c['trunk_provider']=simple(state(log,axis='y'));c['foliage_provider']=simple(state('oak_leaves',distance='7',persistent='true',waterlogged='false'))
 c['decorators']=[{'type':'minecraft:pale_moss','ground_probability':0.0,'leaves_probability':.025,'trunk_probability':.12}]
 if lanterns:c['decorators'].append({'type':'minecraft:attached_to_leaves','probability':.003,'exclusion_radius_xz':4,'exclusion_radius_y':3,'block_provider':simple(state('lantern',hanging='true',waterlogged='false')),'required_empty_blocks':2,'directions':['down']})
 c['force_dirt']=False;c['ignore_vines']=True
 return d
def uniform(a,b):return {'type':'minecraft:uniform','min_inclusive':a,'max_inclusive':b}
def straight(h,a,b):return {'type':'minecraft:straight_trunk_placer','base_height':h,'height_rand_a':a,'height_rand_b':b}
def fancy(h,a,b):return {'type':'minecraft:fancy_trunk_placer','base_height':h,'height_rand_a':a,'height_rand_b':b}
def spruce(radius,stem):return {'type':'minecraft:spruce_foliage_placer','radius':radius,'offset':0,'trunk_height':stem}
def content():
 trees={
 'cathedral_fir':tree('mega_spruce',{'type':'minecraft:giant_trunk_placer','base_height':26,'height_rand_a':6,'height_rand_b':4},{'type':'minecraft:mega_pine_foliage_placer','radius':2,'offset':0,'crown_height':uniform(22,24)},'spruce_log'),
 'spire_fir':tree('spruce',straight(21,6,3),spruce(uniform(4,5),uniform(2,4)),'spruce_log'),
 'young_fir':tree('spruce',straight(11,5,2),spruce(uniform(2,4),uniform(2,3)),'spruce_log',False),
 'silver_birch':tree('fancy_oak',fancy(13,6,3),{'type':'minecraft:fancy_foliage_placer','radius':3,'offset':4,'height':5},'birch_log'),
 'elder_birch':tree('fancy_oak',fancy(20,7,3),{'type':'minecraft:fancy_foliage_placer','radius':4,'offset':4,'height':6},'birch_log'),
 'moon_oak':tree('fancy_oak',fancy(16,7,3),{'type':'minecraft:fancy_foliage_placer','radius':3,'offset':4,'height':6},'oak_log'),
 'ancient_oak':tree('pale_oak',{'type':'minecraft:dark_oak_trunk_placer','base_height':14,'height_rand_a':5,'height_rand_b':3},{'type':'minecraft:dark_oak_foliage_placer','radius':2,'offset':0},'dark_oak_log')}
 mixes={'fir':[('cathedral_fir',.28),('young_fir',.25),('spire_fir',1)],'ancient_fir':[('cathedral_fir',.65),('young_fir',.15),('spire_fir',1)],'birch':[('elder_birch',.18),('silver_birch',1)],'elder_birch':[('elder_birch',.75),('silver_birch',1)],'mixed':[('silver_birch',.30),('cathedral_fir',.10),('moon_oak',1)],'dark':[('cathedral_fir',.15),('ancient_oak',1)]}
 resources={}
 for name,d in trees.items():
  resources['data/midgard/worldgen/configured_feature/'+name+'.json']=d
  sapling='birch_sapling' if 'birch' in name else 'dark_oak_sapling' if name=='ancient_oak' else 'oak_sapling' if name=='moon_oak' else 'spruce_sapling'
  resources['data/midgard/worldgen/placed_feature/'+name+'_checked.json']={'feature':'midgard:'+name,'placement':[{'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:would_survive','state':state(sapling,stage='0')}}]}
 for name,variants in mixes.items():resources['data/midgard/worldgen/configured_feature/'+name+'_grove.json']={'type':'minecraft:random_selector','config':{'features':[{'chance':chance,'feature':'midgard:'+t+'_checked'} for t,chance in variants[:-1]],'default':'midgard:'+variants[-1][0]+'_checked'}}
 for name,(mix,count) in TARGETS.items():
  d=copy.deepcopy(load(SOURCE/'placed_feature'/(name+'.json')));d['feature']='midgard:'+mix+'_grove'
  # Old-growth counts10+ vanilla giants become fewer, much broader full-length crowns.
  d['placement']=[{'type':'minecraft:count','count':count},{'type':'minecraft:in_square'},{'type':'minecraft:surface_water_depth_filter','max_water_depth':0},{'type':'minecraft:heightmap','heightmap':'OCEAN_FLOOR'},{'type':'minecraft:biome'}]
  resources['data/minecraft/worldgen/placed_feature/'+name+'.json']=d
 for p in sorted((ROOT/'datapack/data/minecraft/worldgen/biome').glob('*.json')):
  d=load(p);d['effects'].update(foliage_color='#c4dce8',grass_color='#a7bdcc',dry_foliage_color='#b5cad3',water_color='#5899b5')
  d.setdefault('attributes',{}).update({'minecraft:visual/fog_color':'#d2d6e9','minecraft:visual/sky_color':'#b8cceb','minecraft:visual/water_fog_color':'#7197ac'})
  resources['data/minecraft/worldgen/biome/'+p.name]=d
 return resources
def build():
 source_check();expected=content();OUT.mkdir(exist_ok=True)
 for p in OUT.rglob('*.json'):
  if p.relative_to(OUT).as_posix() not in expected:p.unlink()
 for name,data in expected.items():write(OUT/name,data)
 write(OUT/'pack.mcmeta',{'pack':{'description':'Midgard Storybook 0.3.0 — Minecraft 1.21.11; native frost forests + winter climate','min_format':[94,1],'max_format':[94,1]}})
 validate()
 with zipfile.ZipFile(ROOT/'dist/storybook-datapack.zip','w',zipfile.ZIP_DEFLATED) as z:
  for p in sorted(OUT.rglob('*')):
   if p.is_file():z.write(p,p.relative_to(OUT))
def validate():
 source_check();expected=content();actual={p.relative_to(OUT).as_posix():load(p) for p in OUT.rglob('*.json')}
 assert actual==expected,'Storybook resource missing, modified or outside explicit resource allowlist'
 changes=[]
 for p in (OUT/'data/minecraft/worldgen/biome').glob('*.json'):
  original=load(ROOT/'vendor/vanilla/biome'/p.name);d=load(p)
  for key in ['features','carvers','spawners','spawn_costs']:assert d[key]==original[key],(p.name,key)
  assert set(k for k in original if original[k]!=d[k])<={'attributes','effects','temperature','has_precipitation'}
  changes.append(p.stem)
 assert len(changes)==23
 meta=load(OUT/'pack.mcmeta')['pack'];assert meta['min_format']==meta['max_format']==[94,1]
 write(ROOT/'reports/storybook-static-validation.json',{'status':'PASS','minecraft':'1.21.11','biomes':changes,'native_tree_overrides':list(TARGETS),'original_feature_lists_and_order_preserved':True,'ore_carver_structure_definitions_unchanged':True,'source_proof':'vendor/storybook/provenance.json','resources':len(actual),'server_codec':'NOT RUN by this static validator'})
 print(f'PASS: Storybook23 biomes, {len(TARGETS)} isolated native tree overrides; exact feature lists/order; pinned official resources. Codec/runtime separate.')
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--pin-official',type=Path);p.add_argument('--validate',action='store_true');a=p.parse_args()
 if a.pin_official:pin(a.pin_official)
 if a.validate:validate()
 else:build()
