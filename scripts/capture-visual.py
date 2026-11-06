#!/usr/bin/env python3
"""Press Minecraft's own F2 and copy its original PNG unchanged."""
from pathlib import Path
import argparse,subprocess,os,time,json,hashlib,datetime,struct,shutil
ROOT=Path(__file__).resolve().parents[1];folder=ROOT/'.runtime/visual-client';screens=folder/'game/screenshots';out=ROOT/'reports/screenshots';out.mkdir(exist_ok=True)
p=argparse.ArgumentParser();p.add_argument('name');p.add_argument('--gallery',default='screenshots');p.add_argument('--position',nargs=5,type=float,required=True,metavar=('X','Y','Z','YAW','PITCH'));p.add_argument('--note',required=True);p.add_argument('--existing',type=Path);a=p.parse_args()
assert a.gallery.replace('-','').isalnum();out=ROOT/'reports'/a.gallery;out.mkdir(exist_ok=True)
assert a.name.replace('-','').isalnum();target=out/(a.name+'.png');assert not target.exists()
if a.existing:source=a.existing
else:
 previous=set(screens.glob('*.png'));env=os.environ.copy();env.update(DISPLAY=':99',LD_LIBRARY_PATH='/workspace/tools/x11/usr/lib/x86_64-linux-gnu')
 subprocess.run(['/workspace/tools/x11/usr/bin/xdotool','key','F2'],check=True,env=env)
 deadline=time.monotonic()+30;source=None
 while time.monotonic()<deadline:
  added=set(screens.glob('*.png'))-previous
  if added:
   candidate=max(added,key=lambda f:f.stat().st_mtime)
   # llvmpipe may create the file well before Minecraft finishes encoding the native PNG.
   content=candidate.read_bytes()
   if content[:8]==b'\x89PNG\r\n\x1a\n' and content[-12:]==b'\x00\x00\x00\x00IEND\xaeB`\x82':
    source=candidate;break
  time.sleep(.1)
 assert source is not None,'No native F2 screenshot created'
content=source.read_bytes();assert content[:8]==b'\x89PNG\r\n\x1a\n';size=struct.unpack('>II',content[16:24]);assert size==(1600,900)
shutil.copyfile(source,target);assert hashlib.sha256(target.read_bytes()).digest()==hashlib.sha256(content).digest()
manifest=folder/(a.gallery+'-manifest.json');data=json.loads(manifest.read_text()) if manifest.exists() else []
data.append({'file':target.name,'native_F2_file':source.name,'resolution':list(size),'sha256':hashlib.sha256(content).hexdigest(),'captured_utc':datetime.datetime.fromtimestamp(source.stat().st_mtime,datetime.timezone.utc).isoformat(),'camera_xyz_yaw_pitch':a.position,'note':a.note,'postprocessing':'none; byte-identical copy of Minecraft F2 PNG'})
manifest.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n');print(target)
