#!/usr/bin/env python3
"""Report actual 0.7 evidence; strict underground material mismatches remain FAIL diagnostics."""
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
assert len(checks)==185
for name,n in [('datapack',6),('storybook',11),('schematic',6),('terrain',13)]:
 file='logs/'+name+'-tests.log';s=(R/file).read_text();assert f'Ran {n} tests' in s and '\nOK\n' in s;cases=re.findall(r'^(test_\w+)\s+\(',s,re.M);assert len(cases)==n
 for c in cases:check('python.'+c,True,file,'Python unit')
profiles={'worldedit':'meadow64n-worldedit-','fawe':'meadow64n-fawe-'};(R/'final-integration-profiles.json').write_text(json.dumps(profiles,indent=2)+'\n')
for variant,prefix in profiles.items():
 name=prefix+'runtime-audit.json';d=read(name);assert len(d['checks'])==28
 for row in d['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'real WG/Luna fixtures')
 name=prefix+'client-smoke.json'
 for row in read(name)['checks']:check(variant+'.'+row['name'],row['status']=='PASS',name,'real protocol client')
 name=prefix+'natural-luna.json';d=read(name)
 check(variant+'.default_5x5_natural_field_snow',d['status']=='PASS' and d['platformRadius']==2 and 'minecraft:snow[' in d['belowBefore'] and d['belowBefore']==d['belowAfter'],name,'natural Luna on field',d.get('naturalArea'))
 check(variant+'.End_cleanup',d['ticketCountAfterEnd']==0 and d['surfacePreservedAfterEnd'] and d['blockAfterEnd']=='AIR',name,'natural Luna on field')
 name='logs/'+prefix+'integration-console.log';s=(R/name).read_text()
 for label,token in [('chunky','Task finished'),('stop','All dimensions are saved'),('one_native_pack','FANTASY_PACK_DISCOVERED MidgardWinterWorld/fantasy'),('terrain_profile','FANTASY_TERRAIN_ENABLED true')]:check(variant+'.'+label,token in s,name,'lifecycle')
for prefix,label in [('meadow64n-purpur-disable','purpur_global_disable'),('meadow64n-classic','paper_classic_profile'),('meadow64n-seed42','paper_second_seed')]:
 name=prefix+'-summary.json';d=read(name)
 check(label+'.one_jar',d['status']=='PASS' and d['one_jar_no_external_pack'],name,'bootstrap/restart')
 check(label+'.restart_persistence',not any(d['restart_differences'].values()),name,'bootstrap/restart')
 if 'disable' in prefix:check(label+'.global_disable',d['global_disable_verified'],name,'bootstrap/restart')
 if 'classic' in prefix:check(label+'.independent_terrain_off',d['terrain_disabled_profile'],name,'bootstrap/restart')
 if 'seed42' in prefix:check(label+'.seed42',d['seed']==42,name,'bootstrap/restart')
name='meadow64n-structures-audit.json';d=read(name);assert len(d['checks'])==14
for row in d['checks']:check(row['name'],row['status']=='PASS',name,'native structures/cave biomes',row.get('detail'))
name='meadow64n-artifact-identity.json';d=read(name)
check('identical_current_main_jar',all(v['identical_current_jar'] for v in d),name,'artifacts')
check('no_external_datapack_zip',all(v['no_external_datapack_zip'] for v in d),name,'artifacts')
name='meadow64n-matrix-terrain-invariants.json';d=read(name)
for k,v in d['checks'].items():
 group='raw material diagnostic' if k.startswith('deep_raw_') or k.startswith('deep_ores_') else 'terrain/cave geometry'
 check(k,v,name,group,{'columns':d['columns'],'cave_geometry_coordinates_through31':d['cave_geometry_coordinates_through31'],'raw_material_differences_through_y0':len(d['deepDifferences']),'ore_differences_through31':len(d['oreDifferencesThrough31'])})
for mode in ['vanilla','winter']:
 stem='runtime/v0.7.0/825180311/'+mode+'/measurement-';a=read(stem+'initial.json');b=read(stem+'restart.json')
 diff={key:[[x['x'],x['z']] for x,y in zip(a['chunks'],b['chunks']) if x.get(key)!=y.get(key)] for key in ['rawUndergroundBlockStateHash','belowSurfaceGeometry','surfaceByBiome','blockEntities','winterMarker']}
 check(mode+'.saved_blocks_restart',not any(diff.values()),stem+'restart.json','bootstrap/restart',diff)
 if mode=='winter':
  check('paper_matrix.explicit_qa_halo',a['qaFullHaloRadius']==1 and a['qaUnmeasuredSupportChunks']==36,stem+'initial.json','bootstrap/restart','64 measured chunks +36 unmeasured QA support chunks; production has no decoration halo tickets')
  for phase,data in [('initial',a),('restart',b)]:check('paper_matrix.'+phase+'.pack',data['winterPluginEnabled'] and any('MidgardWinterWorld/fantasy' in p for p in data['enabledDatapacks']),stem+phase+'.json','bootstrap/restart')
for name in ['artifact-check.json','storybook-static-validation.json','terrain-static-validation.json','schematic-validation.json']:check(name,read(name)['status']=='PASS',name,'artifacts')
for phase in ['audit','restart']:
 name='meadow64n-field-'+phase+'.json';d=read(name)
 for label,ok in [('full_64x64',d['columnsFullyScanned']==4096),('gentle_varied_terrain',1<=d['central80PercentHeightRange']<=8 and d['p95AdjacentStep']<=1),('mostly_dry_floor',d['wetOrIceOrPowderColumns']<=4096*.02),('room_between_cover',d['openHeadroomFraction']>=.88),('rare_steep_edges',d['neighborStepsOver2Fraction']<=.03),('limited_tall_cover',d['canopyOrTallCoverFraction']<=.30),('mostly_supported_floor',d['unsupportedFirst3Blocks']<=4096*.05)]:check('field.'+phase+'.'+label,ok,name,'native PvP meadow')
name='meadow64n-field-decorations.json';d=read(name);landmarks=[v for v in d['objects'] if any(k in v['feature'] for k in ['moon-gate','shrine','ruin'])]
check('field.real_landmarks_retained',len(landmarks)>=1,name,'native PvP meadow',landmarks)
name='meadow64n-grief-client.json';d=read(name)
for row in d['checks']:check('non_op.'+row['name'],row['status']=='PASS',name,'actual grief/build/TNT')
name='screenshots-meadow64n/provenance.json';d=read(name);assert d['plugin_sha256']==hashlib.sha256((ROOT/'dist/MidgardWinterWorld.jar').read_bytes()).hexdigest()
for shot in d['screenshots']:
 data=(R/'screenshots-meadow64n'/shot['file']).read_bytes();check('native_F2.'+shot['file'],hashlib.sha256(data).hexdigest()==shot['sha256'] and data[-12:]==b'\x00\x00\x00\x00IEND\xaeB`\x82',name,'native game samples')
for name in ['50_to_100_player_soak','all_cross_biome_crown_edges','all_high_altitude_climate_boundaries','full_purpur_WG_Luna_stack','all_biomes_visual_acceptance','survival_economy_balance','active_luna_restart_restore','luna_hot_reload_dynamic_creation','large_schematics_bridges_custom_Y_offsets','crash_atomicity','external_legacy_pack_priority_combinations','all_village_roads_walkthrough','all_seeds_cave_geometry_equivalence']:
 checks.append({'id':name,'status':'NOT RUN','group':'remaining scope','evidence':'KNOWN_ISSUES.md'})
counts={s:sum(c['status']==s for c in checks) for s in ['PASS','FAIL','BLOCKED','NOT RUN']}
result={'version':'0.7.0 Fantasy PvP Fields','project_status':'TEST VERIFIED (scoped); not RELEASE CANDIDATE','counts':counts,'counting':'185 Java +36 Python. Current Paper WE/FAWE +WG/Luna default 5x5 on natural field, bounded public generator XZ offsets. Purpur global disable, classic profile and seed42 smoke/restart. 64 ROI native matrix +36 unmeasured FULL QA support chunks; original pure vanilla baseline reused. Exhaustive 64x64 field before/after restart, non-OP grief/build/TNT, current native F2 PNG. Raw underground material/ore mismatches remain FAIL.','checks':checks}
(R/'test-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
lines=['# Проверки 0.7.0','',f"**PASS {counts['PASS']}, FAIL {counts['FAIL']}, BLOCKED {counts['BLOCKED']}, NOT RUN {counts['NOT RUN']}. TEST VERIFIED в указанном объёме, не RC.**",'','185 Java +36 Python без skipped. Полный каталог — test-results.json.','', '| Группа | PASS | FAIL | NOT RUN |','|---|---:|---:|---:|']
for group in sorted({c['group'] for c in checks}):
 c=collections.Counter(row['status'] for row in checks if row['group']==group);lines.append(f"| {group} | {c['PASS']} | {c['FAIL']} | {c['NOT RUN']} |")
field=read('meadow64n-field-audit.json');decor=read('meadow64n-field-decorations.json')
lines+=['',f"Луг: seed825180311, X{field['xMin']}..{field['xMin']+63}, Z{field['zMin']}..{field['zMin']+63}, 64×64. Все4096 колонок проверены до и после restart. Основные80% высот: Y{field['p10GroundY']}..{field['p90GroundY']}; свободный проход {field['openHeadroomFraction']:.2%}. Полный диапазон и редкие ступени/пещерные входы сохранены в JSON, а не исключены из измерений.",'',f"В поляне и её64-блочном окружении записано {len(landmarks)} настоящих арок/святилищ/руин; расположение внутри или снаружи измеренного квадрата отмечено явно. Остальные деревья и детали перечислены в meadow64n-field-decorations.json. QA не ставит и не переносит объекты. Это не проверка баланса любого seed.",'','Два строгих raw material/ore FAIL сохранены. Дополнительный FAIL — один эвристический замер опоры поворота пустынной дороги:9 блоков воздуха при лимите8, координаты−3727,94,−1339. Все24 проверенные двери имеют пол. Этот диагностический результат не скрыт и требует отдельного осмотра дороги. Полости/вода/лава сверяются независимо на1572864 одинаковых координатах Y−64..31; cave vines и сухой glow lichen относятся к открытому пространству, waterlogged lichen — к воде. Определения и теги карверов не меняются.','','Основная матрица:64 ROI seed825180311 и36 дополнительных FULL чанков QA, не входящих в замеры. В production нет декорационных halo tickets. Сохранён pure vanilla baseline. Seed42 — отдельный smoke4ROI с restart, без полной сверки пещер.','','Две деревни проверены по настоящим дверям и опорам в нижних девяти слоях jigsaw pieces; это не обход всех домов/дорог. Ещё четыре структуры — native starts/pieces, три cave biome — native locate.','','Luna на WE/FAWE: radius2 (5×5), случайные координаты в естественном лугу через публичные X/Z offsets, native слой SNOW сохранён, End cleanup0tickets. QA max-y280; сторонний конфиг автоматически не меняется. Лут открывается настоящим клиентом. Поршневой regression — публичное синтетическое событие, не redstone-схема.','','Текущие PNG — native F2 без обработки или ручной постройки сцен. Предыдущие11 кадров разной местности относятся к0.6.0. Отклонённый плоский прототип и его диагностика сохранены в history/pvp07-flat-prototype.']
(R/'TEST_RESULTS.md').write_text('\n'.join(lines)+'\n');print(json.dumps(counts))
