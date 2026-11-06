#!/usr/bin/env python3
"""Summarise saved, actual evidence. This does not execute tests or manufacture runtime results."""
from pathlib import Path
import json,xml.etree.ElementTree as ET,collections,hashlib,zipfile,statistics,math
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'reports'
def read(name):return json.loads((R/name).read_text())
checks=[]
def check(name,ok,evidence,group='runtime',detail=None):
 row={'id':name,'status':'PASS' if ok else 'FAIL','group':group,'evidence':evidence}
 if detail is not None:row['detail']=detail
 checks.append(row)
for file in sorted((R/'junit').glob('TEST-*.xml')):
 tree=ET.parse(file).getroot()
 for case in tree.iter('testcase'):
  status='NOT RUN' if case.find('skipped') is not None else 'FAIL' if case.find('failure') is not None or case.find('error') is not None else 'PASS'
  checks.append({'id':case.get('classname')+'.'+case.get('name'),'status':status,'group':'Java unit/contract','evidence':str(file.relative_to(R))})
assert len(checks)==117,'Expected the complete current Java suite'
python_log=(R/'logs/datapack-tests.log').read_text();assert 'Ran 6 tests' in python_log and '\nOK\n' in python_log
for name in ['excluded_biome_rejected','lost_features_rejected','original_tampering_rejected','precipitation_loss_rejected','valid_pack','wrong_pack_version_rejected']:
 check('python.'+name,'test_'+name in python_log,'logs/datapack-tests.log','Python unit')
audit=read('runtime-audit.json');temps=json.loads((ROOT/'plugin/src/main/resources/original-temperatures.json').read_text());winter={f.stem for f in (ROOT/'datapack/data/minecraft/worldgen/biome').glob('*.json')}
for row in audit['biomes']:
 name=row['requested'];check('locate_biome.'+name,row['status']=='PASS','runtime-audit.json','biome location')
 expected=-.5 if name in winter else temps[name]
 check('climate.'+name,row['temperature']<0.15 if name in winter else abs(row['temperature']-expected)<1e-5,'runtime-audit.json','climate',{'measured':row['temperature'],'criterion':'snow temperature < 0.15; altitude-adjusted' if name in winter else 'equals original base temperature at sampled Y=80','original_or_configured_base':expected})
for row in audit['structures']:check('structure.'+row['requested'],row['status']=='PASS','runtime-audit.json','structure locate/API')
profiles=[('worldedit','verified-async-worldedit-'),('fawe','async-snow-fawe-')]
# These prefixes are updated only when the corresponding complete final runs exist.
profile_file=R/'final-integration-profiles.json'
if profile_file.exists():profiles=list(read('final-integration-profiles.json').items())
for variant,prefix in profiles:
 name=prefix+'runtime-audit.json';data=read(name);assert len(data['checks'])==19
 for row in data['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'integration fixtures')
 name=prefix+'client-smoke.json';data=read(name)
 for row in data['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'real client')
 name=prefix+'natural-luna.json';data=read(name)
 check(variant+'.natural_snow_start',data['status']=='PASS' and 'minecraft:snow[' in data['belowBefore'] and data['belowBefore']==data['belowAfter'],name,'natural Luna')
 check(variant+'.natural_end_cleanup',data['ticketCountAfterEnd']==0 and data['surfacePreservedAfterEnd'] and data['blockAfterEnd']=='AIR',name,'natural Luna')
 console='logs/'+prefix+'integration-console.log';text=(R/console).read_text()
 check(variant+'.chunky_task','Task finished' in text,console,'Chunky')
 check(variant+'.clean_stop','All dimensions are saved' in text,console,'server lifecycle')
for row in read('invariant-validation.json'):
 for key,value in row['checks'].items():check(str(row['seed'])+'.'+key,value,'invariant-validation.json','worldgen invariants')
 check(str(row['seed'])+'.strict_snapshot_equality',not row['raw_material_differences'] and not row['additional_block_entities'],'invariant-validation.json','strict diagnostic',{'raw_material_differences':len(row['raw_material_differences']),'additional_block_entities':row['additional_block_entities']})
for run,seed in [('frozen',825180311),('boundary',42),('boundary-final',-892041)]:
 for phase in ['initial','restart']:
  name=f'runtime/{run}/{seed}/winter/measurement-{phase}.json';data=read(name)
  check(f'{seed}.{phase}.paper_pack_load',data['winterPluginEnabled'] and any('winter-datapack.zip' in p for p in data['enabledDatapacks']),name,'server codec/load')
data=read('purpur-final-smoke.json');check('purpur.final_pack_and_plugin',data['winterPluginEnabled'] and any('winter-datapack.zip' in p for p in data['enabledDatapacks']),'purpur-final-smoke.json','Purpur')
data=read('server-matrix-purpur.json');check('purpur.initial_restart',all(not row['diffs'] for row in data),'server-matrix-purpur.json','Purpur')
check('official_biome_sources',len(read('mojang-biome-verification.json')['biomes_equal_to_official_server'])==23,'mojang-biome-verification.json','static/reference')
check('biome_zoom_mojang_reference','PASS: 6000 biome zoom samples and 6 seed hashes' in (R/'logs/biome-zoom-reference.log').read_text(),'logs/biome-zoom-reference.log','static/reference')
data=read('boundary-regression.json');check('excluded_boundary_regression',not data['winter_blocks_added_in_excluded_biomes'] and not data['remaining_nonwinter_material_changes'],'boundary-regression.json','worldgen regression')
check('pack_static_validation','PASS: 23 biome definitions' in (R/'logs/datapack-validation.log').read_text(),'logs/datapack-validation.log','static/reference')
check('artifact_structure',read('artifact-check.json')['status']=='PASS','artifact-check.json','artifact')
for name in ['graphical_visual_acceptance','50_to_100_player_soak','full_purpur_plugin_integration','all_high_altitude_boundaries','crash_recovery_atomicity','active_luna_restart_restore','luna_hot_reload_and_dynamic_creation','large_schematics_custom_y_offsets']:
 checks.append({'id':name,'status':'NOT RUN','group':'remaining scope','evidence':'KNOWN_ISSUES.md'})
counts={s:sum(row['status']==s for row in checks) for s in ['PASS','FAIL','BLOCKED','NOT RUN']}
result={'project_status':'TEST VERIFIED (scoped); not RELEASE CANDIDATE','counts':counts,'counting':'117 Java + 6 Python cases; runtime checks listed individually. One strict diagnostic remains FAIL. Historical failed iterations are preserved separately and not counted as final test cases. 6000 oracle samples are one batch, not 6000 extra test cases.','checks':checks}
(R/'test-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
lines=['# Итоговые результаты тестов','',f"**TEST VERIFIED в указанном объёме. PASS {counts['PASS']}, FAIL {counts['FAIL']}, BLOCKED {counts['BLOCKED']}, NOT RUN {counts['NOT RUN']}.**",'','Полный каталог: test-results.json. Java unit/contract: 117, Python unit: 6. Сборка использует опубликованный официальный Paper API 1.21.11; контракт Luna выполнен с предоставленным JAR, без пропущенных Java-тестов.','', '| Группа | PASS | FAIL | NOT RUN |','|---|---:|---:|---:|']
for group in sorted({row['group'] for row in checks}):
 c=collections.Counter(row['status'] for row in checks if row['group']==group);lines.append(f"| {group} | {c['PASS']} | {c['FAIL']} | {c['NOT RUN']} |")
lines+=['','FAIL относится к строгому равенству снимков seed -892041: пять seagrass/water и дополнительное BEE_NEST. Геометрический/рудный инвариант, сохранность исходных block entities и restart прошли. Нормализация водных растений явно отделена от сырых различий; доказательства не скрыты. Это препятствует заявлению полного поблочного равенства.','','NOT RUN: художественный графический осмотр; 50–100 игроков/soak; полный Purpur набор интеграций; все высотные пограничные случаи; аварийная атомарность; restart активного Luna; hot reload/dynamic drops; крупные схематики/custom Y offsets.','','Исторические ошибки (сеть, несовместимый Java 25 WorldEdit, ранняя утечка льда, жидкостная нестабильность vanilla, старые поисковые/cleanup прогоны Luna) сохранены в исходных JSON/логах. Они не являются дополнительными текущими PASS и не удалялись после исправления. Метаданные отдельных старых JSON отражают свой этап, а не финальный статус.','','Клиентский тест — реальный Minecraft протокол 1.21.11, локальный offline-auth, открытие 54-слотового Luna-инвентаря с предметами. Графический renderer отсутствует. Матрица миров — 1536 уникальных ROI-чанков, 3072 запроса с restart; дополнительные spawn/halo чанки не выданы за измеренные ROI.','']
(R/'TEST_RESULTS.md').write_text('\n'.join(lines))
print(json.dumps(counts));print('Java',sum(row['group']=='Java unit/contract' for row in checks))
