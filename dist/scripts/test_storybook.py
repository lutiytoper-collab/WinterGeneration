#!/usr/bin/env python3
import contextlib,importlib.util,io,json,pathlib,shutil,tempfile,unittest
spec=importlib.util.spec_from_file_location('storybook',pathlib.Path(__file__).with_name('storybook.py'));pack=importlib.util.module_from_spec(spec);spec.loader.exec_module(pack)
class StorybookTests(unittest.TestCase):
 def setUp(self):
  tmp=tempfile.TemporaryDirectory();self.addCleanup(tmp.cleanup);self.root=pathlib.Path(tmp.name)
  for name in ['vendor','datapack','storybook']:shutil.copytree(pack.ROOT/name,self.root/name)
  (self.root/'reports').mkdir();old=(pack.ROOT,pack.SOURCE,pack.OUT);self.addCleanup(lambda:self.restore(old));pack.ROOT=self.root;pack.SOURCE=self.root/'vendor/storybook';pack.OUT=self.root/'storybook'
 def restore(self,old):pack.ROOT,pack.SOURCE,pack.OUT=old
 def mutate(self,rel,fn):
  p=self.root/rel;d=json.loads(p.read_text());fn(d);p.write_text(json.dumps(d))
 def reject(self):
  with self.assertRaises(AssertionError):pack.validate()
 def test_valid_storybook(self):
  with contextlib.redirect_stdout(io.StringIO()):pack.validate()
 def test_ore_feature_loss_rejected(self):
  self.mutate('storybook/data/minecraft/worldgen/biome/taiga.json',lambda d:d['features'][6].clear());self.reject()
 def test_native_feature_order_change_rejected(self):
  self.mutate('storybook/data/minecraft/worldgen/biome/forest.json',lambda d:d['features'][9].reverse());self.reject()
 def test_unlisted_biome_override_rejected(self):
  p=self.root/'storybook/data/minecraft/worldgen/biome';shutil.copyfile(p/'forest.json',p/'desert.json');self.reject()
 def test_shared_ocean_tree_override_rejected(self):
  p=self.root/'storybook/data/minecraft/worldgen/placed_feature';shutil.copyfile(p/'trees_taiga.json',p/'trees_water.json');self.reject()
 def test_reachable_excluded_biome_rejected(self):
  self.mutate('vendor/storybook/provenance.json',lambda d:d['allowlisted_reachable_biomes_by_override']['trees_taiga'].append('savanna'));self.reject()
 def test_unpinned_reference_rejected(self):
  self.mutate('vendor/storybook/configured_feature/spruce.json',lambda d:d['config'].update(force_dirt=True));self.reject()
 def test_wrong_codec_version_rejected(self):
  self.mutate('storybook/pack.mcmeta',lambda d:d['pack'].update(min_format=[48,0]));self.reject()
 def test_no_dimension_or_ore_resources(self):
  names=[p.relative_to(pack.OUT).as_posix() for p in pack.OUT.rglob('*.json')];self.assertFalse(any('/dimension/' in n or '/noise_settings/' in n or '/structure/' in n or '/ore_' in n for n in names))
 def test_birch_identity_and_persistent_frost_crowns(self):
  d=json.loads((pack.OUT/'data/midgard/worldgen/configured_feature/elder_birch.json').read_text())['config'];self.assertEqual(d['trunk_provider']['state']['Name'],'minecraft:birch_log');self.assertEqual(d['foliage_provider']['state']['Properties']['persistent'],'true')
if __name__=='__main__':unittest.main(verbosity=2)
