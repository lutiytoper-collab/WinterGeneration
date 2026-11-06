#!/usr/bin/env python3
"""Convert pinned CC0 Litematica circle geometry into bounded, sanitized winter gate assets.
Only the two audited single-region stone/air files are accepted. No generic untrusted runtime import.
"""
from pathlib import Path
import gzip,hashlib,importlib.util,json,math,struct
ROOT=Path(__file__).resolve().parents[1];SOURCE=ROOT/'vendor/schematics/greaby-circles'
s=importlib.util.spec_from_file_location('nbt',ROOT/'scripts/compare-region.py');nbt=importlib.util.module_from_spec(s);s.loader.exec_module(nbt)
def read(p):return json.loads(p.read_text())
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
def load_circle(size):
 proof=read(SOURCE/'provenance.json');assert proof['license']=='CC0-1.0';assert proof['commit']=='1263463cffb59b08275e9ee9408848285c4cddff'
 for item in proof['files']:assert hashlib.sha256((ROOT/item['path']).read_bytes()).hexdigest()==item['sha256'],item['path']
 p=SOURCE/f'circle-{size}x{size}.litematic';assert p.stat().st_size<=65536
 with gzip.open(p,'rb') as f:data=f.read(2*1024*1024+1)
 assert len(data)<=2*1024*1024
 root=nbt.NBT(data).root();assert root['Version']==5 and len(root['Regions'])==1
 region=next(iter(root['Regions'].values()));dims=region['Size'];assert [abs(dims[k]) for k in ['x','y','z']]==[size,1,size]
 for key in ['Entities','TileEntities','PendingBlockTicks','PendingFluidTicks']:assert not region.get(key),key
 palette=region['BlockStatePalette'];assert {v['Name'] for v in palette}=={'minecraft:air','minecraft:stone'} and all(not v.get('Properties') for v in palette)
 bits=max(2,(len(palette)-1).bit_length());longs=[n&((1<<64)-1) for n in region['BlockStates']];points=[]
 for index in range(size*size):
  bit=index*bits;word,shift=divmod(bit,64);value=longs[word]>>shift
  if shift+bits>64:value|=longs[word+1]<<(64-shift)
  state=palette[value&((1<<bits)-1)]['Name']
  if state!='minecraft:air':points.append((index%size,index//size))
 assert len(points)==root['Metadata']['TotalBlocks'];return points

def gate(size):
 points=load_circle(size);half=size//2;cells={}
 def put(x,y,z,block,layers=0):
  assert abs(x)<=5 and abs(z)<=5 and 1<=y<=24;cells[x,y,z]={'x':x,'y':y,'z':z,'block':block,'layers':layers}
 for x in range(-half-1,half+2):
  for z in range(-1,2):put(x,1,z,'DIORITE')
 for x,y in points:
  px=x-half;py=y+2
  put(px,py,0,'DIORITE' if (x+y)%3 else 'LIGHT_BLUE_STAINED_GLASS')
  put(px,py,1,'PURPLE_STAINED_GLASS' if (x+y)%4==0 else 'LIGHT_BLUE_STAINED_GLASS')
 # Lamps stay low and do not add block entities/loot tables. Ordinary ice is not used.
 put(-half,2,-1,'JACK_O_LANTERN');put(half,2,-1,'JACK_O_LANTERN')
 put(0,size+2,0,'WHITE_STAINED_GLASS')
 for c in list(cells.values()):
  key=c['x'],c['y']+1,c['z']
  if c['block'] in ['DIORITE','LIGHT_BLUE_STAINED_GLASS','PURPLE_STAINED_GLASS'] and key not in cells:put(*key,'SNOW',3)
 assert len(cells)<=512
 return {'format':1,'id':f'moon-gate-{size}','source':'Greaby/minecraft-circles-schematics','source_commit':'1263463cffb59b08275e9ee9408848285c4cddff','license':'CC0-1.0','adaptation':'Original flat stone ring rotated upright; ordinary diorite/colored-glass palette, snow, low lights and a supported base. No original air is pasted.','cells':list(cells.values())}

def string(v):b=v.encode();return struct.pack('>H',len(b))+b
def payload(t,v):
 if t==3:return struct.pack('>i',v)
 if t==8:return string(v)
 if t==9:return bytes([v[0]])+struct.pack('>i',len(v[1]))+b''.join(payload(v[0],item) for item in v[1])
 if t==10:return b''.join(bytes([tag])+string(name)+payload(tag,value) for name,(tag,value) in v.items())+b'\0'
 raise ValueError(t)
def structure(plan):
 palette=[];states={};blocks=[];cells=plan['cells'];minx=min(c['x'] for c in cells);minz=min(c['z'] for c in cells)
 for c in cells:
  state={'Name':(8,'minecraft:'+c['block'].lower())}
  if c['block']=='SNOW':state['Properties']=(10,{'layers':(8,str(c['layers']))})
  key=json.dumps(state,sort_keys=True)
  if key not in states:states[key]=len(palette);palette.append(state)
  blocks.append({'pos':(9,(3,[c['x']-minx,c['y']-1,c['z']-minz])),'state':(3,states[key])})
 root={'DataVersion':(3,read(ROOT/'vendor/vanilla/version.json')['data_version']),'size':(9,(3,[max(c['x'] for c in cells)-minx+1,max(c['y'] for c in cells),max(c['z'] for c in cells)-minz+1])),'palette':(9,(10,palette)),'blocks':(9,(10,blocks)),'entities':(9,(10,[]))}
 return gzip.compress(b'\x0a\x00\x00'+payload(10,root),mtime=0)
def landmark(name):
 cells={}
 def put(x,y,z,block,layers=0):
  assert abs(x)<=5 and abs(z)<=5 and 1<=y<=24
  cells[x,y,z]={'x':x,'y':y,'z':z,'block':block,'layers':layers}
 shrine=name.endswith('shrine');wood=name in ['woodland-shrine','timber-ruin']
 stone='MOSSY_COBBLESTONE' if wood else 'DIORITE';pillar='SPRUCE_LOG' if wood else 'STONE_BRICKS'
 # A compact composition with an approach, low garden stones and a walkable opening.
 for x in range(-2,3):
  for z in range(-1,3):put(x,1,z,stone)
 for z in [-4,-3,-2]:put(0,1,z,'ANDESITE')
 for x in [-3,3]:
  for z in [-1,2]:
   height=4 if shrine else (3 if x<0 else 2)
   for y in range(1,height+1):put(x,y,z,pillar)
   put(x,height+1,z,'SNOW',3)
 if shrine:
  for x in range(-3,4):
   for z in range(-1,3):
    y=5+(1 if abs(x)<2 else 0);put(x,y,z,'SPRUCE_PLANKS' if wood else 'DIORITE');put(x,y+1,z,'SNOW',3)
  put(0,2,1,'JACK_O_LANTERN');put(0,3,1,'LIGHT_BLUE_STAINED_GLASS');put(0,4,1,'WHITE_STAINED_GLASS')
 else:
  for x in range(-2,3):put(x,2,2,stone)
  put(-2,3,2,'SPRUCE_PLANKS' if wood else 'MOSSY_STONE_BRICKS');put(-2,4,2,'SNOW',3)
  put(2,3,2,'LIGHT_BLUE_STAINED_GLASS')
 for x,z in [(-4,-3),(4,-2)]:
  put(x,1,z,'COBBLESTONE');put(x,2,z,'SPRUCE_LEAVES' if wood else 'SNOW_BLOCK');put(x,3,z,'SNOW',3)
 return {'format':1,'id':name,'source':'MidgardWinterWorld original composition','license':'PROJECT','cells':list(cells.values())}

def plans():return [gate(7),gate(9)]+[landmark(n) for n in ['frost-shrine','woodland-shrine','stone-ruin','timber-ruin']]
def validate():
 results=[]
 for expected in plans():
  name=expected['id'];p=ROOT/f'plugin/src/main/resources/schematics/{name}.json';assert read(p)==expected,p
  dest=ROOT/f'storybook/data/midgard/structure/{name.replace("-","_")}.nbt';actual=nbt.NBT(gzip.decompress(dest.read_bytes())).root();assert actual==nbt.NBT(gzip.decompress(structure(expected))).root()
  assert not actual['entities'];assert all('nbt' not in row for row in actual['blocks']);assert len(actual['blocks'])<=512
  results.append({'id':expected['id'],'license':expected['license'],'blocks':len(expected['cells']),'dimensions':actual['size'],'structure_sha256':hashlib.sha256(dest.read_bytes()).hexdigest()})
 write(ROOT/'reports/schematic-validation.json',{'status':'PASS','source_license':'CC0-1.0','source_commit':'1263463cffb59b08275e9ee9408848285c4cddff','source':'https://github.com/Greaby/minecraft-circles-schematics','scope':'Two pinned CC0 gate outlines and four original project landmarks. Ordinary-material palette. No generic runtime import/WorldEdit requirement.','assets':results})
 print('PASS: 2 pinned CC0 gates +4 original landmarks; ordinary materials; no entities/block entities/air clearing')
def build():
 for plan in plans():
  name=plan['id'];write(ROOT/f'plugin/src/main/resources/schematics/{name}.json',plan);p=ROOT/f'storybook/data/midgard/structure/{name.replace("-","_")}.nbt';p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(structure(plan))
 dest=ROOT/'plugin/src/main/resources/licenses/greaby-circles-CC0.txt';dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes((SOURCE/'LICENSE').read_bytes())
 validate()
if __name__=='__main__':build()
