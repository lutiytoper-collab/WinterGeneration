#!/usr/bin/env python3
"""Validate distributed plugin/pack structure and record hashes, without running a server."""
from pathlib import Path
import hashlib,json,zipfile,struct
root=Path(__file__).resolve().parents[1];dist=root/'dist';result={'status':'PASS','minecraft':'1.21.11','java_class_major':65,'artifacts':[]}
for name in ['MidgardWinterWorld.jar','WinterProbe.jar','winter-datapack.zip','storybook-datapack.zip','fantasy-terrain-datapack.zip']:
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
   assert 'version: 0.7.0' in z.read('plugin.yml').decode() or "version: '0.7.0'" in z.read('plugin.yml').decode()
   if name=='MidgardWinterWorld.jar':
    assert 'bootstrapper: ru.midgard.winter.WinterBootstrap' in z.read('paper-plugin.yml').decode()
    assert 'fantasy-pack/pack.mcmeta' in names
    assert len([n for n in names if n.startswith('fantasy-pack/data/minecraft/worldgen/biome/') and n.endswith('.json')])==23
    assert 'fantasy-terrain-pack/data/minecraft/worldgen/noise_settings/overworld.json' in names
    assert not any(n.startswith('fantasy-terrain-pack/data/minecraft/tags/') and n.endswith('.json') for n in names)
    assert not any(n.startswith('fantasy-pack/data/minecraft/worldgen/noise_settings/') for n in names)
    templates=['moon-gate-7','moon-gate-9','frost-shrine','woodland-shrine','stone-ruin','timber-ruin']
    palette=set()
    for template in templates:
     file='schematics/'+template+'.json';data=json.loads(z.read(file))
     assert z.read(file)==(root/'plugin/src/main/resources'/file).read_bytes()
     nbt='data/midgard/structure/'+template.replace('-','_')+'.nbt'
     assert z.read('fantasy-pack/'+nbt)==(root/'storybook'/nbt).read_bytes()
     palette.update(c['block'] for c in data['cells'])
    forbidden={'DIAMOND_BLOCK','EMERALD_BLOCK','GOLD_BLOCK','IRON_BLOCK','COPPER_BLOCK','NETHERITE_BLOCK','LAPIS_BLOCK','REDSTONE_BLOCK','AMETHYST_BLOCK','AMETHYST_CLUSTER','SEA_LANTERN','PEARLESCENT_FROGLIGHT','BLUE_ICE','PACKED_ICE','LANTERN','SOUL_LANTERN','END_ROD','CHEST','SPAWNER'}
    assert not palette&forbidden
    assert z.read('licenses/greaby-circles-CC0.txt')==(root/'vendor/schematics/greaby-circles/LICENSE').read_bytes()
    result['landmark_palette']=sorted(palette);result['landmark_templates']=templates
  elif name=='fantasy-terrain-datapack.zip':
   assert 'data/minecraft/worldgen/noise_settings/overworld.json' in names
   assert len([n for n in names if n.startswith('data/minecraft/')])==36
   assert len([n for n in names if n.startswith('data/minecraft/worldgen/biome/')])==23
   assert not any('/tags/' in n for n in names)
   with zipfile.ZipFile(root/'dist/MidgardWinterWorld.jar') as main:
    for n in names:
     if n!='pack.mcmeta':assert z.read(n)==main.read('fantasy-terrain-pack/'+n),n
  else:
   assert len([n for n in names if '/worldgen/biome/' in n and n.endswith('.json')])==23
   assert not any('/dimension/' in n or '/noise_settings/' in n or '/worldgen/structure/' in n for n in names)
 result['artifacts'].append({'name':name,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'zip_crc':'PASS'})
(root/'reports/artifact-check.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
