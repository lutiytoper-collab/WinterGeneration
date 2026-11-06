#!/usr/bin/env python3
"""Fetch exact official Mojang client/assets for local visual QA, verifying every published SHA-1."""
from pathlib import Path
import json,hashlib,urllib.request,concurrent.futures,time,zipfile,os
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'.runtime/visual-client';OUT.mkdir(parents=True,exist_ok=True)
metadata=ROOT/'.runtime/downloads/minecraft-1.21.11.json';manifest=json.loads((ROOT/'.runtime/downloads/version_manifest.json').read_text())
version=next(v for v in manifest['versions'] if v['id']=='1.21.11')
assert hashlib.sha1(metadata.read_bytes()).hexdigest()==version['sha1']
data=json.loads(metadata.read_text());assert data['id']=='1.21.11'
def fetch(url,path,sha1):
 if path.is_file() and hashlib.sha1(path.read_bytes()).hexdigest()==sha1:return
 path.parent.mkdir(parents=True,exist_ok=True)
 for attempt in range(4):
  try:
   request=urllib.request.Request(url,headers={'User-Agent':'MidgardWinterWorld-VisualQA/0.1.0'})
   with urllib.request.urlopen(request,timeout=60) as response:content=response.read()
   assert hashlib.sha1(content).hexdigest()==sha1,'Official SHA-1 mismatch: '+url
   temp=path.with_suffix(path.suffix+'.partial');temp.write_bytes(content);temp.replace(path);return
  except Exception:
   if attempt==3:raise
   time.sleep(1+attempt)
client=data['downloads']['client'];fetch(client['url'],OUT/'client.jar',client['sha1'])
index=data['assetIndex'];indexfile=OUT/'assets/indexes'/f"{index['id']}.json";fetch(index['url'],indexfile,index['sha1'])
def allowed(library):
 if 'rules' not in library:return True
 result=False
 for rule in library['rules']:
  target=rule.get('os',{});match=target.get('name','linux')=='linux' and target.get('arch','x86_64') in ('x86_64','amd64')
  if match:result=rule['action']=='allow'
 return result
jobs=[];classpath=[OUT/'client.jar']
for library in data['libraries']:
 if not allowed(library):continue
 artifact=library['downloads'].get('artifact')
 if artifact:
  path=OUT/'libraries'/artifact['path'];jobs.append((artifact['url'],path,artifact['sha1']));classpath.append(path)
objects=json.loads(indexfile.read_text())['objects']
for entry in {entry['hash']:entry for entry in objects.values()}.values():
 h=entry['hash'];jobs.append((f'https://resources.download.minecraft.net/{h[:2]}/{h}',OUT/'assets/objects'/h[:2]/h,h))
with concurrent.futures.ThreadPoolExecutor(max_workers=12) as executor:
 futures=[executor.submit(fetch,*job) for job in jobs]
 for i,future in enumerate(concurrent.futures.as_completed(futures),1):
  future.result()
  if i%500==0:print('Verified',i,'/',len(jobs),flush=True)
native=OUT/'natives';native.mkdir(exist_ok=True)
for file in classpath[1:]:
 if 'natives-linux' not in file.name:continue
 with zipfile.ZipFile(file) as archive:
  for name in archive.namelist():
   if name.endswith('.so'):(native/Path(name).name).write_bytes(archive.read(name))
(OUT/'classpath.txt').write_text(os.pathsep.join(map(str,classpath)))
report={'minecraft':'1.21.11','client_url':client['url'],'client_sha1':client['sha1'],'asset_index_sha1':index['sha1'],'verified_library_files':len(classpath)-1,'verified_asset_objects':len({e['hash'] for e in objects.values()}),'scope':'Official unmodified client; isolated offline-auth QA server only; no Microsoft login credentials supplied'}
(OUT/'download-provenance.json').write_text(json.dumps(report,indent=2)+'\n');print(report)
