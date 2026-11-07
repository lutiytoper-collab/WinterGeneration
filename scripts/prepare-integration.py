#!/usr/bin/env python3
"""Prepare isolated QA folders from user-supplied/verified JARs; never overwrite an existing run."""
import argparse,pathlib,shutil,zipfile,re,json
ROOT=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--paper',type=pathlib.Path,required=True);p.add_argument('--luna',type=pathlib.Path,required=True);p.add_argument('--worldguard',type=pathlib.Path,required=True);p.add_argument('--worldedit',type=pathlib.Path,required=True);p.add_argument('--fawe',type=pathlib.Path);p.add_argument('--chunky',type=pathlib.Path,required=True);p.add_argument('--natural-snow',action='store_true',help='Single-chest QA profile: default radius 2 (5x5), max-y 280, native SNOW allowed, winter-biome search');p.add_argument('--platform-radius',type=int,choices=[1,2],default=2);p.add_argument('--natural-field-center',nargs=2,type=int,metavar=('X','Z'),help='Optional native field QA window: radius 24 using public Luna X/Z offsets; no terrain edits');p.add_argument('--accept-eula',action='store_true');p.add_argument('--suffix',default='');p.add_argument('--storybook',action='store_true');p.add_argument('--embedded',action='store_true');a=p.parse_args()
if a.natural_field_center and (not a.natural_snow or max(map(abs,a.natural_field_center))>100000):p.error('Field center requires --natural-snow and bounded coordinates')
if a.suffix and not re.fullmatch(r'[a-z0-9-]+',a.suffix):p.error('Invalid suffix')
if not a.accept_eula:p.error('Read https://aka.ms/MinecraftEULA and accept explicitly')
for source in [a.paper,a.luna,a.worldguard,a.worldedit,a.chunky,*([a.fawe] if a.fawe else [])]:
 if not source.is_file():p.error(f'Missing JAR: {source}')
for variant,port,editor in [('integration',25576,a.worldedit),*([('fawe-integration',25577,a.fawe)] if a.fawe else [])]:
 folder=ROOT/'.runtime'/(variant+('-'+a.suffix if a.suffix else ''))
 if folder.exists():raise SystemExit(f'Existing run preserved: {folder}; archive it first or use its existing configuration')
 (folder/'plugins/LunaAirDrop/airdrops').mkdir(parents=True);(folder/'winter_test/datapacks').mkdir(parents=True)
 shutil.copyfile(a.paper,folder/'paper.jar')
 for source,name in [(a.luna,'LunaAirDrop.jar'),(a.worldguard,'WorldGuard.jar'),(a.chunky,'Chunky.jar'),(editor,'WorldEdit.jar' if variant=='integration' else 'FastAsyncWorldEdit.jar'),(ROOT/'dist/MidgardWinterWorld.jar','MidgardWinterWorld.jar'),(ROOT/'dist/WinterProbe.jar','WinterProbe.jar')]:shutil.copyfile(source,folder/'plugins'/name)
 if not a.embedded:
  shutil.copyfile(ROOT/('dist/storybook-datapack.zip' if a.storybook else 'dist/winter-datapack.zip'),folder/'winter_test/datapacks'/('storybook-datapack.zip' if a.storybook else 'winter-datapack.zip'))
 (folder/'eula.txt').write_text('eula=true\n');(folder/'server.properties').write_text(f'level-name=winter_test\nlevel-seed=825180311\nserver-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nview-distance=3\nsimulation-distance=3\nmax-players=2\n')
 with zipfile.ZipFile(a.luna) as z:
  data=z.read('airdrops/default.yml').decode();data=re.sub(r'^air-spawn-world:.*$','air-spawn-world: winter_test',data,flags=re.M);data=re.sub(r'^  world:.*$','  world: winter_test',data,flags=re.M);data=re.sub(r'^timeToStart:.*$','timeToStart: 24h',data,flags=re.M);data=re.sub(r'^openingTime:.*$','openingTime: 3s',data,flags=re.M);data=re.sub(r'^min-online-players:.*$','min-online-players: 0',data,flags=re.M)
  (folder/'plugins/LunaAirDrop/airdrops/default.yml').write_text(data)
  cfg=z.read('config.yml').decode().replace('state-serializable: true','state-serializable: false');(folder/'plugins/LunaAirDrop/config.yml').write_text(cfg)
  if a.natural_snow:
   generator=z.read('generators.yml').decode()
   section=re.search(r'(?ms)^black-List:\s*\n(.*?)(?=^[^ #\s]|\Z)',generator)
   if not section:raise SystemExit('Luna black-List section not found; refusing to guess')
   generator=generator[:section.start()]+re.sub(r'(?m)^\s*- SNOW\s*\n','',section.group())+generator[section.end():]
   generator=generator.replace('black-List:\n','black-List:\n'+''.join('  - '+material+'\n' for material in ['GRASS_BLOCK','DIRT','COARSE_DIRT','PODZOL','ROOTED_DIRT','STONE','ANDESITE','GRANITE','DIORITE','DEEPSLATE']),1)
   generator=re.sub(r'max-y:\s*85', 'max-y: 280',generator,count=1)
   if a.natural_field_center:
    x,z=a.natural_field_center;generator,count=re.subn(r'(?m)(^      offsets:\n        x:) 0(\n        y: 1\n        z:) 0$',lambda m:m[1]+' '+str(x)+m[2]+' '+str(z),generator,count=1)
    if count!=1:raise SystemExit('Expected first Luna NORMAL offsets not found')
    (folder/'plugins/WinterProbe').mkdir(exist_ok=True);(folder/'plugins/WinterProbe/natural-luna-area.json').write_text(json.dumps({'spawnRadius':24,'centerX':x,'centerZ':z,'selection':'Public Luna random XZ bounds and generator offsets within a 64x64 natural meadow; unmodified native terrain'},indent=2)+'\n')
   all_biomes=json.loads((ROOT/'plugin/src/main/resources/original-temperatures.json').read_text())
   winter={f.stem for f in (ROOT/'datapack/data/minecraft/worldgen/biome').glob('*.json')}
   excluded=[str(key).removeprefix('minecraft:').upper() for key in all_biomes if str(key).removeprefix('minecraft:') not in winter]
   generator=re.sub(r'(?ms)^black-List-biome:\s*\n.*?(?=^[^ #\s]|\Z)','black-List-biome:\n'+''.join('  - '+key+'\n' for key in sorted(excluded)),generator,count=1)
   (folder/'plugins/LunaAirDrop/generators.yml').write_text(generator)
   (folder/'plugins/MidgardWinterWorld').mkdir()
   winter_cfg=(ROOT/'plugin/src/main/resources'/('config-storybook.yml' if a.storybook else 'config.yml')).read_text().replace('platform-radius: 2',f'platform-radius: {a.platform_radius}')
   (folder/'plugins/MidgardWinterWorld/config.yml').write_text(winter_cfg)
 if (a.storybook or a.embedded) and not a.natural_snow:
  (folder/'plugins/MidgardWinterWorld').mkdir(exist_ok=True);shutil.copyfile(ROOT/'plugin/src/main/resources/config-storybook.yml',folder/'plugins/MidgardWinterWorld/config.yml')
 print('Prepared',folder,'loopback only; no production world used')
