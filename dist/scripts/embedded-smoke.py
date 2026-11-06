#!/usr/bin/env python3
"""Exercise one-file install and repeat bootstrap on a real isolated exact-version server."""
from pathlib import Path
from urllib.parse import urlsplit
import argparse,hashlib,json,os,queue,shutil,subprocess,threading,time
ROOT=Path(__file__).resolve().parents[1];p=argparse.ArgumentParser();p.add_argument('--name',required=True);p.add_argument('--server',type=Path,required=True);p.add_argument('--sha256',required=True);p.add_argument('--cache',type=Path,required=True);p.add_argument('--port',type=int,default=25580);p.add_argument('--accept-eula',action='store_true');a=p.parse_args()
assert a.accept_eula,'Explicit EULA acceptance required';assert a.name.replace('-','').isalnum();assert hashlib.sha256(a.server.read_bytes()).hexdigest()==a.sha256
folder=ROOT/'.runtime'/a.name;folder.mkdir(exist_ok=False);(folder/'plugins').mkdir()
for name in ['MidgardWinterWorld.jar','WinterProbe.jar']:shutil.copyfile(ROOT/'dist'/name,folder/'plugins'/name)
(folder/'server.jar').symlink_to(a.server.resolve())
for name in ['cache','libraries','versions']:(folder/name).symlink_to(a.cache.resolve()/name,target_is_directory=True)
(folder/'eula.txt').write_text('eula=true\n');(folder/'server.properties').write_text(f'level-name=winter_test\nlevel-seed=825180311\nserver-ip=127.0.0.1\nserver-port={a.port}\nonline-mode=true\nview-distance=3\nsimulation-distance=3\npause-when-empty-seconds=-1\n')
proxy=urlsplit(os.environ['HTTPS_PROXY']);flags=[f'-Dhttps.proxyHost={proxy.hostname}',f'-Dhttps.proxyPort={proxy.port}',f'-Dhttp.proxyHost={proxy.hostname}',f'-Dhttp.proxyPort={proxy.port}','-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts'];results=[]
for phase in ['initial','restart']:
 q=queue.Queue();log=(ROOT/'reports/logs'/f'{a.name}-{phase}.log').open('w')
 proc=subprocess.Popen(['/workspace/tools/jdk-21.0.9+10/bin/java',*flags,'-Xms512M','-Xmx2G','-jar','server.jar','--nogui'],cwd=folder,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
 def reader():
  for line in proc.stdout:log.write(line);log.flush();q.put(line)
 thread=threading.Thread(target=reader,daemon=True);thread.start()
 def wait(token,timeout=300):
  end=time.monotonic()+timeout
  while time.monotonic()<end:
   try:line=q.get(timeout=1)
   except queue.Empty:
    if proc.poll() is not None:raise RuntimeError(f'Server exited{proc.returncode}')
    continue
   if 'WINTERPROBE_FAILED' in line:raise RuntimeError(line)
   if token in line:return
  raise TimeoutError(token)
 try:
  wait('Done (');proc.stdin.write('tick freeze\nwinterprobe run 2 10 8\n');proc.stdin.flush();wait('WINTERPROBE_DONE',600)
  data=json.loads((folder/'plugins/WinterProbe/measurement.json').read_text());assert data['winterPluginEnabled'] and data['minecraft']=='1.21.11';assert any('MidgardWinterWorld/fantasy' in name for name in data['enabledDatapacks']);assert len(data['chunks'])==4
  out=ROOT/'reports'/f'{a.name}-{phase}.json';out.write_text(json.dumps(data,indent=2)+'\n');results.append(data)
  proc.stdin.write('winterworld status\nwinterworld benchmark\nsave-all flush\nstop\n');proc.stdin.flush();proc.wait(timeout=120);assert proc.returncode==0
 finally:
  if proc.poll() is None:proc.terminate();proc.wait(timeout=60)
  thread.join(timeout=10);log.close()
assert not list((folder/'winter_test/datapacks').glob('*.zip')),'One-JAR test cannot install external packs'
diffs={key:[[a['x'],a['z']] for a,b in zip(results[0]['chunks'],results[1]['chunks']) if a.get(key)!=b.get(key)] for key in ['surfaceByBiome','blockEntities','winterMarker']}
result={'status':'PASS' if not any(diffs.values()) else 'FAIL','one_jar_no_external_pack':True,'rediscovered_on_restart':True,'minecraft':'1.21.11','server':results[1]['server'],'plugin_sha256':hashlib.sha256((folder/'plugins/MidgardWinterWorld.jar').read_bytes()).hexdigest(),'restart_differences':diffs,'roi':4}
(ROOT/'reports'/f'{a.name}-summary.json').write_text(json.dumps(result,indent=2)+'\n');assert result['status']=='PASS';print('EMBEDDED_SMOKE_COMPLETE',result['server'],flush=True)
