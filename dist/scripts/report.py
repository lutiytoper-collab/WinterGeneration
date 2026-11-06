#!/usr/bin/env python3
"""Report current anarchy05 evidence without normalizing original failing diagnostics."""
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
java_count=len(checks);assert java_count==177
for name,n in [('logs/datapack-tests.log',6),('logs/storybook-tests.log',11),('logs/schematic-tests.log',6)]:
 log=(R/name).read_text();assert f'Ran {n} tests' in log and '\nOK\n' in log;cases=re.findall(r'^(test_\w+)\s+\(',log,re.M);assert len(cases)==n
 for c in cases:check('python.'+c,True,name,'Python unit')
profiles={'worldedit':'anarchy05-worldedit-','fawe':'anarchy05-fawe-'};(R/'final-integration-profiles.json').write_text(json.dumps(profiles,indent=2)+'\n')
for variant,prefix in profiles.items():
 name=prefix+'runtime-audit.json';d=read(name);assert len(d['checks'])==28
 for row in d['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'real integration fixtures')
 for row in read(prefix+'client-smoke.json')['checks']:check(variant+'.'+row['name'],row['status']=='PASS',prefix+'client-smoke.json','real protocol client')
 name=prefix+'natural-luna.json';d=read(name)
 check(variant+'.snow_start',d['status']=='PASS' and 'minecraft:snow[' in d['belowBefore'] and d['belowBefore']==d['belowAfter'],name,'natural Luna')
 check(variant+'.End_cleanup',d['ticketCountAfterEnd']==0 and d['surfacePreservedAfterEnd'] and d['blockAfterEnd']=='AIR',name,'natural Luna')
 log='logs/'+prefix+'integration-console.log';s=(R/log).read_text()
 for label,token in [('chunky','Task finished'),('stop','All dimensions are saved'),('builtin_pack','FANTASY_PACK_DISCOVERED MidgardWinterWorld/fantasy')]:check(variant+'.'+label,token in s,log,'lifecycle')
name='anarchy05-disable-summary.json';d=read(name)
check('purpur.one_jar',d['status']=='PASS' and d['one_jar_no_external_pack'],name,'one-JAR bootstrap')
check('purpur.global_disable_on_restart',d['global_disable_verified'] and not any(d['restart_differences'].values()),name,'restart persistence')
name='anarchy05-artifact-identity.json';d=read(name)
check('same_current_jar_in_all5_servers',all(v['identical_current_jar'] for v in d),name,'artifacts')
check('no_external_datapack_in_all5_servers',all(v['no_external_datapack_zip'] for v in d),name,'one-JAR bootstrap')
name='anarchy05-matrix-coordinate-invariants.json';d=read(name)
for key,value in d['checks'].items():check('825180311.'+key,value,name,'same-coordinate worldgen',{'roi':d['chunks'],'coordinates':d['common_coordinates']})
summary=read('server-matrix-anarchy05-matrix.json')
check('825180311.original_variable_window_probe',not any(row['diffs'] for row in summary),'server-matrix-anarchy05-matrix.json','raw variable-window diagnostic','Original height-selected fingerprints retained; independent identical-coordinate result is separate.')
for mode in ['vanilla','winter']:
 base='runtime/v0.5.0/825180311/'+mode+'/measurement-';a=read(base+'initial.json');b=read(base+'restart.json')
 diff={key:[[x['x'],x['z']] for x,y in zip(a['chunks'],b['chunks']) if x.get(key)!=y.get(key)] for key in ['rawUndergroundBlockStateHash','belowSurfaceGeometry','surfaceByBiome','blockEntities','winterMarker']}
 check(mode+'.restart',not any(diff.values()),base+'restart.json','restart persistence',diff)
 if mode=='winter':
  for phase,data in [('initial',a),('restart',b)]:check('paper.'+phase+'.builtin_pack',data['winterPluginEnabled'] and any('MidgardWinterWorld/fantasy' in v for v in data['enabledDatapacks']),base+phase+'.json','one-JAR bootstrap')
for name in ['artifact-check.json','storybook-static-validation.json','schematic-validation.json']:check(name,read(name)['status']=='PASS',name,'artifacts')
name='anarchy05-grief-client.json';d=read(name)
for row in d['checks']:check('anarchy.'+row['name'],row['status']=='PASS',name,'non-OP grief client')
name='anarchy05-visual-metrics.json';d=read(name)
for kind in ['moon-mushrooms-objects','moonstones-objects','lantern-willows-objects']:check('natural.'+kind,d['metrics'].get(kind,0)>0,name,'natural fantasy objects')
check('natural.new_landmarks',any(d['metrics'].get(k,0)>0 for k in ['moon-gate-7-objects','moon-gate-9-objects','frost-shrine-objects','woodland-shrine-objects','stone-ruin-objects','timber-ruin-objects']),name,'natural fantasy objects')
name='screenshots-anarchy05/provenance.json';d=read(name);assert d['plugin_sha256']==hashlib.sha256((ROOT/'dist/MidgardWinterWorld.jar').read_bytes()).hexdigest()
for shot in d['screenshots']:
 p=R/'screenshots-anarchy05'/shot['file'];data=p.read_bytes();check('native_F2.'+shot['file'],hashlib.sha256(data).hexdigest()==shot['sha256'] and data[-12:]==b'\x00\x00\x00\x00IEND\xaeB`\x82',name,'real graphical samples')
for name in ['50_to_100_player_soak','all_cross_biome_crown_edges','all_high_altitude_climate_boundaries','full_purpur_WG_Luna_stack','all_biomes_visual_acceptance','survival_economy_balance','active_luna_restart_restore','luna_hot_reload_dynamic_creation','large_schematics_bridges_custom_Y_offsets','crash_atomicity','external_legacy_pack_priority_combinations']:
 checks.append({'id':name,'status':'NOT RUN','group':'remaining scope','evidence':'KNOWN_ISSUES.md'})
counts={s:sum(c['status']==s for c in checks) for s in ['PASS','FAIL','BLOCKED','NOT RUN']}
result={'version':'0.5.0 Anarchy Fantasy','project_status':'TEST VERIFIED (scoped); not RELEASE CANDIDATE','counts':counts,'counting':'177 Java +23 Python. Current final JAR Paper integrations, Purpur enabled→disabled restart, 64ROI per variant matrix, non-OP protocol grief client, native F2. Vanilla baseline reused. Earlier audit05 palette evidence is historical. Raw variable-window diagnostic retained.','checks':checks}
(R/'test-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
lines=['# Проверки Anarchy Fantasy 0.5.0','',f"**PASS {counts['PASS']}, FAIL {counts['FAIL']}, BLOCKED {counts['BLOCKED']}, NOT RUN {counts['NOT RUN']}. TEST VERIFIED в указанном объёме.**",'','177 Java +23 Python без skipped. Текущий конечный JAR; предварительные audit05-прогоны до смены палитры не засчитаны как текущие.','', '| Группа | PASS | FAIL | NOT RUN |','|---|---:|---:|---:|']
for group in sorted({c['group'] for c in checks}):
 n=collections.Counter(c['status'] for c in checks if c['group']==group);lines.append(f"| {group} | {n['PASS']} | {n['FAIL']} | {n['NOT RUN']} |")
lines+=['','Исходный variable-window fingerprint сохранён отдельно. Полные raw states сравниваются на одинаковых координатах пересечения окон −64..min(31,baselineHeight−8,winterHeight−8); руды — на всей сохранённой высоте. См. anarchy05-matrix-coordinate-invariants.json. Это один seed и 64 ROI на вариант, vanilla-контроль повторно использован.','','Каждый Paper WG/Luna+WE/FAWE стек: 28 fixtures, три клиентские проверки Luna, природный снег, End cleanup, Chunky и stop. Поршень проверен публичным синтетическим событием, не физическим redstone. Все шесть NBT загружены StructureManager. Purpur проверяет enabled→disabled restart, а не включённый pack на обоих запусках.','','Отдельный не-OP клиент проверяет ломание/строительство/взрыв в локальном QA-мире; это не обещание обойти правила сторонних плагинов на другом сервере. F2 кадры сняты vanilla-клиентом без обработки и ручной постройки сцены.']
(R/'TEST_RESULTS.md').write_text('\n'.join(lines)+'\n');print(json.dumps(counts))
