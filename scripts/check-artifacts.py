#!/usr/bin/env python3
"""Validate distributed plugin/pack structure and record hashes, without running a server."""
from pathlib import Path
import hashlib,json,zipfile,struct
root=Path(__file__).resolve().parents[1];dist=root/'dist';result={'status':'PASS','minecraft':'1.21.11','java_class_major':65,'artifacts':[]}
for name in ['MidgardWinterWorld.jar','WinterProbe.jar','winter-datapack.zip','storybook-datapack.zip']:
 p=dist/name
 with zipfile.ZipFile(p) as z:
  assert z.testzip() is None,name
  names=z.namelist()
  if name.endswith('.jar'):
   classes=[n for n in names if n.endswith('.class')];assert classes
   assert all(n.startswith('ru/midgard/winter/') for n in classes), 'Unexpected embedded API/server class'
   for n in classes:
    data=z.read(n);assert data[:4]==b'\xca\xfe\xba\xbe' and struct.unpack('>H',data[6:8])[0]==65
    assert b'net/minecraft/' not in data and b'org/bukkit/craftbukkit/' not in data, 'Production NMS/CraftBukkit reference'
   assert 'version: 0.3.0' in z.read('plugin.yml').decode() or "version: '0.3.0'" in z.read('plugin.yml').decode()
  else:
   assert len([n for n in names if '/worldgen/biome/' in n and n.endswith('.json')])==23
   assert not any('/dimension/' in n or '/noise_settings/' in n or '/structure/' in n for n in names)
 result['artifacts'].append({'name':name,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'zip_crc':'PASS'})
(root/'reports/artifact-check.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
