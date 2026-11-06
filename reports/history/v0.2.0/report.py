#!/usr/bin/env python3
"""Summarise saved actual 0.2.0 evidence. Does not execute tests or manufacture results."""
from pathlib import Path
import collections, hashlib, json, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]; R=ROOT/'reports'
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
java_count=len(checks);assert java_count==146
log=(R/'logs/datapack-tests.log').read_text();assert 'Ran 6 tests' in log and '\nOK\n' in log
for name in ['excluded_biome_rejected','lost_features_rejected','original_tampering_rejected','precipitation_loss_rejected','valid_pack','wrong_pack_version_rejected']:
 check('python.'+name,'test_'+name in log,'logs/datapack-tests.log','Python unit')
profiles={'worldedit':'fairy02-final-worldedit-','fawe':'fairy02-final-fawe-'}
(R/'final-integration-profiles.json').write_text(json.dumps(profiles,indent=2)+'\n')
for variant,prefix in profiles.items():
 name=prefix+'runtime-audit.json';data=read(name);assert len(data['checks'])==19
 for row in data['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'integration fixtures')
 name=prefix+'client-smoke.json';data=read(name)
 for row in data['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'real protocol client')
 name=prefix+'natural-luna.json';data=read(name)
 check(variant+'.natural_snow_start',data['status']=='PASS' and 'minecraft:snow[' in data['belowBefore'] and data['belowBefore']==data['belowAfter'],name,'natural Luna')
 check(variant+'.natural_end_cleanup',data['ticketCountAfterEnd']==0 and data['surfacePreservedAfterEnd'] and data['blockAfterEnd']=='AIR',name,'natural Luna')
 console='logs/'+prefix+'integration-console.log';log=(R/console).read_text()
 check(variant+'.chunky_task','Task finished' in log,console,'server lifecycle')
 check(variant+'.clean_stop','All dimensions are saved' in log,console,'server lifecycle')
for row in read('fairy02-final-invariants.json'):
 seed=str(row['seed']);evidence='fairy02-final-invariants.json'
 for key,diff in row['final_vanilla_winter_differences'].items():check(seed+'.after_restart_vanilla_winter.'+key,not diff,evidence,'worldgen final comparison',diff)
 check(seed+'.excluded_biome_surface',not row['excluded_biome_surface_differences'],evidence,'worldgen final comparison')
 for mode,diffs in row['restart_differences'].items():
  # Raw hashes and geometric representation are one diagnostic case, not duplicate FAILs.
  check(seed+'.'+mode+'.strict_restart',not diffs['rawUndergroundBlockStateHash'] and not diffs['belowSurfaceGeometry'],evidence,'strict restart diagnostic',diffs)
  check(seed+'.'+mode+'.surface_and_markers_restart',not diffs['surfaceByBiome'] and not diffs['blockEntities'] and not diffs['winterMarker'],evidence,'worldgen persistence')
 for phase in ['initial','restart']:
  name=row['source']+'/winter/measurement-'+phase+'.json';data=read(name)
  check(seed+'.'+phase+'.paper_load',data['winterPluginEnabled'] and any('winter-datapack.zip' in p for p in data['enabledDatapacks']),name,'server codec/load')
name='fairy02-purpur-smoke.json';data=read(name)
check('purpur.current_plugin_load',data['winterPluginEnabled'] and data['minecraft']=='1.21.11' and len(data['chunks'])==4 and any('winter-datapack.zip' in p for p in data['enabledDatapacks']),name,'Purpur smoke')
log=(R/'logs/fairy02-purpur-runner.log').read_text()
check('purpur.vanilla_control_restart',"AssertionError: ('vanilla', 825180311, 'restart mismatch', 'surfaceByBiome')" not in log,'logs/fairy02-purpur-runner.log','strict restart diagnostic','Separate matrix stopped in vanilla control; no claim of a full Purpur matrix.')
check('official_biome_sources',len(read('mojang-biome-verification.json')['biomes_equal_to_official_server'])==23,'mojang-biome-verification.json','unchanged climate/reference')
check('biome_zoom_mojang_reference','PASS: 6000 biome zoom samples and 6 seed hashes' in (R/'logs/biome-zoom-reference.log').read_text(),'logs/biome-zoom-reference.log','unchanged climate/reference')
comparison=read('fairy02-climate-content-comparison.json')
check('climate_content_matches_v0_1_frozen_matrix',comparison['current_sha256']==hashlib.sha256((ROOT/'dist/winter-datapack.zip').read_bytes()).hexdigest() and comparison['same_entries'] and not comparison['content_differences'],'fairy02-climate-content-comparison.json','unchanged climate/reference','Compared to the actual retained v0.1 frozen-matrix pack. All entry bytes identical; rebuilt ZIP timestamps differ, so archive SHA256 differs.')
check('pack_static_validation','PASS: 23 biome definitions' in (R/'logs/datapack-validation.log').read_text(),'logs/datapack-validation.log','unchanged climate/reference')
check('artifact_structure',read('artifact-check.json')['status']=='PASS','artifact-check.json','artifacts')
name='screenshots-v2/provenance.json';data=read(name)
assert data['plugin_sha256']==hashlib.sha256((ROOT/'dist/MidgardWinterWorld.jar').read_bytes()).hexdigest()
for shot in data['screenshots']:
 p=R/'screenshots-v2'/shot['file'];source=ROOT/'.runtime/visual-client/game/screenshots'/shot['native_F2_file']
 # The source is private local QA data; packaged provenance retains its name and hash.
 ok=hashlib.sha256(p.read_bytes()).hexdigest()==shot['sha256']
 if source.exists():ok=ok and p.read_bytes()==source.read_bytes()
 check('native_F2.'+shot['file'],ok,name,'graphical samples')
name='fairy02-visual-metrics.json';data=read(name)
for feature in ['snow-conifers','snow-deciduous','clearings-objects','trail-blocks','canopy-snow','drift-blocks']:
 check('procedural_visual_world.'+feature,data['metrics'].get(feature,0)>0,name,'graphical world generation')
for name in ['exhaustive_visual_acceptance','50_to_100_player_soak','full_purpur_plugin_integration','all_high_altitude_boundaries','crash_recovery_atomicity','active_luna_restart_restore','luna_hot_reload_and_dynamic_creation','large_schematics_custom_y_offsets']:
 checks.append({'id':name,'status':'NOT RUN','group':'remaining scope','evidence':'KNOWN_ISSUES.md'})
counts={s:sum(row['status']==s for row in checks) for s in ['PASS','FAIL','BLOCKED','NOT RUN']}
result={'version':'0.2.0','project_status':'TEST VERIFIED (scoped); not RELEASE CANDIDATE','counts':counts,'counting':f'{java_count} Java + 6 Python cases. Current Paper integrations and two-seed 320 ROI/variant comparison. Two strict diagnostics remain FAIL: winter seed 42 initial/restart raw states; separate Purpur vanilla-control surface restart. Superseded implementation failures and v0.1 evidence are retained, not counted as current PASS. Graphical checks verify native F2 files and sample generated features, not exhaustive artistic acceptance.','checks':checks}
(R/'test-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
lines=['# Результаты 0.2.0','',f"**TEST VERIFIED в указанном объёме: PASS {counts['PASS']}, FAIL {counts['FAIL']}, BLOCKED {counts['BLOCKED']}, NOT RUN {counts['NOT RUN']}.**",'',f'{java_count} Java unit/contract и 6 Python unit, без skipped. Официальный опубликованный Paper API 1.21.11; Luna contract использует предоставленный JAR. Полный каталог: test-results.json.','', '| Группа | PASS | FAIL | NOT RUN |','|---|---:|---:|---:|']
for group in sorted({row['group'] for row in checks}):
 c=collections.Counter(row['status'] for row in checks if row['group']==group);lines.append(f"| {group} | {c['PASS']} | {c['FAIL']} | {c['NOT RUN']} |")
lines+=['','Два FAIL оставлены явно:','','1. Paper seed 42: initial/restart меняет raw state hash в шести чанках; геометрический fingerprint меняется в одном, material delta GLOW_LICHEN −1/AIR +1. Поверхность, PDC и block entities стабильны. После одинакового restart raw underground hashes всех 256 зимних ROI точно совпадают с vanilla. Полное равенство initial/restart не заявляется.','2. Отдельная Purpur-матрица остановилась на несовпадении surfaceByBiome в vanilla-контроле, до запуска зимней ветки. Дополнительный smoke текущего плагина на Purpur 2568/четырёх чанках прошёл; полной матрицей это не считается.','','Финальная Paper-матрица: seeds825180311/42, 64+256 ROI на вариант, 640 уникальных ROI и 1280 запросов с restart. Сохранённые vanilla-контроли использованы повторно; зимние ветки запущены с текущим JAR. После restart underground raw states, геометрия, структуры и block entities совпадают во всех 320 парах; поверхность исключённых биомов также совпадает. Сравнение подземелья ограничено Y −64..min(31,surface−8).','','Настоящие интеграции Paper: WorldGuard+Luna+WorldEdit 7.3.19 и отдельно FAWE 2.16.2; в каждой 19 fixtures,3 protocol-client проверки, снежная природная площадка, End cleanup0tickets, Chunky и чистый stop. Графический официальный клиент 1.21.11 дал 5 неизменённых F2 PNG. Полной художественной приёмки всех модулей и нагрузочного прогона 50–100 игроков нет.','','Исторические неудачные варианты с постоянными tickets, прежние seed и их ошибки сохранены. Они не объявляются текущими PASS.','']
(R/'TEST_RESULTS.md').write_text('\n'.join(lines))
print(json.dumps(counts));print('Java',java_count)
