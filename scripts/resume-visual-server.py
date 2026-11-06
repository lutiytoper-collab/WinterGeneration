#!/usr/bin/env python3
"""Resume only the existing owned visual QA world after interrupted cloud sessions."""
from pathlib import Path
import subprocess,time,os,hashlib,argparse
from urllib.parse import urlsplit
ROOT=Path(__file__).resolve().parents[1];p=argparse.ArgumentParser();p.add_argument('--name',required=True);p.add_argument('--log-prefix',required=True);a=p.parse_args();assert a.name.replace('-','').isalnum() and a.log_prefix.replace('-','').isalnum()
folder=ROOT/'.runtime'/a.name
assert (folder/'eula.txt').read_text().strip()=='eula=true'
assert 'server-ip=127.0.0.1' in (folder/'server.properties').read_text()
assert hashlib.sha256((folder/'plugins/MidgardWinterWorld.jar').read_bytes()).digest()==hashlib.sha256((ROOT/'dist/MidgardWinterWorld.jar').read_bytes()).digest()
control=folder/'commands.txt';control.write_text('') # Never replay previous placement/explosion/teleport commands.
proxy=urlsplit(os.environ['HTTPS_PROXY']);flags=[f'-Dhttps.proxyHost={proxy.hostname}',f'-Dhttps.proxyPort={proxy.port}',f'-Dhttp.proxyHost={proxy.hostname}',f'-Dhttp.proxyPort={proxy.port}','-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
with (ROOT/f'reports/logs/{a.log_prefix}-server.log').open('w') as log:
 proc=subprocess.Popen(['/workspace/tools/jdk-21.0.9+10/bin/java',*flags,'-Xms512M','-Xmx3G','-jar','paper.jar','--nogui'],cwd=folder,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True);print('VISUAL_SERVER_PID',proc.pid,flush=True)
 try:
  with control.open() as commands:
   while proc.poll() is None:
    command=commands.readline()
    if command:proc.stdin.write(command);proc.stdin.flush()
    else:time.sleep(.2)
 finally:
  if proc.poll() is None:proc.stdin.write('stop\n');proc.stdin.flush();proc.wait(timeout=120)
 print('VISUAL_SERVER_EXIT',proc.returncode)
