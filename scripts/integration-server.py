from pathlib import Path
import subprocess,threading,queue,time,os,shutil,json
root=Path(__file__).resolve().parents[1];variant=os.environ.get('QA_VARIANT','worldedit');folder=root/('.runtime/fawe-integration' if variant=='fawe' else '.runtime/integration');prefix=os.environ.get('QA_PREFIX',('fawe-' if variant=='fawe' else ''));suffix=os.environ.get('QA_SUFFIX','')
if suffix:
 assert suffix.replace('-','').isalnum();folder=folder.with_name(folder.name+'-'+suffix)
for name in ['MidgardWinterWorld.jar','WinterProbe.jar']:shutil.copyfile(root/'dist'/name,folder/'plugins'/name)
from urllib.parse import urlsplit
p=urlsplit(os.environ['HTTPS_PROXY']);flags=[f'-Dhttps.proxyHost={p.hostname}',f'-Dhttps.proxyPort={p.port}',f'-Dhttp.proxyHost={p.hostname}',f'-Dhttp.proxyPort={p.port}','-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
log=(root/('reports/logs/'+prefix+'integration-console.log')).open('w');q=queue.Queue()
proc=subprocess.Popen(['/workspace/tools/jdk-21.0.9+10/bin/java',*flags,'-Xms512M','-Xmx'+os.environ.get('QA_XMX','4G'),'-jar','paper.jar','--nogui'],cwd=folder,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1)
def read():
 for line in proc.stdout:log.write(line);log.flush();q.put(line)
threading.Thread(target=read,daemon=True).start()
def send(s):proc.stdin.write(s+'\n');proc.stdin.flush()
def wait(token,timeout=300):
 end=time.monotonic()+timeout
 while time.monotonic()<end:
  try:line=q.get(timeout=1)
  except queue.Empty:
   if proc.poll() is not None:raise RuntimeError('Server exited '+str(proc.returncode))
   continue
  if 'NATURALLUNA_FAILED' in line or 'WINTERAUDIT_FAILED' in line:raise RuntimeError(line.strip())
  if token in line:return
 raise TimeoutError(token)
try:
 wait('Done (');send('winterworld status');send('datapack list enabled');send(os.environ.get('QA_COMMAND','winterprobe audit'));wait('WINTERAUDIT_DONE',1200)
 shutil.copyfile(folder/'plugins/WinterProbe/runtime-audit.json',root/('reports/'+prefix+'runtime-audit.json'))
 assert all(c['status']=='PASS' for c in json.loads((folder/'plugins/WinterProbe/runtime-audit.json').read_text())['checks'])
 if os.environ.get('QA_NATURAL'):
  send('winterprobe natural');wait('NATURALLUNA_READY',360);natural=json.loads((folder/'plugins/WinterProbe/natural-luna.json').read_text())
 clientlog=(root/('reports/logs/'+prefix+'client-smoke.log')).open('w');client_env=os.environ.copy();client_env['QA_PORT']='25577' if variant=='fawe' else '25576';client_env['QA_REPORT']=prefix+'client-smoke.json';
 if os.environ.get('QA_NATURAL'):client_env['QA_TARGET']=json.dumps({k:natural[k] for k in ('x','y','z')})
 client=subprocess.Popen(['node',str(root/'scripts/client-smoke.cjs')],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1,env=client_env)
 for line in client.stdout:
  clientlog.write(line);clientlog.flush()
  if 'CLIENT_READY' in line:send('op WinterQA');send('winterprobe natural-teleport' if os.environ.get('QA_NATURAL') else 'winterprobe teleport');send('tell WinterQA QA_GO')
 client.wait(timeout=100);clientlog.close();assert client.returncode==0,"Real client smoke failed"
 if os.environ.get('QA_NATURAL'):
  send('winterprobe natural-end');wait('NATURALLUNA_COMPLETE',30);shutil.copyfile(folder/'plugins/WinterProbe/natural-luna.json',root/('reports/'+prefix+'natural-luna.json'))
 send('chunky world winter_test');send('chunky center 4096 4096');send('chunky radius 64');send('chunky start');wait('Task finished',180)
 send('winterworld status');send('winterworld benchmark');send('save-all flush');send('stop');proc.wait(timeout=120)
 print('Integration server exited',proc.returncode,'client exited',client.returncode)
finally:
 if proc.poll() is None:
  send('stop')
  try:proc.wait(timeout=60)
  except subprocess.TimeoutExpired:proc.terminate();proc.wait(timeout=15)
 log.close()
