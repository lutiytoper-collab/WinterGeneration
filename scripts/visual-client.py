#!/usr/bin/env python3
"""Launch the verified, unmodified Mojang client against the local offline QA server."""
from pathlib import Path
import os,json,subprocess,hashlib,uuid,argparse
from urllib.parse import urlsplit
p=argparse.ArgumentParser();p.add_argument('--log-prefix',default='visual');a=p.parse_args();assert a.log_prefix.replace('-','').isalnum()
ROOT=Path(__file__).resolve().parents[1];folder=ROOT/'.runtime/visual-client';game=folder/'game';game.mkdir(exist_ok=True)
(game/'options.txt').write_text('version:4671\nautoJump:false\nenableVsync:false\nmaxFps:30\nrenderDistance:10\nsimulationDistance:5\ngraphicsMode:1\ncloudStatus:0\nguiScale:3\ngamma:0.6\nfov:0.0\nbobView:false\nparticles:1\nsoundCategory_master:0.0\nlang:ru_ru\npauseOnLostFocus:false\nonboardAccessibility:false\nskipMultiplayerWarning:true\ntutorialStep:none\nnarrator:0\n')
proxy=urlsplit(os.environ['HTTPS_PROXY']);flags=[f'-Dhttps.proxyHost={proxy.hostname}',f'-Dhttps.proxyPort={proxy.port}',f'-Dhttp.proxyHost={proxy.hostname}',f'-Dhttp.proxyPort={proxy.port}','-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
identifier=str(uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:WinterVisual').digest(),version=3));native=str(folder/'natives')
command=['/workspace/tools/jdk-21.0.9+10/bin/java',*flags,'-Xmx3G',f'-Djava.library.path={native}',f'-Dorg.lwjgl.system.SharedLibraryExtractPath={native}','-Dminecraft.launcher.brand=MidgardVisualQA','-Dminecraft.launcher.version=0.1.0','-cp',(folder/'classpath.txt').read_text(),'net.minecraft.client.main.Main','--username','WinterVisual','--version','1.21.11','--gameDir',str(game),'--assetsDir',str(folder/'assets'),'--assetIndex','29','--uuid',identifier,'--accessToken','0','--versionType','release','--width','1600','--height','900','--quickPlayMultiplayer','127.0.0.1:25578']
environment=os.environ.copy();environment.update(DISPLAY=':99',LIBGL_ALWAYS_SOFTWARE='1',LD_LIBRARY_PATH='/workspace/tools/x11/usr/lib/x86_64-linux-gnu',ALSOFT_DRIVERS='null')
with (ROOT/f'reports/logs/{a.log_prefix}-client-console.log').open('w') as log:
 proc=subprocess.Popen(command,cwd=game,env=environment,stdout=log,stderr=subprocess.STDOUT);print('VISUAL_CLIENT_PID',proc.pid,flush=True);proc.wait();print('VISUAL_CLIENT_EXIT',proc.returncode)
