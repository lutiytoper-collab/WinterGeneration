#!/usr/bin/env python3
import contextlib,copy,importlib.util,io,json,pathlib,shutil,tempfile,unittest
s=importlib.util.spec_from_file_location('terrain',pathlib.Path(__file__).with_name('terrain.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class TerrainTests(unittest.TestCase):
 def setUp(self):
  tmp=tempfile.TemporaryDirectory();self.addCleanup(tmp.cleanup);self.root=pathlib.Path(tmp.name)
  for n in ['terrain','vendor/terrain','vendor/vanilla/biome','vendor/storybook','datapack']:shutil.copytree(m.ROOT/n,self.root/n)
  (self.root/'reports').mkdir();old=m.ROOT,m.OUT,m.SOURCE;self.addCleanup(lambda:self.restore(old));m.ROOT=self.root;m.OUT=self.root/'terrain';m.SOURCE=self.root/'vendor/terrain'
 def restore(self,old):m.ROOT,m.OUT,m.SOURCE=old
 def mutate(self,path,fn):
  p=self.root/path;d=json.loads(p.read_text());fn(d);p.write_text(json.dumps(d))
 def test_valid_native_density_graph(self):
  with contextlib.redirect_stdout(io.StringIO()):m.validate()
 def test_only_final_density_router_branch_changes(self):
  a=m.load(m.SOURCE/'noise_settings/overworld.json');b=m.content()['data/minecraft/worldgen/noise_settings/overworld.json'];self.assertEqual({'noise_router','surface_rule'},{k for k in a if a[k]!=b[k]});self.assertEqual({'final_density'},{k for k in a['noise_router'] if a['noise_router'][k]!=b['noise_router'][k]})
 def test_cave_aquifer_ore_biome_and_original_surface_inputs_kept(self):
  a=m.load(m.SOURCE/'noise_settings/overworld.json');b=m.content()['data/minecraft/worldgen/noise_settings/overworld.json']
  for k in ['aquifers_enabled','ore_veins_enabled','default_block','default_fluid','sea_level','noise','spawn_target']:self.assertEqual(a[k],b[k])
  self.assertEqual(a['surface_rule'],b['surface_rule']['sequence'][1])
  for k in ['preliminary_surface_level','temperature','vegetation','continents','erosion','depth','ridges','vein_toggle','vein_ridged','vein_gap','barrier','lava']:self.assertEqual(a['noise_router'][k],b['noise_router'][k])
 def test_no_nether_end_dimension_carver_ore_structure_or_biome_override(self):
  for p in m.content():self.assertFalse(any(v in p for v in ['/dimension/','/biome/','/configured_carver/','/configured_feature/','/structure/','nether','end.json']),p)
 def test_custom_depth_fades_only_above_48(self):
  resources=m.content();d=resources['data/midgard/worldgen/density_function/terrain/depth.json'];self.assertEqual('minecraft:overworld/depth',d['argument1']);self.assertEqual(m.gradient(48,88,0,1),resources['data/midgard/worldgen/density_function/terrain/base_depth_shift.json']['argument1']);self.assertEqual(m.gradient(48,64,0,1),resources['data/midgard/worldgen/density_function/terrain/plains_weight.json']['argument1'])
 def test_dangerous_resource_injection_is_rejected(self):
  p=m.OUT/'data/minecraft/worldgen/noise_settings/nether.json';p.write_text('{}')
  with self.assertRaises(AssertionError):m.validate()
 def test_changing_ore_veins_is_rejected(self):
  self.mutate('terrain/data/minecraft/worldgen/noise_settings/overworld.json',lambda d:d.update(ore_veins_enabled=False))
  with self.assertRaises(AssertionError):m.validate()
 def test_source_tampering_is_rejected(self):
  self.mutate('vendor/terrain/noise_settings/overworld.json',lambda d:d.update(sea_level=90))
  with self.assertRaises(AssertionError):m.validate()
 def test_terrain_disabling_does_not_modify_base_forest_pack(self):
  self.assertFalse((m.OUT/'data/minecraft/worldgen/biome').exists());self.assertEqual(13,len(list((m.OUT/'data/minecraft').rglob('*.json'))))
 def test_field_surface_prefix_is_cold_only_and_narrow_height_band(self):
  d=m.content()['data/minecraft/worldgen/noise_settings/overworld.json']['surface_rule']['sequence'][0];conditions=[]
  while d['type']=='minecraft:condition':conditions.append(d['if_true']);d=d['then_run']
  cold=conditions[0]['biome_is'];self.assertEqual(23,len(cold));self.assertFalse(set(cold)&{'minecraft:desert','minecraft:savanna','minecraft:jungle','minecraft:badlands'})
  self.assertEqual({'Name':'minecraft:rooted_dirt'},d['result_state']);self.assertEqual(72,conditions[2]['anchor']['absolute']);self.assertEqual(97,conditions[3]['invert']['anchor']['absolute']);self.assertEqual(0,conditions[4]['offset'])
 def test_field_weight_cannot_change_density_through_y48(self):
  d=m.content()['data/midgard/worldgen/density_function/terrain/plains_weight.json'];self.assertEqual(m.gradient(48,64,0,1),d['argument1'])
  self.assertEqual(-7,m.content()['data/midgard/worldgen/noise/fantasy_plains.json']['firstOctave']);self.assertEqual(-6,m.content()['data/midgard/worldgen/noise/fantasy_plains_detail.json']['firstOctave']);self.assertIn('data/midgard/worldgen/density_function/terrain/meadow_relief_mask.json',m.content())
 def test_field_prefix_tampering_is_rejected(self):
  self.mutate('terrain/data/minecraft/worldgen/noise_settings/overworld.json',lambda d:d['surface_rule']['sequence'].reverse())
  with self.assertRaises(AssertionError):m.validate()
 def test_only_dense_forest_placements_filter_field_soil_and_no_carver_tag_changes(self):
  d=m.content();self.assertFalse(any('/tags/' in p for p in d));trees={p:v for p,v in d.items() if '/placed_feature/' in p};self.assertEqual(12,len(trees))
  for p,v in trees.items():
   f=v['placement'][-2]['predicate'];self.assertEqual('minecraft:not',f['type']);self.assertEqual('minecraft:rooted_dirt',f['predicate']['blocks']);self.assertEqual([0,-1,0],f['predicate']['offset'])
if __name__=='__main__':unittest.main(verbosity=2)
