#!/usr/bin/env python3
"""Diagnostic map drawn from real chunk audit data, NOT a Minecraft screenshot."""
from pathlib import Path
import json,csv,html
r=Path(__file__).resolve().parents[1];audit=json.loads((r/'reports/runtime-audit.json').read_text());lines=['# Реальные тестовые координаты','',f"Мир `winter_test`, seed `{audit['seed']}`. Координаты получены от настоящего сервера Paper 1.21.11. Перед осмотром подземных объектов используйте spectator.",'','| Тип | ID | X | Y | Z |','|---|---|---|---|---|'];rows=[]
for kind,items in [('biome',audit['biomes']),('structure',audit['structures'])]:
 for item in items:
  if all(k in item for k in ('x','y','z')):
   rows.append([kind,item['requested'],'winter_test',item['x'],item['y'],item['z'],item.get('status')]);lines.append('| '+' | '.join(map(str,[kind,item['requested'],item['x'],item['y'],item['z']]))+' |')
(r/'reports/COORDINATES.md').write_text('\n'.join(lines)+'\n')
with (r/'reports/test-coordinates.csv').open('w') as f:
 writer=csv.writer(f);writer.writerow(['type','id','world','x','y','z','status']);writer.writerows(rows)
svg=['<svg xmlns="http://www.w3.org/2000/svg" width="850" height="470" viewBox="0 0 850 470">','<rect width="850" height="470" fill="#101923"/>','<g font-family="sans-serif" fill="#e9f0f7"><text x="24" y="30" font-size="20">Real chunk audit: seed 825180311</text><text x="24" y="52" font-size="12">Diagnostic data map; not a Minecraft screenshot. Each cell = 16 x 16 blocks.</text>']
for panel,mode in enumerate(['vanilla','winter']):
 p=r/'.runtime/frozen/825180311'/mode/'measurement-restart.json';data=json.loads(p.read_text());ox=24+panel*410;svg.append(f'<text x="{ox}" y="82" font-size="16">{mode}</text>')
 for c in data['chunks']:
  x=ox+(c['x']-128)*23;y=100+(c['z']-128)*20;amount=min(1,c['snowBlocks']/256);value=int(55+amount*190);color=f'#{value:02x}{min(255,value+7):02x}{min(255,value+12):02x}'
  text=html.escape(f"chunk {c['x']},{c['z']}; snow blocks {c['snowBlocks']}; biomes {', '.join(c['surfaceByBiome'])}")
  svg.append(f'<rect x="{x}" y="{y}" width="22" height="19" fill="{color}"><title>{text}</title></rect>')
 svg.append(f'<text x="{ox}" y="445" font-size="12">X/Z chunks 128..143; brightness = observed snow count</text>')
svg.append('</g></svg>');(r/'reports/world-map.svg').write_text('\n'.join(svg))
