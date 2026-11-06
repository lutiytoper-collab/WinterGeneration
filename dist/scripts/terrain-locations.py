#!/usr/bin/env python3
"""Sparse natural-ground map of the actually saved QA world for choosing unmodified game cameras."""
from pathlib import Path
import argparse,collections,importlib.util,json,math
ROOT=Path(__file__).resolve().parents[1];s=importlib.util.spec_from_file_location('terrain_inv',ROOT/'scripts/terrain-invariants.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
def block_reader(root):
 sections={}
 for sec in root['sections']:
  st=sec.get('block_states')
  if st:sections[sec['Y']]=(st['palette'],st.get('data'))
 def get(x,y,z):
  entry=sections.get(y//16)
  if not entry:return 'air'
  palette,data=entry
  if not data:return palette[0]['Name'].removeprefix('minecraft:')
  bits=max(4,(len(palette)-1).bit_length());per=64//bits;i=(y%16)*256+z*16+x;v=(data[i//per]>>((i%per)*bits))&((1<<bits)-1)
  return palette[v]['Name'].removeprefix('minecraft:')
 return get
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('world');p.add_argument('--min-chunk',type=int,default=-8);p.add_argument('--max-chunk',type=int,default=24);a=p.parse_args();folder=ROOT/'.runtime'/a.world/'winter_test';rows=[]
 for cx in range(a.min_chunk,a.max_chunk+1):
  for cz in range(a.min_chunk,a.max_chunk+1):
   try:r=m.m.region.chunk(folder,cx,cz)
   except (FileNotFoundError,ValueError,KeyError,TypeError):continue
   get=block_reader(r)
   for x in [4,12]:
    for z in [4,12]:
     for y in range(319,-65,-1):
      n=get(x,y,z)
      if n in m.GROUND and not (n=='snow_block' and get(x,y-1,z).endswith('_leaves')):
       rows.append({'x':cx*16+x,'y':y,'z':cz*16+z,'block':n});break
 out=ROOT/'reports'/f'{a.world}-ground-map.json';out.write_text(json.dumps({'world':a.world,'seed':825180311,'sampling':'Actual saved region NBT natural-ground scan; 4 columns per chunk. Ignores trunks/leaves/thin snow. Small prop floors with natural materials can affect individual samples.','points':rows},indent=2)+'\n');print('samples',len(rows),'range',min(r['y'] for r in rows),max(r['y'] for r in rows));print('peaks',sorted(rows,key=lambda r:r['y'],reverse=True)[:8]);print('lowlands',sorted(rows,key=lambda r:r['y'])[:5])
