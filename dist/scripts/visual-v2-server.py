#!/usr/bin/env python3
"""Fresh seed-matched visual QA world; does not regenerate any existing user or baseline chunks."""
from pathlib import Path
import shutil,subprocess,os,time,argparse
from urllib.parse import urlsplit
ROOT=Path(__file__).resolve().parents[1];p=argparse.ArgumentParser();p.add_argument('--name',default='visual-v2');p.add_argument('--storybook',action='store_true');p.add_argument('--embedded',action='store_true');a=p.parse_args();assert a.name.replace('-','').isalnum();folder=ROOT/'.runtime'/a.name;folder.mkdir(exist_ok=False)
(folder/'plugins').mkdir();(folder/'winter_test/datapacks').mkdir(parents=True)
for name in ['MidgardWinterWorld.jar','WinterProbe.jar']:shutil.copyfile(ROOT/'dist'/name,folder/'plugins'/name)
shutil.copyfile(ROOT/'.runtime/downloads/Chunky-Bukkit-1.4.40.jar',folder/'plugins/Chunky.jar')
if not a.embedded:shutil.copyfile(ROOT/('dist/storybook-datapack.zip' if a.storybook else 'dist/winter-datapack.zip'),folder/'winter_test/datapacks'/('storybook-datapack.zip' if a.storybook else 'winter-datapack.zip'))
if a.storybook and not a.embedded:
 (folder/'plugins/MidgardWinterWorld').mkdir();shutil.copyfile(ROOT/'plugin/src/main/resources/config-storybook.yml',folder/'plugins/MidgardWinterWorld/config.yml')
(folder/'paper.jar').symlink_to(ROOT/'.runtime/downloads/paper-1.21.11-132.jar')
for name in ['cache','libraries','versions']:(folder/name).symlink_to(ROOT/'.runtime/integration'/name,target_is_directory=True)
(folder/'eula.txt').write_text('eula=true\n')
(folder/'server.properties').write_text('level-name=winter_test\nserver-ip=127.0.0.1\nserver-port=25578\nonline-mode=false\nenforce-secure-profile=false\nview-distance=10\nsimulation-distance=3\nmax-players=2\nlevel-seed=825180311\nallow-nether=false\n')
(folder/'bukkit.yml').write_text('settings:\n  allow-end: false\n');control=folder/'commands.txt';control.write_text('');proxy=urlsplit(os.environ['HTTPS_PROXY']);flags=[f'-Dhttps.proxyHost={proxy.hostname}',f'-Dhttps.proxyPort={proxy.port}',f'-Dhttp.proxyHost={proxy.hostname}',f'-Dhttp.proxyPort={proxy.port}','-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
with (ROOT/f'reports/logs/{a.name}-console.log').open('w') as log:
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
