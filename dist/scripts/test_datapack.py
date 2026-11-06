#!/usr/bin/env python3
import contextlib,importlib.util,io,json,pathlib,shutil,tempfile,unittest
spec=importlib.util.spec_from_file_location('pack',pathlib.Path(__file__).with_name('datapack.py'))
pack=importlib.util.module_from_spec(spec);spec.loader.exec_module(pack)
class DatapackTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
  self.root=pathlib.Path(self.tmp.name)
  for name in ('vendor','datapack'): shutil.copytree(pack.ROOT/name,self.root/name)
  (self.root/'reports').mkdir()
  self.old=(pack.ROOT,pack.CACHE);pack.ROOT=self.root;pack.CACHE=self.root/'vendor/vanilla'
  self.addCleanup(self.restore)
 def restore(self): pack.ROOT,pack.CACHE=self.old
 def mutate(self,rel,fn):
  p=self.root/rel;data=json.loads(p.read_text());fn(data);p.write_text(json.dumps(data))
 def checkFails(self):
  with self.assertRaises(AssertionError): pack.validate()
 def test_valid_pack(self):
  with contextlib.redirect_stdout(io.StringIO()):pack.validate()
 def test_lost_features_rejected(self):
  self.mutate('datapack/data/minecraft/worldgen/biome/plains.json',lambda d:d.update(features=[]));self.checkFails()
 def test_excluded_biome_rejected(self):
  shutil.copyfile(self.root/'datapack/data/minecraft/worldgen/biome/plains.json',self.root/'datapack/data/minecraft/worldgen/biome/desert.json');self.checkFails()
 def test_wrong_pack_version_rejected(self):
  self.mutate('datapack/pack.mcmeta',lambda d:d['pack'].update(min_format=[48,0]));self.checkFails()
 def test_precipitation_loss_rejected(self):
  self.mutate('datapack/data/minecraft/worldgen/biome/plains.json',lambda d:d.update(has_precipitation=False));self.checkFails()
 def test_original_tampering_rejected(self):
  self.mutate('vendor/vanilla/biome/plains.json',lambda d:d.update(temperature=2));self.checkFails()
if __name__=='__main__': unittest.main(verbosity=2)
