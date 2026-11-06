#!/usr/bin/env python3
"""Read real Anvil NBT; report exact block/material differences without interpreting them as success."""
import argparse,collections,gzip,json,math,pathlib,struct,zlib
class NBT:
 def __init__(self,data):self.data=data;self.pos=0
 def take(self,n):r=self.data[self.pos:self.pos+n];self.pos+=n;return r
 def num(self,fmt):return struct.unpack('>'+fmt,self.take(struct.calcsize('>'+fmt)))[0]
 def text(self):return self.take(self.num('H')).decode('utf-8')
 def payload(self,t):
  if t in (1,2,3,4,5,6):return self.num({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
  if t==7:return self.take(self.num('i'))
  if t==8:return self.text()
  if t==9:
   elem,n=self.num('B'),self.num('i');return [self.payload(elem) for _ in range(n)]
  if t==10:
   result={}
   while (sub:=self.num('B'))!=0:
    name=self.text();result[name]=self.payload(sub)
   return result
  if t in (11,12):return [self.num('i' if t==11 else 'q') for _ in range(self.num('i'))]
  raise ValueError('Unsupported NBT tag '+str(t))
 def root(self):t=self.num('B');self.text();return self.payload(t)
def chunk(folder,cx,cz):
 p=folder/'region'/f'r.{cx//32}.{cz//32}.mca'
 with p.open('rb') as f:
  f.seek(4*((cx%32)+(cz%32)*32));entry=f.read(4);offset=int.from_bytes(entry[:3],'big')
  if not offset:raise ValueError('Missing chunk')
  f.seek(offset*4096);size=int.from_bytes(f.read(4),'big');compression=f.read(1)[0];raw=f.read(size-1)
 data={1:gzip.decompress,2:zlib.decompress,3:lambda b:b}[compression](raw)
 return NBT(data).root()
def blocks(root,max_y=31):
 out={}
 for sec in root['sections']:
  state=sec.get('block_states')
  if not state:continue
  palette=[s['Name'] for s in state['palette']];data=state.get('data');bits=max(4,(len(palette)-1).bit_length());per=64//bits;mask=(1<<bits)-1
  for i in range(4096):
   y=sec['Y']*16+i//256
   if y>max_y:continue
   entry=0 if not data else (data[i//per]>>((i%per)*bits))&mask
   out[(i%16,y,(i//16)%16)]=palette[entry]
 return out
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('baseline',type=pathlib.Path);p.add_argument('winter',type=pathlib.Path);p.add_argument('--max-y',type=int,default=31);p.add_argument('--chunks',nargs='+',required=True);p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args();result=[]
 for pair in a.chunks:
  cx,cz=map(int,pair.split(','));base=blocks(chunk(a.baseline,cx,cz),a.max_y);winter=blocks(chunk(a.winter,cx,cz),a.max_y);diff=[]
  for pos in sorted(set(base)|set(winter)):
   if base.get(pos)!=winter.get(pos):diff.append({'x':cx*16+pos[0],'y':pos[1],'z':cz*16+pos[2],'before':base.get(pos),'after':winter.get(pos)})
  result.append({'chunk':[cx,cz],'changes':diff})
 a.output.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
