#!/usr/bin/env python3
"""Run one actual survival grief/build/explosion test in the owned visual QA world."""
from pathlib import Path
import argparse,json,os,subprocess
ROOT=Path(__file__).resolve().parents[1];p=argparse.ArgumentParser();p.add_argument('--world',required=True);p.add_argument('--target',type=Path,required=True);a=p.parse_args();assert a.world.replace('-','').isalnum()
target=json.loads(a.target.read_text());control=ROOT/'.runtime'/a.world/'commands.txt';assert control.exists()
def send(text):
 with control.open('a') as f:f.write(text+'\n')
env=os.environ.copy();env['QA_GRIEF_TARGET']=json.dumps(target)
with (ROOT/'reports/logs/anarchy05-grief-client.log').open('w') as log:
 proc=subprocess.Popen(['node',str(ROOT/'scripts/anarchy-client.cjs')],cwd=ROOT,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1)
 for line in proc.stdout:
  log.write(line);log.flush()
  if 'GRIEF_READY' in line:
   x,y,z=target['player'];send(f'deop WinterGrief\ngamemode survival WinterGrief\ngive WinterGrief stone 3\ntp WinterGrief {x} {y} {z}\ntell WinterGrief GRIEF_GO')
  if 'GRIEF_TNT_READY' in line:
   x,y,z=target['block'];send(f'tp WinterGrief {x+12} {y+2} {z+12}\nsummon minecraft:tnt {x+.5} {y+1} {z+.5} {{fuse:40}}')
 assert proc.wait(timeout=15)==0,'Actual grief client failed; see retained log'
ops=json.loads((control.parent/'ops.json').read_text());assert all(row['name']!='WinterGrief' for row in ops)
report=ROOT/'reports/anarchy05-grief-client.json';data=json.loads(report.read_text());data['checks'].append({'name':'non_operator_verified','status':'PASS','detail':'WinterGrief absent from actual server ops.json after deop'})
report.write_text(json.dumps(data,indent=2)+'\n');print('ANARCHY_CLIENT_COMPLETE')
