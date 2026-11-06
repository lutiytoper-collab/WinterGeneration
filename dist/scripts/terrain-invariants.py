#!/usr/bin/env python3
"""Saved NBT terrain/deep-cave comparison at identical coordinates; no material normalization."""
from pathlib import Path
import argparse,collections,hashlib,importlib.util,json,statistics
ROOT=Path(__file__).resolve().parents[1];s=importlib.util.spec_from_file_location('inv',ROOT/'scripts/storybook-invariants.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
GROUND={'stone','deepslate','dirt','grass_block','podzol','coarse_dirt','rooted_dirt','gravel','sand','red_sand','sandstone','red_sandstone','granite','diorite','andesite','tuff','calcite','snow_block','ice','packed_ice','blue_ice','water','terracotta','red_terracotta','orange_terracotta','yellow_terracotta','white_terracotta','light_gray_terracotta','brown_terracotta'}
def name(v):return v.split('[')[0].removeprefix('minecraft:')
def ground(blocks,x,z):
 for y in range(319,-65,-1):
  v=name(blocks.get((x,y,z),'minecraft:air'))
  if v not in GROUND:continue
  if v=='snow_block' and name(blocks.get((x,y-1,z),'')).endswith('_leaves'):continue
  return y
 return -64
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('run');p.add_argument('seed',type=int);a=p.parse_args();folder=ROOT/'.runtime'/a.run/str(a.seed);base=json.loads((folder/'vanilla/measurement-restart.json').read_text());winter=json.loads((folder/'winter/measurement-restart.json').read_text());rows=[];heights=[{},{}];deepdiff=[];upperdiff=[];orediff=[];raw=[hashlib.sha256(),hashlib.sha256()];count=0
 for one,two in zip(base['chunks'],winter['chunks']):
  assert (one['x'],one['z'])==(two['x'],two['z']);cx,cz=one['x'],one['z'];blocks=[m.states(m.region.chunk(folder/v/'winter_test',cx,cz)) for v in ['vanilla','winter']]
  changed=0;local=[]
  for x in range(16):
   for z in range(16):
    for index in range(2):heights[index][cx*16+x,cz*16+z]=ground(blocks[index],x,z)
    delta=heights[1][cx*16+x,cz*16+z]-heights[0][cx*16+x,cz*16+z];local.append(delta);changed+=delta!=0
    for y in range(-64,32):
     pos=x,y,z;values=[d.get(pos,'minecraft:air') for d in blocks];xyz=[cx*16+x,y,cz*16+z]
     if y<=0:
      count+=1
      for index,value in enumerate(values):raw[index].update((str(xyz)+':'+value+';').encode())
     if values[0]!=values[1]:
      row={'xyz':xyz,'before':values[0],'after':values[1]};(deepdiff if y<=0 else upperdiff).append(row)
      if any(name(v).endswith('_ore') for v in values):orediff.append(row)
  rows.append({'chunk':[cx,cz],'changedTerrainColumns':changed,'meanHeightDelta':statistics.mean(local),'minDelta':min(local),'maxDelta':max(local)})
 deltas=[heights[1][p]-h for p,h in heights[0].items()];stats=[]
 for hs in heights:
  values=sorted(hs.values());slopes=[abs(h-hs[x+1,z]) for (x,z),h in hs.items() if (x+1,z) in hs]+[abs(h-hs[x,z+1]) for (x,z),h in hs.items() if (x,z+1) in hs]
  stats.append({'min':min(values),'max':max(values),'mean':statistics.mean(values),'p10':values[int(len(values)*.1)],'p90':values[int(len(values)*.9)],'heightRange':max(values)-min(values),'meanAdjacentSlope':statistics.mean(slopes),'maxAdjacentSlope':max(slopes)})
 def cavity(v):
  n=name(v)
  if n in {'water','lava'}:return n
  if n in {'glow_lichen','cave_vines','cave_vines_plant'}:return 'water' if 'waterlogged=true' in v else 'open'
  return 'open' if n in {'air','cave_air','void_air'} else 'solid'
 cavity_differences=[v for v in deepdiff+upperdiff if cavity(v['before'])!=cavity(v['after'])]
 checks={'cave_wall_and_fluid_classes_equal_through31':not cavity_differences,'deep_raw_states_y_minus64_through_0_equal':not deepdiff,'deep_ores_y_minus64_through_0_equal':not [d for d in orediff if d['xyz'][1]<=0],'terrain_changed_at_least_half_sampled_columns':sum(abs(d)>=8 for d in deltas)>=len(deltas)/2,'substantial_mean_absolute_height_change':statistics.mean(abs(d) for d in deltas)>=12}
 result={'seed':a.seed,'scope':'Saved native FULL region NBT, same XZ and fixed deep coordinates. Height scan selects actual natural ground materials, skips tree logs/leaves and thin snow. Upper-surface differences are diagnostic, not normalized.','columns':len(deltas),'cave_geometry_coordinates_through31':len(deltas)*96,'deep_coordinates':count,'rawDeepHashes':[h.hexdigest() for h in raw],'checks':checks,'naturalGroundStats':{'vanilla':stats[0],'terrain':stats[1]},'meanAbsoluteHeightChange':statistics.mean(abs(d) for d in deltas),'changedByAtLeast8Blocks':sum(abs(d)>=8 for d in deltas),'deepDifferences':deepdiff,'y1_through31_differences':upperdiff,'caveGeometryDifferencesThrough31':cavity_differences,'caveComparisonClassification':'Open air, cave vines and dry glow lichen share open class; waterlogged lichen is water. Full original raw material differences are retained separately and are NOT renamed PASS.','oreDifferencesThrough31':orediff,'rows':rows}
 out=ROOT/'reports'/f'{a.run}-terrain-invariants.json';out.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:result[k] for k in ['checks','columns','deep_coordinates','naturalGroundStats','meanAbsoluteHeightChange','changedByAtLeast8Blocks']},indent=2));print(out)
