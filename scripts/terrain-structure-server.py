#!/usr/bin/env python3
from pathlib import Path
import hashlib,json,os,queue,shutil,subprocess,threading,time
from urllib.parse import urlsplit
ROOT=Path(__file__).resolve().parents[1];folder=ROOT/'.runtime/terrain06-structures';folder.mkdir(exist_ok=False);(folder/'plugins').mkdir()
for n in ['MidgardWinterWorld.jar','WinterProbe.jar']:shutil.copyfile(ROOT/'dist'/n,folder/'plugins'/n)
(folder/'paper.jar').symlink_to(ROOT/'.runtime/downloads/paper-1.21.11-132.jar')
for n in ['cache','libraries','versions']:(folder/n).symlink_to(ROOT/'.runtime/integration'/n,target_is_directory=True)
(folder/'eula.txt').write_text('eula=true\n');(folder/'server.properties').write_text('level-name=winter_test\nlevel-seed=825180311\nserver-ip=127.0.0.1\nserver-port=25581\nonline-mode=true\nview-distance=3\nsimulation-distance=3\npause-when-empty-seconds=-1\n')
p=urlsplit(os.environ['HTTPS_PROXY']);flags=[f'-Dhttps.proxyHost={p.hostname}',f'-Dhttps.proxyPort={p.port}',f'-Dhttp.proxyHost={p.hostname}',f'-Dhttp.proxyPort={p.port}','-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts'];q=queue.Queue()
with (ROOT/'reports/logs/terrain06-structures-console.log').open('w') as log:
 proc=subprocess.Popen(['/workspace/tools/jdk-21.0.9+10/bin/java',*flags,'-Xms512M','-Xmx3G','-jar','paper.jar','--nogui'],cwd=folder,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1)
 def reader():
  for line in proc.stdout:log.write(line);log.flush();q.put(line)
 thread=threading.Thread(target=reader,daemon=True);thread.start()
 def wait(token,timeout):
  end=time.monotonic()+timeout
  while time.monotonic()<end:
   try:line=q.get(timeout=1)
   except queue.Empty:
    if proc.poll() is not None:raise RuntimeError('Server exited '+str(proc.returncode))
    continue
   if token in line:return
  raise TimeoutError(token)
 try:
  wait('Done (',300);proc.stdin.write('winterprobe terrain-audit\n');proc.stdin.flush();wait('TERRAINAUDIT_DONE',1800)
  src=folder/'plugins/WinterProbe/terrain-audit.json';shutil.copyfile(src,ROOT/'reports/terrain06-structures-audit.json');data=json.loads(src.read_text());print('STRUCTURE_AUDIT',len(data['checks']),[r for r in data['checks'] if r['status']!='PASS'],flush=True)
  proc.stdin.write('save-all flush\nstop\n');proc.stdin.flush();proc.wait(timeout=120);assert proc.returncode==0;assert all(r['status']=='PASS' for r in data['checks'])
 finally:
  if proc.poll() is None:proc.terminate();proc.wait(timeout=60)
  thread.join(timeout=10)
