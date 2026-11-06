#!/usr/bin/env python3
"""Report actual 0.6 terrain evidence. Raw material mismatches remain FAIL diagnostics, separate from cave geometry."""
from pathlib import Path
import collections,hashlib,json,re,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'reports';checks=[]
def read(n):return json.loads((R/n).read_text())
def check(name,ok,evidence,group,detail=None):
 row={'id':name,'status':'PASS' if ok else 'FAIL','group':group,'evidence':evidence}
 if detail is not None:row['detail']=detail
 checks.append(row)
for file in sorted((R/'junit').glob('TEST-*.xml')):
 for c in ET.parse(file).getroot().iter('testcase'):
  status='NOT RUN' if c.find('skipped') is not None else 'FAIL' if c.find('failure') is not None or c.find('error') is not None else 'PASS'
  checks.append({'id':c.get('classname')+'.'+c.get('name'),'status':status,'group':'Java unit/contract','evidence':str(file.relative_to(R))})
assert len(checks)==182
for name,n in [('datapack',6),('storybook',11),('schematic',6),('terrain',9)]:
 file='logs/'+name+'-tests.log';s=(R/file).read_text();assert f'Ran {n} tests' in s and '\nOK\n' in s;cases=re.findall(r'^(test_\w+)\s+\(',s,re.M);assert len(cases)==n
 for c in cases:check('python.'+c,True,file,'Python unit')
profiles={'worldedit':'terrain06-worldedit-','fawe':'terrain06-fawe-'};(R/'final-integration-profiles.json').write_text(json.dumps(profiles,indent=2)+'\n')
for variant,prefix in profiles.items():
 name=prefix+'runtime-audit.json';d=read(name);assert len(d['checks'])==28
 for row in d['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'real WG/Luna fixtures')
 name=prefix+'client-smoke.json'
 for row in read(name)['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'real protocol client')
 name=prefix+'natural-luna.json';d=read(name)
 check(variant+'.default_5x5_natural_snow',d['status']=='PASS' and d['platformRadius']==2 and 'minecraft:snow[' in d['belowBefore'] and d['belowBefore']==d['belowAfter'],name,'natural Luna')
 check(variant+'.End_cleanup',d['ticketCountAfterEnd']==0 and d['surfacePreservedAfterEnd'] and d['blockAfterEnd']=='AIR',name,'natural Luna')
 name='logs/'+prefix+'integration-console.log';s=(R/name).read_text()
 for label,token in [('chunky','Task finished'),('stop','All dimensions are saved'),('one_native_pack','FANTASY_PACK_DISCOVERED MidgardWinterWorld/fantasy'),('terrain_profile','FANTASY_TERRAIN_ENABLED true')]:check(variant+'.'+label,token in s,name,'lifecycle')
for prefix,label in [('terrain06-final-disable','purpur_global_disable'),('terrain06-classic','paper_classic_profile'),('terrain06-seed42','paper_second_seed')]:
 name=prefix+'-summary.json';d=read(name)
 check(label+'.one_jar',d['status']=='PASS' and d['one_jar_no_external_pack'],name,'bootstrap/restart')
 check(label+'.restart_persistence',not any(d['restart_differences'].values()),name,'bootstrap/restart')
 if 'disable' in prefix:check(label+'.global_disable',d['global_disable_verified'],name,'bootstrap/restart')
 if 'classic' in prefix:
  check(label+'.independent_terrain_off',d['terrain_disabled_profile'] and all('FANTASY_TERRAIN_ENABLED false' in (R/f'logs/{prefix}-{p}.log').read_text() for p in ['initial','restart']),name,'bootstrap/restart')
 if 'seed42' in prefix:check(label+'.seed42',d['seed']==42,name,'bootstrap/restart')
name='terrain06-structures-audit.json';d=read(name);assert len(d['checks'])==14
for row in d['checks']:check(row['name'],row['status']=='PASS',name,'native structures/cave biomes',row.get('detail'))
name='terrain06-artifact-identity.json';d=read(name)
check('identical_current_main_jar',all(v['identical_current_jar'] for v in d),name,'artifacts')
check('no_external_datapack_zip',all(v['no_external_datapack_zip'] for v in d),name,'artifacts')
name='terrain06-final-matrix-terrain-invariants.json';d=read(name)
for k,v in d['checks'].items():
 group='raw material diagnostic' if k.startswith('deep_raw_') or k.startswith('deep_ores_') else 'terrain/cave geometry'
 check(k,v,name,group,{'columns':d['columns'],'cave_geometry_coordinates_through31':d['cave_geometry_coordinates_through31'],'raw_material_differences_through_y0':len(d['deepDifferences']),'ore_differences_through31':len(d['oreDifferencesThrough31'])})
name='terrain06-sparse-map-comparison.json';d=read(name);check('wide_same_xz_sparse_map',d['points']==4356 and d['meanAbsoluteHeightChange']>=15,name,'terrain/cave geometry',d)
for mode in ['vanilla','winter']:
 stem='runtime/v0.6.0/825180311/'+mode+'/measurement-';a=read(stem+'initial.json');b=read(stem+'restart.json')
 diff={key:[[x['x'],x['z']] for x,y in zip(a['chunks'],b['chunks']) if x.get(key)!=y.get(key)] for key in ['rawUndergroundBlockStateHash','belowSurfaceGeometry','surfaceByBiome','blockEntities','winterMarker']}
 check(mode+'.saved_blocks_restart',not any(diff.values()),stem+'restart.json','bootstrap/restart',diff)
 if mode=='winter':
  for phase,data in [('initial',a),('restart',b)]:check('paper_matrix.'+phase+'.pack',data['winterPluginEnabled'] and any('MidgardWinterWorld/fantasy' in p for p in data['enabledDatapacks']),stem+phase+'.json','bootstrap/restart')
for name in ['artifact-check.json','storybook-static-validation.json','terrain-static-validation.json','schematic-validation.json']:check(name,read(name)['status']=='PASS',name,'artifacts')
name='terrain06-grief-client.json';d=read(name)
for row in d['checks']:check('non_op.'+row['name'],row['status']=='PASS',name,'actual grief/build/TNT')
name='screenshots-terrain06/provenance.json';d=read(name);assert d['plugin_sha256']==hashlib.sha256((ROOT/'dist/MidgardWinterWorld.jar').read_bytes()).hexdigest()
for shot in d['screenshots']:
 data=(R/'screenshots-terrain06'/shot['file']).read_bytes();check('native_F2.'+shot['file'],hashlib.sha256(data).hexdigest()==shot['sha256'] and data[-12:]==b'\x00\x00\x00\x00IEND\xaeB`\x82',name,'native game samples')
for name in ['50_to_100_player_soak','all_cross_biome_crown_edges','all_high_altitude_climate_boundaries','full_purpur_WG_Luna_stack','all_biomes_visual_acceptance','survival_economy_balance','active_luna_restart_restore','luna_hot_reload_dynamic_creation','large_schematics_bridges_custom_Y_offsets','crash_atomicity','external_legacy_pack_priority_combinations','all_village_roads_walkthrough','all_seeds_cave_geometry_equivalence']:
 checks.append({'id':name,'status':'NOT RUN','group':'remaining scope','evidence':'KNOWN_ISSUES.md'})
counts={s:sum(c['status']==s for c in checks) for s in ['PASS','FAIL','BLOCKED','NOT RUN']}
result={'version':'0.6.0 Fantasy Terrain','project_status':'TEST VERIFIED (scoped); not RELEASE CANDIDATE','counts':counts,'counting':'182 Java +32 Python. Two current Paper WG/Luna stacks with default 5x5 safety footprint, Purpur global disable restart, classic terrain-off profile, seed42 4ROI restart, 64ROI per variant native terrain matrix, 2 actual village door/foundation scans, 4 further native structure metadata checks, 3 cave-biome locates, real non-OP client and 5 native F2 PNG. Original vanilla baseline reused; old material mismatch diagnostics remain FAIL.','checks':checks}
(R/'test-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
lines=['# Проверки Fantasy Terrain 0.6.0','',f"**PASS {counts['PASS']}, FAIL {counts['FAIL']}, BLOCKED {counts['BLOCKED']}, NOT RUN {counts['NOT RUN']}. TEST VERIFIED в указанном объёме, не RC.**",'','182 Java +32 Python без skipped. Полный машинный каталог — test-results.json.','', '| Группа | PASS | FAIL | NOT RUN |','|---|---:|---:|---:|']
for group in sorted({c['group'] for c in checks}):
 c=collections.Counter(row['status'] for row in checks if row['group']==group);lines.append(f"| {group} | {c['PASS']} | {c['FAIL']} | {c['NOT RUN']} |")
lines+=['','Два FAIL оставлены у исходных строгих сравнений материалов/руд под землёй. Меняется геометрия поверхности, а фактические размещения жил/пород/пещерной растительности местами отличаются. Настройки ore features и cave/structure definitions не меняются. Эти результаты не переименованы в PASS.','','Отдельная сверка класса полостей/жидкостей на 1 572 864 одинаковых координатах Y −64..31 прошла: открытое пространство, вода и лава совпали. Неплотные cave vines и сухой glow lichen отнесены к открытому пространству, waterlogged lichen — к воде; исходные raw block states сохранены. Это не утверждение полного поблочного совпадения мира или всех пещер на всех высотах/seed.','','Основная матрица — seed825180311, 64ROI на вариант с restart, vanilla baseline повторно использован. Второй seed42 — отдельный Paper smoke 4ROI с restart, без независимой полной сверки пещер. Широкая sparse map:4356 одинаковых XZ, четыре колонки на чанк, не сплошной heightmap.','','Две деревни:10+8 дверей с полом, sampled foundation max air gap1/5. Проверяется опора, не прохождение всех дорог. Stronghold/mineshaft/ancient_city/trial_chambers подтверждены нативными starts/pieces на FULL chunks; полные interiors не сканировались. Три cave biome найдены native locate.','','Luna на обоих стеках прошла с radius2(5×5), снег сохранён, End cleanup0tickets. QA generator max-y поднят до280; конфиг установленной отдельно Luna автоматически не меняется. Протокол открытия54-слотового лута реальный. Поршневой regression — синтетическое публичное событие, не физическая redstone-схема.','','Все выбранные PNG — native F2, без обработки/ручной постройки сцен. Полные источники, камеры, хеши и ограничения — screenshots-terrain06/provenance.json.']
(R/'TEST_RESULTS.md').write_text('\n'.join(lines)+'\n');print(json.dumps(counts))
