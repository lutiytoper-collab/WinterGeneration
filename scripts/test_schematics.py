#!/usr/bin/env python3
import importlib.util,pathlib,json,unittest,contextlib,io,tempfile,shutil,gzip
s=importlib.util.spec_from_file_location('schematics',pathlib.Path(__file__).with_name('schematics.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class SchematicTests(unittest.TestCase):
 def test_original_negative_Z_regions_decode_to_expected_rings(self):
  for size,count in [(7,16),(9,24)]:
   p=m.load_circle(size);self.assertEqual(count,len(p));self.assertEqual(set(p),{(size-1-x,z) for x,z in p});self.assertEqual(set(p),{(x,size-1-z) for x,z in p})
 def test_both_native_templates_have_no_entities_or_block_entity_payloads(self):
  for plan in m.plans():
   data=m.nbt.NBT(gzip.decompress(m.structure(plan))).root();self.assertEqual([],data['entities']);self.assertTrue(all('nbt' not in b for b in data['blocks']));self.assertEqual(4671,data['DataVersion'])
 def test_no_air_commands_jigsaws_containers_or_fluids_can_be_pasted(self):
  for plan in m.plans():
   blocks={c['block'] for c in plan['cells']};self.assertLessEqual(blocks,{'DIORITE','ANDESITE','STONE_BRICKS','MOSSY_STONE_BRICKS','MOSSY_COBBLESTONE','COBBLESTONE','SPRUCE_LOG','SPRUCE_LEAVES','SPRUCE_PLANKS','LIGHT_BLUE_STAINED_GLASS','WHITE_STAINED_GLASS','PURPLE_STAINED_GLASS','JACK_O_LANTERN','SNOW','SNOW_BLOCK'})
 def test_budget_and_single_chunk_geometry(self):
  for plan in m.plans():
   c=plan['cells'];self.assertLessEqual(len(c),512);self.assertTrue(all(abs(b['x'])<=5 and abs(b['z'])<=5 and 1<=b['y']<=24 for b in c))
 def test_duplicate_positions_are_not_emitted(self):
  for plan in m.plans():
   c=plan['cells'];self.assertEqual(len(c),len({(b['x'],b['y'],b['z']) for b in c}))
 def test_pinned_generated_outputs_validate(self):
  with contextlib.redirect_stdout(io.StringIO()):m.validate()
if __name__=='__main__':unittest.main(verbosity=2)
