#!/usr/bin/env python3
"""Summarise actual 0.3.0 Storybook evidence. Historical failures remain in their own records."""
from pathlib import Path
import collections,hashlib,json,re,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'reports'
def read(name):return json.loads((R/name).read_text())
checks=[]
def check(name,ok,evidence,group,detail=None):
 row={'id':name,'status':'PASS' if ok else 'FAIL','group':group,'evidence':evidence}
 if detail is not None:row['detail']=detail
 checks.append(row)
for file in sorted((R/'junit').glob('TEST-*.xml')):
 for case in ET.parse(file).getroot().iter('testcase'):
  status='NOT RUN' if case.find('skipped') is not None else 'FAIL' if case.find('failure') is not None or case.find('error') is not None else 'PASS'
  checks.append({'id':case.get('classname')+'.'+case.get('name'),'status':status,'group':'Java unit/contract','evidence':str(file.relative_to(R))})
java_count=len(checks);assert java_count==149
for name,n in [('logs/datapack-tests.log',6),('logs/story03-datapack-tests.log',10)]:
 log=(R/name).read_text();assert f'Ran {n} tests' in log and '\nOK\n' in log
 cases=re.findall(r'^(test_\w+)\s+\(',log,re.M);assert len(cases)==n
 for case in cases:check('python.'+case,True,name,'Python unit')
winter={p.stem for p in (ROOT/'datapack/data/minecraft/worldgen/biome').glob('*.json')};temps=json.loads((ROOT/'plugin/src/main/resources/original-temperatures.json').read_text())
profiles={'worldedit':'story03-worldedit-','fawe':'story03-fawe-'}
(R/'final-integration-profiles.json').write_text(json.dumps(profiles,indent=2)+'\n')
for variant,prefix in profiles.items():
 name=prefix+'runtime-audit.json';data=read(name);assert len(data['checks'])==19
 for row in data['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'integration fixtures')
 for row in data['biomes']:
  biome=row['requested'];check(variant+'.biome.'+biome,row['status']=='PASS',name,'biome location')
  ok=row['temperature']<.15 if biome in winter else abs(row['temperature']-temps[biome])<1e-5
  check(variant+'.climate.'+biome,ok,name,'climate runtime',{'temperature':row['temperature'],'original_base':temps[biome]})
 for row in data['structures']:check(variant+'.structure.'+row['requested'],row['status']=='PASS',name,'structure locate/API')
 name=prefix+'client-smoke.json';data=read(name)
 for row in data['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'real protocol client')
 name=prefix+'natural-luna.json';data=read(name)
 check(variant+'.natural_snow_start',data['status']=='PASS' and 'minecraft:snow[' in data['belowBefore'] and data['belowBefore']==data['belowAfter'],name,'natural Luna')
 check(variant+'.natural_end_cleanup',data['ticketCountAfterEnd']==0 and data['surfacePreservedAfterEnd'] and data['blockAfterEnd']=='AIR',name,'natural Luna')
 console='logs/'+prefix+'integration-console.log';log=(R/console).read_text()
 check(variant+'.chunky','Task finished' in log,console,'server lifecycle')
 check(variant+'.clean_stop','All dimensions are saved' in log,console,'server lifecycle')
for run,seed in [('story03-matrix',825180311),('story03-boundary',42)]:
 name=run+'-coordinate-invariants.json';data=read(name)
 for key,ok in data['checks'].items():check(str(seed)+'.'+key,ok,name,'same-coordinate worldgen',{'common_coordinates':data['common_coordinates'],'roi':data['chunks']})
 initial={};restart={}
 for mode in ['vanilla','winter']:
  initial[mode]=read(f'runtime/v0.3.0/{seed}/{mode}/measurement-initial.json');restart[mode]=read(f'runtime/v0.3.0/{seed}/{mode}/measurement-restart.json')
  changed={key:[[a['x'],a['z']] for a,b in zip(initial[mode]['chunks'],restart[mode]['chunks']) if a.get(key)!=b.get(key)] for key in ['rawUndergroundBlockStateHash','belowSurfaceGeometry','surfaceByBiome','blockEntities','winterMarker']}
  check(str(seed)+'.'+mode+'.restart',not any(changed.values()),f'runtime/v0.3.0/{seed}/{mode}/measurement-restart.json','restart persistence',changed)
 pairs=list(zip(restart['vanilla']['chunks'],restart['winter']['chunks']))
 diff=[[a['x'],a['z']] for a,b in pairs if a['belowSurfaceGeometry']!=b['belowSurfaceGeometry'] or a['rawUndergroundBlockStateHash']!=b['rawUndergroundBlockStateHash']]
 check(str(seed)+'.original_variable_window_probe',not diff,'server-matrix-'+run+'.json','raw variable-window diagnostic',{'different_chunk_hashes':diff,'interpretation':'Original diagnostic retained. Tall trunks change height-selected sampling windows; independent same-coordinate check is separate.'})
 for phase in ['initial','restart']:
  name=f'runtime/v0.3.0/{seed}/winter/measurement-{phase}.json';d=read(name)
  check(str(seed)+'.'+phase+'.storybook_load',d['winterPluginEnabled'] and any('storybook-datapack.zip' in p for p in d['enabledDatapacks']),name,'server codec/load')
name='story03-excluded-surface.json';d=read(name);check('excluded_surface_seed42',not d['excluded_surface_hash_differences'],name,'boundary sample')
name='story03-purpur-smoke.json';d=read(name);check('purpur.current_native_pack_load',d['winterPluginEnabled'] and d['minecraft']=='1.21.11' and len(d['chunks'])==4 and any('storybook-datapack.zip' in p for p in d['enabledDatapacks']),name,'Purpur smoke')
check('storybook_static',read('storybook-static-validation.json')['status']=='PASS','storybook-static-validation.json','native pack/reference')
check('official_biome_sources',len(read('mojang-biome-verification.json')['biomes_equal_to_official_server'])==23,'mojang-biome-verification.json','native pack/reference')
check('artifact_structure',read('artifact-check.json')['status']=='PASS','artifact-check.json','artifacts')
check('runtime_artifact_identity',all(v['identical'] for v in read('story03-runtime-artifact-identity.json')),'story03-runtime-artifact-identity.json','artifacts')
name='screenshots-storybook/provenance.json';data=read(name)
assert data['plugin_sha256']==hashlib.sha256((ROOT/'dist/MidgardWinterWorld.jar').read_bytes()).hexdigest()
assert data['storybook_datapack_sha256']==hashlib.sha256((ROOT/'dist/storybook-datapack.zip').read_bytes()).hexdigest()
for shot in data['screenshots']:
 p=R/'screenshots-storybook'/shot['file'];native=ROOT/'.runtime/visual-client/game/screenshots'/shot['native_F2_file'];b=p.read_bytes();ok=hashlib.sha256(b).hexdigest()==shot['sha256'] and b[-12:]==b'\x00\x00\x00\x00IEND\xaeB`\x82'
 if native.exists():ok=ok and b==native.read_bytes()
 check('native_F2.'+shot['file'],ok,name,'graphical samples')
for name in ['exhaustive_visual_acceptance','50_to_100_player_soak','full_purpur_plugin_integration','all_cross_biome_crown_edges','all_high_altitude_boundaries','crash_recovery_atomicity','active_luna_restart_restore','luna_hot_reload_and_dynamic_creation','large_schematics_custom_y_offsets','survival_economy_balance','crystal_runtime_visual_acceptance']:
 checks.append({'id':name,'status':'NOT RUN','group':'remaining scope','evidence':'KNOWN_ISSUES.md'})
counts={s:sum(row['status']==s for row in checks) for s in ['PASS','FAIL','BLOCKED','NOT RUN']}
result={'version':'0.3.0 Storybook','project_status':'TEST VERIFIED (scoped); not RELEASE CANDIDATE','counts':counts,'counting':f'{java_count} Java +16Python; both complete current Paper audits/integrations;2seed/320pairs; native F2 file checks. Raw variable-window FAIL is retained separately from successful identical-coordinate/ore comparisons. Historical corrected failures are not counted as current PASS.','checks':checks}
(R/'test-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
lines=['# Результаты0.3.0 Storybook','',f"**PASS {counts['PASS']}, FAIL {counts['FAIL']}, BLOCKED {counts['BLOCKED']}, NOT RUN {counts['NOT RUN']}. TEST VERIFIED только в указанном объёме.**",'',f'{java_count} Java unit/contract,6 климатических и10Storybook Python тестов, без skipped. Полный каталог: test-results.json.','', '| Группа | PASS | FAIL | NOT RUN |','|---|---:|---:|---:|']
for group in sorted({row['group'] for row in checks}):
 c=collections.Counter(row['status'] for row in checks if row['group']==group);lines.append(f"| {group} | {c['PASS']} | {c['FAIL']} | {c['NOT RUN']} |")
lines+=['','Оригинальный variable-window probe сохранил FAIL seed825180311 в чанке131,135: разные высоты новых стволов меняют границу выборки MOTION_BLOCKING_NO_LEAVES−8. Числа STONE+9/DIORITE+5 означали дополнительные выбранные позиции, а не замену породы. Точное фиксированное Y≤31 сравнение этого чанка нашло только3AIR→SNOW на открытой поверхности Y31.','','Независимое сравнение **одинаковых координат**, без нормализации materials/states: seeds825180311/42,320пар ROI,7 864 241общая подземная позиция — полное совпадение raw block states. Отдельно все рудные позиции/состояния на всей сохранённой высоте, структуры и block entities совпали. Объём подземелья — пересечение исходных окон от−64доmin(31,baselineHeight−8,winterHeight−8), а не весь объём мира. Сырые переменные fingerprints не переименованы в PASS.','','640уникальных vanilla/winter ROI,1280запросов с restart. Vanilla-контроли использованы повторно; зимние ветки новые с текущим JAR/Storybook. На выбранных256пограничных ROI seed42 поверхность исключённых биомов совпала; все возможные переходы крупных крон не проверены.','','Каждый Paper стек WorldGuard/Luna+WorldEdit7.3.19 либо FAWE2.16.2:37биомов/климат,11типов структур,19fixtures,3реальных protocol-client проверки, природный снег,54слота лута,End cleanup0tickets,Chunky и чистый stop. Purpur2568:4ROI smoke с текущим native pack, полный стек NOT RUN.','','Шесть настоящих F2 PNG сняты официальным1.21.11 клиентом, без шейдеров/обработки. Это осмотр выбранных мест, не исчерпывающая художественная приёмка. Кристаллы проверены unit/preflight, но успешное природное размещение и отдельный игровой кадр кристаллов в этом прогоне не получены.','','Первый native codec запуск отклонён из-за crown_height28 при максимуме24; параметр исправлен до22..24 и финальный pack загрузился на всех площадках. Ошибка сохранена в logs/story03-preview-console.log. Исторические0.1/0.2 и промежуточные неудачи не объявляются новыми PASS.','']
s='\n'.join(lines);s=re.sub(r'(?<=[А-Яа-яЁё])(?=\d)|(?<=\d)(?=[А-Яа-яЁё])',' ',s);(R/'TEST_RESULTS.md').write_text(s)
print(json.dumps(counts));print('Java',java_count)
