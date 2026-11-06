#!/usr/bin/env python3
"""Validate immutable terrain/structures and preservation, retaining all exact material differences."""
from pathlib import Path
import importlib.util,json,re
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('region',ROOT/'scripts/compare-region.py');region=importlib.util.module_from_spec(spec);spec.loader.exec_module(region)
CASES=[('frozen',825180311),('boundary',42),('boundary-final',-892041)]
result=[]
for run,seed in CASES:
 folder=ROOT/'.runtime'/run/str(seed);worlds={m:json.loads((folder/m/'measurement-restart.json').read_text()) for m in ['vanilla','winter']}
 check={'seed':seed,'run':run,'chunks_per_world':len(worlds['vanilla']['chunks']),'checks':{},'nonterrain_changes':[],'additional_block_entities':[],'raw_material_differences':[]}
 terrain=True;structures=True;entities=True;restart=True
 for mode in ['vanilla','winter']:
  before=json.loads((folder/mode/'measurement-initial.json').read_text())['chunks'];after=worlds[mode]['chunks']
  for a,b in zip(before,after):
   for key in ['x','z','belowSurfaceGeometry','surfaceByBiome','blockEntities','winterMarker']:
    restart=restart and a.get(key)==b.get(key)
   normalize=lambda entries:[re.sub(r'(minY|maxY)=-?[0-9.]+',r'\1=<refreshed>',s) for s in entries]
   restart=restart and normalize(a['structures'])==normalize(b['structures'])
 for a,b in zip(worlds['vanilla']['chunks'],worlds['winter']['chunks']):
  assert (a['x'],a['z'])==(b['x'],b['z'])
  structures=structures and a['structures']==b['structures']
  entities=entities and set(a['blockEntities'])<=set(b['blockEntities'])
  check['additional_block_entities'] += sorted(set(b['blockEntities'])-set(a['blockEntities']))
  if a['belowSurfaceGeometry']!=b['belowSurfaceGeometry']:
   cx,cz=a['x'],a['z'];old=region.blocks(region.chunk(folder/'vanilla/winter_test',cx,cz));new=region.blocks(region.chunk(folder/'winter/winter_test',cx,cz))
   for position in set(old)|set(new):
    if old.get(position)!=new.get(position):
     x,y,z=position;change={'x':cx*16+x,'y':y,'z':cz*16+z,'before':old.get(position),'after':new.get(position)};check['raw_material_differences'].append(change)
     # Seagrass occupies a water cell and has no solid collision geometry; preserve the raw change separately.
     norm=lambda name:'minecraft:water' if name in {'minecraft:seagrass','minecraft:tall_seagrass'} else name
     if norm(old.get(position))!=norm(new.get(position)):terrain=False
     else:check['nonterrain_changes'].append(change)
 check['checks']={'terrain_and_ores':terrain,'structure_boxes_after_reload':structures,'existing_block_entities_preserved':entities,'restart_idempotence':restart}
 result.append(check)
(ROOT/'reports/invariant-validation.json').write_text(json.dumps(result,indent=2)+'\n')
for r in result:
 print(r['seed'],r['checks'],'nonterrain differences',len(r['nonterrain_changes']),'new block entities',r['additional_block_entities'])
assert all(all(r['checks'].values()) for r in result),'An immutable invariant failed; inspect raw evidence'
