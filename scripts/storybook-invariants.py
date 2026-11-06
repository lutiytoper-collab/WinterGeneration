#!/usr/bin/env python3
"""Compare identical saved coordinates, retaining the original probe's height-window diagnostic.
A tall custom trunk changes MOTION_BLOCKING_NO_LEAVES. Probe hashes from unequal sample
windows cannot prove a cave change. This independent check uses the intersection of both
windows, compares complete states without material normalisation, and checks every ore
coordinate over the full stored world height. It never turns the original failed hash into PASS.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,collections
ROOT=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location('region',ROOT/'scripts/compare-region.py');region=importlib.util.module_from_spec(s);s.loader.exec_module(region)
def states(root):
 out={}
 for sec in root['sections']:
  st=sec.get('block_states')
  if not st:continue
  palette=[v['Name']+('['+','.join(k+'='+value for k,value in sorted(v.get('Properties',{}).items()))+']' if v.get('Properties') else '') for v in st['palette']]
  data=st.get('data');bits=max(4,(len(palette)-1).bit_length());per=64//bits;mask=(1<<bits)-1
  for i in range(4096):
   n=0 if not data else (data[i//per]>>((i%per)*bits))&mask
   out[i%16,sec['Y']*16+i//256,(i//16)%16]=palette[n]
 return out
def heights(root,blocks):
 data=root['Heightmaps']['MOTION_BLOCKING_NO_LEAVES'];result={}
 for i in range(256):
  y=((data[i//7]>>((i%7)*9))&511)-65;x,z=i%16,i//16
  if blocks.get((x,y,z),'').startswith('minecraft:snow['):y-=1
  result[x,z]=y
 return result
def inspect(folder,seed):
 before=json.loads((folder/'vanilla/measurement-restart.json').read_text());after=json.loads((folder/'winter/measurement-restart.json').read_text());rows=[]
 for a,b in zip(before['chunks'],after['chunks']):
  assert (a['x'],a['z'])==(b['x'],b['z']);cx,cz=a['x'],a['z']
  roots=[region.chunk(folder/v/'winter_test',cx,cz) for v in ['vanilla','winter']];blocks=[states(c) for c in roots];height=[heights(c,m) for c,m in zip(roots,blocks)]
  raw=[hashlib.sha256(),hashlib.sha256()];count=0;diff=[];window_changes=[]
  for x in range(16):
   for z in range(16):
    limits=[min(31,h[x,z]-8) for h in height];top=min(limits)
    if limits[0]!=limits[1]:window_changes.append({'column':[cx*16+x,cz*16+z],'probe_y_max_before_after':limits})
    for y in range(-64,top+1):
     pos=x,y,z;v=[m.get(pos,'minecraft:air') for m in blocks];count+=1
     for index in range(2):raw[index].update((str(pos)+':'+v[index]+';').encode())
     if v[0]!=v[1]:diff.append({'xyz':[cx*16+x,y,cz*16+z],'before':v[0],'after':v[1]})
  ores=[{str((cx*16+x,y,cz*16+z)):v for (x,y,z),v in m.items() if v.split('[')[0].endswith('_ore')} for m in blocks]
  ore_diff=[{'xyz':k,'before':ores[0].get(k),'after':ores[1].get(k)} for k in sorted(set(ores[0])|set(ores[1])) if ores[0].get(k)!=ores[1].get(k)]
  rows.append({'chunk':[cx,cz],'common_coordinates':count,'raw_common_hashes':[v.hexdigest() for v in raw],'common_raw_differences':diff,'ore_differences_all_heights':ore_diff,'probe_original_window_changed_columns':window_changes,'original_probe_geometry_equal':a['belowSurfaceGeometry']==b['belowSurfaceGeometry'],'structure_boxes_equal':a['structures']==b['structures'],'block_entities_equal':a['blockEntities']==b['blockEntities']})
 result={'seed':seed,'scope':'Saved after-restart region NBT; complete raw states at identical coordinates in intersection of original probe underground windows. Separate all-height exact ore coordinate/state comparison. Original variable-window diagnostic retained.','baseline':str((folder/'vanilla').resolve().relative_to(ROOT)),'winter':str((folder/'winter').relative_to(ROOT)),'chunks':len(rows),'common_coordinates':sum(v['common_coordinates'] for v in rows),'checks':{'common_raw_underground_equal':all(not v['common_raw_differences'] for v in rows),'all_height_ores_equal':all(not v['ore_differences_all_heights'] for v in rows),'structure_boxes_equal':all(v['structure_boxes_equal'] for v in rows),'block_entities_equal':all(v['block_entities_equal'] for v in rows)},'rows':rows}
 return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('run');p.add_argument('seed',type=int);a=p.parse_args();assert a.run.replace('-','').isalnum();result=inspect(ROOT/'.runtime'/a.run/str(a.seed),a.seed);out=ROOT/'reports'/f'{a.run}-coordinate-invariants.json';out.write_text(json.dumps(result,indent=2)+'\n');print(a.seed,result['checks'],'coordinates',result['common_coordinates']);print(out)
