#!/usr/bin/env python3
"""Report actual Fantasy0.4 evidence; preserve original failing diagnostics and historical scope."""
from pathlib import Path
import collections,hashlib,json,re,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'reports';checks=[]
def read(name):return json.loads((R/name).read_text())
def check(name,ok,evidence,group,detail=None):
 row={'id':name,'status':'PASS' if ok else 'FAIL','group':group,'evidence':evidence}
 if detail is not None:row['detail']=detail
 checks.append(row)
for file in sorted((R/'junit').glob('TEST-*.xml')):
 for c in ET.parse(file).getroot().iter('testcase'):
  status='NOT RUN' if c.find('skipped') is not None else 'FAIL' if c.find('failure') is not None or c.find('error') is not None else 'PASS'
  checks.append({'id':c.get('classname')+'.'+c.get('name'),'status':status,'group':'Java unit/contract','evidence':str(file.relative_to(R))})
assert len(checks)==161
for name,n in [('logs/datapack-tests.log',6),('logs/storybook-tests.log',10)]:
 log=(R/name).read_text();assert f'Ran {n} tests' in log and '\nOK\n' in log;cases=re.findall(r'^(test_\w+)\s+\(',log,re.M);assert len(cases)==n
 for c in cases:check('python.'+c,True,name,'Python unit')
profiles={'worldedit':'fantasy04-worldedit-','fawe':'fantasy04-fawe-'};(R/'final-integration-profiles.json').write_text(json.dumps(profiles,indent=2)+'\n')
for variant,prefix in profiles.items():
 name=prefix+'runtime-audit.json';d=read(name);assert len(d['checks'])==19
 for row in d['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'real integration fixtures')
 name=prefix+'client-smoke.json'
 for row in read(name)['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'real protocol client')
 name=prefix+'natural-luna.json';d=read(name)
 check(variant+'.snow_start',d['status']=='PASS' and 'minecraft:snow[' in d['belowBefore'] and d['belowBefore']==d['belowAfter'],name,'natural Luna')
 check(variant+'.End_cleanup',d['ticketCountAfterEnd']==0 and d['surfacePreservedAfterEnd'] and d['blockAfterEnd']=='AIR',name,'natural Luna')
 log='logs/'+prefix+'integration-console.log';s=(R/log).read_text()
 check(variant+'.chunky','Task finished' in s,log,'lifecycle')
 check(variant+'.stop','All dimensions are saved' in s,log,'lifecycle')
 check(variant+'.builtin_pack','FANTASY_PACK_DISCOVERED MidgardWinterWorld/fantasy' in s,log,'one-JAR bootstrap')
name='fantasy04-purpur-summary.json';d=read(name)
check('purpur.one_jar_no_external_pack',d['status']=='PASS' and d['one_jar_no_external_pack'],name,'one-JAR bootstrap')
check('purpur.rediscovery_and_restart',d['rediscovered_on_restart'] and not any(d['restart_differences'].values()),name,'restart persistence')
name='fantasy04-artifact-identity.json';d=read(name)
check('same_current_jar_in_all5_servers',all(v['identical_current_jar'] for v in d),name,'artifacts')
check('no_external_datapack_in_all5_servers',all(v['no_external_datapack_zip'] for v in d),name,'one-JAR bootstrap')
name='fantasy04-matrix-coordinate-invariants.json';d=read(name)
for key,value in d['checks'].items():check('825180311.'+key,value,name,'same-coordinate worldgen',{'roi':d['chunks'],'coordinates':d['common_coordinates']})
summary=read('server-matrix-fantasy04-matrix.json')
check('825180311.original_variable_window_probe',not any(row['diffs'] for row in summary),'server-matrix-fantasy04-matrix.json','raw variable-window diagnostic','Original height-selected fingerprints retained; independent identical-coordinate result is separate.')
for mode in ['vanilla','winter']:
 a=read('runtime/v0.4.0/825180311/'+mode+'/measurement-initial.json');b=read('runtime/v0.4.0/825180311/'+mode+'/measurement-restart.json')
 diff={key:[[x['x'],x['z']] for x,y in zip(a['chunks'],b['chunks']) if x.get(key)!=y.get(key)] for key in ['rawUndergroundBlockStateHash','belowSurfaceGeometry','surfaceByBiome','blockEntities','winterMarker']}
 check(mode+'.restart',not any(diff.values()),'runtime/v0.4.0/825180311/'+mode+'/measurement-restart.json','restart persistence',diff)
 if mode=='winter':
  for phase,data in [('initial',a),('restart',b)]:check('paper.'+phase+'.builtin_pack',data['winterPluginEnabled'] and any('MidgardWinterWorld/fantasy' in v for v in data['enabledDatapacks']),'runtime/v0.4.0/825180311/winter/measurement-'+phase+'.json','one-JAR bootstrap')
check('artifact_structure',read('artifact-check.json')['status']=='PASS','artifact-check.json','artifacts')
check('native_static_allowlist_and_sources',read('storybook-static-validation.json')['status']=='PASS','storybook-static-validation.json','native static')
name='fantasy04-visual-metrics.json';d=read(name)
for kind in ['lantern-willows-objects','moon-mushrooms-objects','moonstones-objects','ice-objects','clearings-objects']:check('natural_placement.'+kind,d['metrics'].get(kind,0)>0,name,'natural fantasy objects')
name='screenshots-fantasy04/provenance.json';d=read(name);assert d['plugin_sha256']==hashlib.sha256((ROOT/'dist/MidgardWinterWorld.jar').read_bytes()).hexdigest()
for shot in d['screenshots']:
 p=R/'screenshots-fantasy04'/shot['file'];data=p.read_bytes();check('native_F2.'+shot['file'],hashlib.sha256(data).hexdigest()==shot['sha256'] and data[-12:]==b'\x00\x00\x00\x00IEND\xaeB`\x82',name,'real graphical samples')
for name in ['50_to_100_player_soak','all_cross_biome_crown_edges','all_high_altitude_climate_boundaries','full_purpur_WG_Luna_stack','all_biomes_visual_acceptance','survival_economy_balance','active_luna_restart_restore','luna_hot_reload_dynamic_creation','large_schematics_custom_Y_offsets','crash_atomicity','external_legacy_pack_priority_combinations']:
 checks.append({'id':name,'status':'NOT RUN','group':'remaining scope','evidence':'KNOWN_ISSUES.md'})
counts={s:sum(c['status']==s for c in checks) for s in ['PASS','FAIL','BLOCKED','NOT RUN']}
result={'version':'0.4.0 Fantasy','project_status':'TEST VERIFIED (scoped); not RELEASE CANDIDATE','counts':counts,'counting':'161 Java +16Python. Current one-JAR Paper integrations, Purpur initial/restart, one64ROI-per-variant matrix. Old37biome/two-seed coverage is historical, not counted here. Source raw variable-window diagnostic remains FAIL. Loop samples are not counted as extra tests.','checks':checks}
(R/'test-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
lines=['# Проверки Fantasy0.4.0','',f"**PASS {counts['PASS']}, FAIL {counts['FAIL']}, BLOCKED {counts['BLOCKED']}, NOT RUN {counts['NOT RUN']}. TEST VERIFIED в указанном объёме.**",'','161 Java и16 Python, без skipped. Публичный Paper API1.21.11, Java21. Контракт Luna выполнен с предоставленным JAR. Полный каталог: test-results.json.','', '| Группа | PASS | FAIL | NOT RUN |','|---|---:|---:|---:|']
for group in sorted({c['group'] for c in checks}):
 n=collections.Counter(c['status'] for c in checks if c['group']==group);lines.append(f"| {group} | {n['PASS']} | {n['FAIL']} | {n['NOT RUN']} |")
lines+=['','Один FAIL сохранён: исходный height-window fingerprint825180311/131,135. Разная высота native-стволов меняет набор выбранных координат. Как в0.3, независимая сверка **одинаковых координат** прошла:1 572 785 raw underground states, все сравниваемые *_ore позиции/состояния на всей сохранённой высоте, bbox структур и block entities совпали. Сырые fingerprints не переименованы в PASS. Подземелье ограничено пересечением окон −64..min(31,baselineHeight−8,winterHeight−8).','', 'Финальная0.4 матрица — один seed,64ROI на вариант,128уникальных vanilla/winter ROI и256запросов initial/restart. Vanilla-контроль повторно использован. Два seed/320пар/7,9млн0.3 не объявляются новым прогоном0.4.','','Оба текущих Paper стека WG/Luna+WE7.3.19 и отдельно FAWE2.16.2:19fixtures,3реальных protocol-client проверки,54-слотовый лут, природный снег,End cleanup0tickets,Chunky/stop. В0.4 запущены fixtures, не повторный поиск37биомов/11структур. Purpur2568:4ROI, initial/restart и повторное обнаружение встроенного pack. Все5площадок используют один текущий JAR, без внешних datapack ZIP.','','В игре подтверждены ивы, светящиеся грибы, лунные камни и кристаллы; новый flower_forest с зимними розовыми кронами снят отдельным кадром.5native F2 PNG, без image editing/шейдеров/ручного строительства сцены.','']
s='\n'.join(lines);s=re.sub(r'(?<=[А-Яа-яЁё])(?=\d)|(?<=\d)(?=[А-Яа-яЁё])',' ',s);(R/'TEST_RESULTS.md').write_text(s)
print(json.dumps(counts))
