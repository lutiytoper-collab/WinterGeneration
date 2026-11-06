#!/usr/bin/env python3
import contextlib,copy,importlib.util,io,json,pathlib,shutil,tempfile,unittest
s=importlib.util.spec_from_file_location('terrain',pathlib.Path(__file__).with_name('terrain.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class TerrainTests(unittest.TestCase):
 def setUp(self):
  tmp=tempfile.TemporaryDirectory();self.addCleanup(tmp.cleanup);self.root=pathlib.Path(tmp.name)
  for n in ['terrain','vendor/terrain']:shutil.copytree(m.ROOT/n,self.root/n)
  (self.root/'reports').mkdir();old=m.ROOT,m.OUT,m.SOURCE;self.addCleanup(lambda:self.restore(old));m.ROOT=self.root;m.OUT=self.root/'terrain';m.SOURCE=self.root/'vendor/terrain'
 def restore(self,old):m.ROOT,m.OUT,m.SOURCE=old
 def mutate(self,path,fn):
  p=self.root/path;d=json.loads(p.read_text());fn(d);p.write_text(json.dumps(d))
 def test_valid_native_density_graph(self):
  with contextlib.redirect_stdout(io.StringIO()):m.validate()
 def test_only_final_density_router_branch_changes(self):
  a=m.load(m.SOURCE/'noise_settings/overworld.json');b=m.content()['data/minecraft/worldgen/noise_settings/overworld.json'];self.assertEqual({'noise_router'},{k for k in a if a[k]!=b[k]});self.assertEqual({'final_density'},{k for k in a['noise_router'] if a['noise_router'][k]!=b['noise_router'][k]})
 def test_cave_aquifer_ore_biome_and_surface_inputs_kept(self):
  a=m.load(m.SOURCE/'noise_settings/overworld.json');b=m.content()['data/minecraft/worldgen/noise_settings/overworld.json']
  for k in ['aquifers_enabled','ore_veins_enabled','default_block','default_fluid','sea_level','noise','surface_rule','spawn_target']:self.assertEqual(a[k],b[k])
  for k in ['preliminary_surface_level','temperature','vegetation','continents','erosion','depth','ridges','vein_toggle','vein_ridged','vein_gap','barrier','lava']:self.assertEqual(a['noise_router'][k],b['noise_router'][k])
 def test_no_nether_end_dimension_carver_ore_structure_or_biome_override(self):
  for p in m.content():self.assertFalse(any(v in p for v in ['/dimension/','/biome/','/configured_carver/','/configured_feature/','/structure/','nether','end.json']),p)
 def test_custom_depth_fades_only_above_48(self):
  d=m.content()['data/midgard/worldgen/density_function/terrain/depth.json'];self.assertEqual('minecraft:overworld/depth',d['argument1']);self.assertEqual(m.gradient(48,88,0,1),d['argument2']['argument1'])
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
  self.assertFalse((m.OUT/'data/minecraft/worldgen/biome').exists());self.assertEqual(1,len(list((m.OUT/'data/minecraft').rglob('*.json'))))
if __name__=='__main__':unittest.main(verbosity=2)
