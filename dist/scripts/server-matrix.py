#!/usr/bin/env python3
"""Real isolated vanilla/winter baseline and restart runs; never creates results without running servers."""
import argparse,hashlib,json,os,pathlib,queue,shutil,statistics,subprocess,threading,time,sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
from urllib.parse import urlsplit
proxy=urlsplit(os.environ.get('HTTPS_PROXY',''))
java_flags=['-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
if proxy.hostname:
 java_flags += [f'-Dhttps.proxyHost={proxy.hostname}',f'-Dhttps.proxyPort={proxy.port or 8080}',f'-Dhttp.proxyHost={proxy.hostname}',f'-Dhttp.proxyPort={proxy.port or 8080}']
p=argparse.ArgumentParser();p.add_argument('--paper',type=pathlib.Path,required=True);p.add_argument('--sha256',required=True);p.add_argument('--accept-eula',action='store_true');p.add_argument('--port',type=int,default=25575);p.add_argument('--resume',action='store_true');p.add_argument('--side',type=int,default=8);p.add_argument('--runtime-cache',type=pathlib.Path);p.add_argument('--run-name',default='matrix');p.add_argument('--seeds',nargs='+',type=int,default=[825180311,42,-892041]);p.add_argument('--storybook',action='store_true');p.add_argument('--embedded',action='store_true');a=p.parse_args()
pack_name='storybook-datapack.zip' if a.storybook else 'winter-datapack.zip'
if not __import__('re').fullmatch(r'[A-Za-z0-9_-]+',a.run_name):p.error('Invalid run name')
if not a.accept_eula:p.error('Read https://aka.ms/MinecraftEULA and explicitly accept with --accept-eula')
if not 1<=a.side<=16:p.error('side must be 1..16')
if hashlib.sha256(a.paper.read_bytes()).hexdigest()!=a.sha256:p.error('Official Paper checksum mismatch')
for artifact in ['WinterProbe.jar','MidgardWinterWorld.jar',pack_name]:
 if not (ROOT/'dist'/artifact).is_file():p.error('Missing artifact: '+artifact)
java=pathlib.Path('/workspace/tools/jdk-21.0.9+10/bin/java')
def run(folder,phase):
 logfile=folder/f'console-{phase}.log';lines=queue.Queue()
 with logfile.open('w') as log:
  proc=subprocess.Popen([str(java),*java_flags,'-Xms512M','-Xmx2G','-jar','paper.jar','--nogui'],cwd=folder,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1)
  def reader():
   for line in proc.stdout:log.write(line);log.flush();lines.put(line)
  thread=threading.Thread(target=reader,daemon=True);thread.start()
  def waitfor(token,timeout):
   deadline=time.monotonic()+timeout
   while time.monotonic()<deadline:
    try:line=lines.get(timeout=1)
    except queue.Empty:
     if proc.poll() is not None:raise RuntimeError(f'Server exited {proc.returncode}: {logfile}')
     continue
    if 'WINTERPROBE_FAILED' in line:raise RuntimeError(line.strip())
    if token in line:return
   raise TimeoutError(f'{token}: {logfile}')
  try:
   waitfor('Done (',240);proc.stdin.write('tick freeze\n');proc.stdin.flush();proc.stdin.write(f'winterprobe run {a.side} 128 128\n');proc.stdin.flush();waitfor('WINTERPROBE_DONE',1200)
   result=json.loads((folder/'plugins/WinterProbe/measurement.json').read_text())
   assert result['minecraft']=='1.21.11' and len(result['chunks'])==a.side*a.side
   (folder/f'measurement-{phase}.json').write_text(json.dumps(result,indent=2))
   proc.stdin.write('winterworld status\nwinterworld benchmark\nsave-all flush\nstop\n');proc.stdin.flush();proc.wait(timeout=120)
   if proc.returncode!=0:raise RuntimeError(f'Server exit status {proc.returncode}')
   return result
  finally:
   if proc.poll() is None:
    proc.terminate()
    try:proc.wait(timeout=30)
    except subprocess.TimeoutExpired:proc.kill();proc.wait()
   thread.join(timeout=5)
results=[]
for seed in a.seeds:
 pair={}
 for mode in ['vanilla','winter']:
  folder=ROOT/'.runtime'/a.run_name/str(seed)/mode
  if folder.exists() and not a.resume:raise RuntimeError(f'Preserving existing run: {folder}; use a new seed or explicit --resume')
  if folder.exists() and a.resume:
   assert hashlib.sha256((folder/'paper.jar').read_bytes()).hexdigest()==a.sha256
   if mode=='winter':assert hashlib.sha256((folder/'plugins/MidgardWinterWorld.jar').read_bytes()).hexdigest()==hashlib.sha256((ROOT/'dist/MidgardWinterWorld.jar').read_bytes()).hexdigest()
   for phase in ('initial','restart'):assert 'The game is frozen' in (folder/f'console-{phase}.log').read_text()
   initial=json.loads((folder/'measurement-initial.json').read_text());restart=json.loads((folder/'measurement-restart.json').read_text())
  else:
   (folder/'plugins').mkdir(parents=True)
   if a.runtime_cache:
    for directory in ('cache','libraries','versions'):
     source=a.runtime_cache.resolve()/directory
     if source.is_dir():(folder/directory).symlink_to(source,target_is_directory=True)
   shutil.copyfile(a.paper,folder/'paper.jar');shutil.copyfile(ROOT/'dist/WinterProbe.jar',folder/'plugins/WinterProbe.jar')
   if mode=='winter':
    (folder/'winter_test/datapacks').mkdir(parents=True)
    if not a.embedded:shutil.copyfile(ROOT/'dist'/pack_name,folder/'winter_test/datapacks'/pack_name)
    shutil.copyfile(ROOT/'dist/MidgardWinterWorld.jar',folder/'plugins/MidgardWinterWorld.jar')
    if a.storybook:
     (folder/'plugins/MidgardWinterWorld').mkdir();shutil.copyfile(ROOT/'plugin/src/main/resources/config-storybook.yml',folder/'plugins/MidgardWinterWorld/config.yml')
   (folder/'eula.txt').write_text('eula=true\n');(folder/'server.properties').write_text(f'level-name=winter_test\nlevel-seed={seed}\nserver-ip=127.0.0.1\nserver-port={a.port}\nonline-mode=true\nview-distance=3\nsimulation-distance=3\n')
   initial=run(folder,'initial');restart=run(folder,'restart')
  for measured in (initial,restart):
   if mode=='winter':
    assert measured['winterPluginEnabled'],'Winter plugin failed to enable'
    assert any(('MidgardWinterWorld/fantasy' if a.embedded else pack_name) in p for p in measured['enabledDatapacks']),'Winter datapack not enabled'
    assert all('winterMarker' in c for c in measured['chunks']),'Winter processing never ran'
   else:assert not measured['winterPluginEnabled'],'Baseline must not contain winter plugin'
  for before,after in zip(initial['chunks'],restart['chunks']):
   for key in ['x','z','belowSurfaceGeometry','surfaceByBiome','blockEntities','winterMarker']:
    assert before.get(key)==after.get(key),(mode,seed,'restart mismatch',key)
  import re
  project=lambda rows:[re.sub(r'(minY|maxY)=-?[0-9.]+',r'\1=<refreshed>',s) for s in rows]
  for before,after in zip(initial['chunks'],restart['chunks']):
   assert project(before['structures'])==project(after['structures']),'Structure type/XZ changed after restart'
  pair[mode]={'initial':initial,'restart':restart}
 diffs=[]
 for base,winter in zip(pair['vanilla']['restart']['chunks'],pair['winter']['restart']['chunks']):
  assert (base['x'],base['z'])==(winter['x'],winter['z'])
  for key in ['belowSurfaceGeometry','structures','blockEntities']:
   if base[key]!=winter[key]:diffs.append({'chunk':[base['x'],base['z']],'field':key})
 summary={'seed':seed,'diffs':diffs,'timings':{},'metadata_y_refreshes':[]}
 for mode,runs in pair.items():
  for before,after in zip(runs['initial']['chunks'],runs['restart']['chunks']):
   if before['structures']!=after['structures']:summary['metadata_y_refreshes'].append({'mode':mode,'chunk':[before['x'],before['z']],'before':before['structures'],'after':after['structures']})
 for mode,runs in pair.items():
  r=runs['initial']
  values=sorted(c['requestMillis'] for c in r['chunks']);summary['timings'][mode]={'chunks':len(values),'mean_ms':statistics.mean(values),'median_ms':statistics.median(values),'p95_ms':values[__import__('math').ceil(.95*len(values))-1],'max_ms':max(values),'heapStart':r['heapStart'],'heapEnd':r['heapEnd']}
 results.append(summary)
 out=ROOT/'reports'/f'server-matrix-{a.run_name}.json';out.write_text(json.dumps(results,indent=2)+'\n')
 if diffs:raise RuntimeError(f'Real baseline differences require investigation: {out}')
print('Real server matrix completed:',ROOT/'reports'/f'server-matrix-{a.run_name}.json')
