#!/usr/bin/env python3
from pathlib import Path
import shutil,zipfile,hashlib
r=Path(__file__).resolve().parents[1]; d=r/'dist'; d.mkdir(exist_ok=True)
for name in ('config.yml','config-storybook.yml'): shutil.copyfile(r/'plugin/src/main/resources'/name,d/name)
shutil.copyfile(r/'README.md',d/'README.md')
shutil.copytree(r/'scripts',d/'scripts',dirs_exist_ok=True,ignore=shutil.ignore_patterns('__pycache__'))
shutil.copyfile(r/'docs/CHUNKY.md',d/'CHUNKY.md')
gallery=r/'reports/screenshots-storybook'
with zipfile.ZipFile(d/'game-screenshots-storybook-0.3.0.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(gallery.iterdir()):
  if p.is_file():z.write(p,'screenshots-storybook/'+p.name)
bundle={
 'plugins/MidgardWinterWorld.jar':d/'MidgardWinterWorld.jar',
 'plugins/MidgardWinterWorld/config.yml':d/'config-storybook.yml',
 'winter_test/datapacks/storybook-datapack.zip':d/'storybook-datapack.zip',
 'INSTALL-RU.md':r/'docs/STORYBOOK_INSTALL.md'}
with zipfile.ZipFile(d/'Midgard-Storybook-0.3.0.zip','w',zipfile.ZIP_DEFLATED) as z:
 for name,p in bundle.items():z.write(p,name)
 z.writestr('SHA256SUMS',''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+name+'\n' for name,p in bundle.items()))
for name,files in [('reports.zip',list((r/'reports').rglob('*'))),('sources.zip',list(r.rglob('*')))]:
 with zipfile.ZipFile(d/name,'w',zipfile.ZIP_DEFLATED) as z:
  for p in sorted(files):
   if not p.is_file(): continue
   rel=p.relative_to(r)
   if name=='sources.zip' and any(s in {'.git','.gradle','.runtime','.cache','build','dist','__pycache__'} for s in rel.parts): continue
   z.write(p,rel)
(d/'SHA256SUMS').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in sorted(d.iterdir()) if p.is_file() and p.name!='SHA256SUMS'))
print('Packaged actual files only; plugin JAR exists:',(d/'MidgardWinterWorld.jar').exists())
