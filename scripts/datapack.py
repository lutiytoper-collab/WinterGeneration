#!/usr/bin/env python3
"""Build minimal climate-only overrides from pinned vanilla resource extraction; validate recursively."""
import concurrent.futures, hashlib, json, pathlib, sys, urllib.request, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
CACHE=ROOT/'vendor'/'vanilla'
DATA='3458455b4b4537db37d6251a6ace1eed98d025a9'
SUMMARY='c976eb3b2cfcb9f205171527dec46b266afa3ac9'
BIOMES='plains sunflower_plains forest flower_forest birch_forest old_growth_birch_forest dark_forest taiga old_growth_pine_taiga old_growth_spruce_taiga snowy_taiga snowy_plains meadow grove snowy_slopes frozen_peaks jagged_peaks stony_peaks windswept_hills windswept_forest windswept_gravelly_hills river frozen_river'.split()
def fetch(item):
    target,commit,path=item
    url=f'https://raw.githubusercontent.com/misode/mcmeta/{commit}/{path}'
    with urllib.request.urlopen(url,timeout=60) as response: data=response.read()
    json.loads(data)
    target.parent.mkdir(parents=True,exist_ok=True); target.write_bytes(data)
    return {'path':str(target.relative_to(ROOT)), 'url':url, 'sha256':hashlib.sha256(data).hexdigest()}
def download():
    items=[(CACHE/'version.json',SUMMARY,'version.json'),(CACHE/'registries.json',SUMMARY,'registries/data.json'),(CACHE/'pack.mcmeta',DATA,'pack.mcmeta')]
    items += [(CACHE/'biome'/f'{b}.json',DATA,f'data/minecraft/worldgen/biome/{b}.json') for b in BIOMES]
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool: sources=list(pool.map(fetch,items))
    (ROOT/'datapack'/'provenance.json').write_text(json.dumps({'minecraft':'1.21.11','source':'misode/mcmeta vanilla extraction (third party); official server validation outstanding','sources':sources},indent=2)+'\n')
def check_sources():
    p=json.loads((ROOT/'datapack'/'provenance.json').read_text())
    for source in p['sources']:
        assert hashlib.sha256((ROOT/source['path']).read_bytes()).hexdigest()==source['sha256'],source['path']
    version=json.loads((CACHE/'version.json').read_text())
    assert version['id']=='1.21.11' and (version['data_pack_version'],version['data_pack_version_minor'])==(94,1)
def build():
    check_sources()
    source_meta=json.loads((CACHE/'pack.mcmeta').read_text())
    meta=source_meta; meta['pack']['description']='MidgardWinterWorld 0.1.0 — Minecraft 1.21.11 climate only'
    (ROOT/'datapack'/'pack.mcmeta').write_text(json.dumps(meta,indent=2)+'\n')
    for b in BIOMES:
        original=json.loads((CACHE/'biome'/f'{b}.json').read_text())
        modified=dict(original); modified['temperature']=-.5; modified['has_precipitation']=True
        out=ROOT/'datapack'/'data'/'minecraft'/'worldgen'/'biome'/f'{b}.json'
        out.parent.mkdir(parents=True,exist_ok=True); out.write_text(json.dumps(modified,indent=2)+'\n')
    validate()
    with zipfile.ZipFile(ROOT/'dist'/'winter-datapack.zip','w',zipfile.ZIP_DEFLATED) as z:
        for p in sorted((ROOT/'datapack').rglob('*')):
            if p.is_file(): z.write(p,p.relative_to(ROOT/'datapack'))
def validate():
    check_sources()
    registries=json.loads((CACHE/'registries.json').read_text())
    differences=[]
    files=list((ROOT/'datapack'/'data').rglob('*.json'))
    assert {p.stem for p in files}==set(BIOMES)
    for b in BIOMES:
        original=json.loads((CACHE/'biome'/f'{b}.json').read_text())
        modified=json.loads((ROOT/'datapack'/'data'/'minecraft'/'worldgen'/'biome'/f'{b}.json').read_text())
        assert b in registries['worldgen/biome'],b
        assert set(modified)==set(original)
        delta={k:{'before':original[k],'after':modified[k]} for k in original if original[k]!=modified[k]}
        assert set(delta)<= {'temperature','has_precipitation'},delta
        assert modified['temperature']==-.5 and modified['has_precipitation'] is True
        for key in ('features','carvers','spawners','spawn_costs','effects','attributes'):
            if key in original: assert modified[key]==original[key],(b,key)
        for group in modified['features']:
            for entry in group:
                assert isinstance(entry,str) and entry.removeprefix('minecraft:') in registries['worldgen/placed_feature'],entry
        for entry in modified['carvers']:
            assert entry.removeprefix('minecraft:') in registries['worldgen/configured_carver'],entry
        for entry in modified['spawners'].values():
            for spawn in entry: assert spawn['type'].removeprefix('minecraft:') in registries['entity_type']
        differences.append({'biome':b,'changes':delta})
    meta=json.loads((ROOT/'datapack'/'pack.mcmeta').read_text())['pack']
    assert meta['min_format']==[94,1] and meta['max_format']==[94,1]
    (ROOT/'reports'/'biome-diff.json').write_text(json.dumps(differences,indent=2)+'\n')
    print(f'PASS: {len(files)} biome definitions; climate-only diff; registry references; exact 94.1 metadata. Server load NOT RUN.')
if __name__=='__main__':
    if len(sys.argv)>1 and sys.argv[1]=='fetch': download(); build()
    elif len(sys.argv)>1 and sys.argv[1]=='validate': validate()
    else: build()
