#!/usr/bin/env python3
"""Local screenshot server; preserves the original measured QA world by copying it."""
from pathlib import Path
import shutil,subprocess,os,time,json
from urllib.parse import urlsplit
ROOT=Path(__file__).resolve().parents[1];folder=ROOT/'.runtime/visual-server';folder.mkdir(exist_ok=False)
shutil.copytree(ROOT/'.runtime/integration/winter_test',folder/'winter_test')
(folder/'plugins').mkdir();shutil.copyfile(ROOT/'dist/MidgardWinterWorld.jar',folder/'plugins/MidgardWinterWorld.jar');shutil.copyfile(ROOT/'.runtime/downloads/Chunky-Bukkit-1.4.40.jar',folder/'plugins/Chunky.jar')
(folder/'paper.jar').symlink_to(ROOT/'.runtime/downloads/paper-1.21.11-132.jar')
for name in ['cache','libraries','versions']:(folder/name).symlink_to(ROOT/'.runtime/integration'/name,target_is_directory=True)
(folder/'eula.txt').write_text('eula=true\n') # Explicitly accepted for this isolated test project in this conversation.
(folder/'server.properties').write_text('level-name=winter_test\nserver-ip=127.0.0.1\nserver-port=25578\nonline-mode=false\nenforce-secure-profile=false\nview-distance=10\nsimulation-distance=3\nmax-players=2\nlevel-seed=825180311\nallow-nether=false\n')
(folder/'bukkit.yml').write_text('settings:\n  allow-end: false\n')
control=folder/'commands.txt';control.write_text('');proxy=urlsplit(os.environ['HTTPS_PROXY']);flags=[f'-Dhttps.proxyHost={proxy.hostname}',f'-Dhttps.proxyPort={proxy.port}',f'-Dhttp.proxyHost={proxy.hostname}',f'-Dhttp.proxyPort={proxy.port}','-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
with (ROOT/'reports/logs/visual-server-console.log').open('w') as log:
 proc=subprocess.Popen(['/workspace/tools/jdk-21.0.9+10/bin/java',*flags,'-Xms512M','-Xmx3G','-jar','paper.jar','--nogui'],cwd=folder,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True)
 print('VISUAL_SERVER_PID',proc.pid,flush=True)
 try:
  with control.open() as commands:
   while proc.poll() is None:
    command=commands.readline()
    if command:proc.stdin.write(command);proc.stdin.flush()
    else:time.sleep(.2)
 finally:
  if proc.poll() is None:proc.stdin.write('stop\n');proc.stdin.flush();proc.wait(timeout=120)
 print('VISUAL_SERVER_EXIT',proc.returncode)
