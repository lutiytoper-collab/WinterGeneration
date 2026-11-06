# Worldgen0.3.0

Основной новый артефакт — storybook-datapack.zip:23 умеренных/холодных biome JSON с климатом, frost palette/effects/visual attributes,12 native tree placed-feature overrides и 20 собственных configured/placed ресурсов (7 деревьев,7 checked placements,6 смесей: всего 20). Точный ресурсный счётчик — storybook-static-validation.json. Списки biome features и их порядок сохранены полностью; ore/carver/noise/dimension/structure JSON не переопределяются. Содержание 12 лесных features намеренно заменено, это отдельный более сильный режим, чем прежний climate-only пакет.

Семь деревьев: cathedral_fir, spire_fir, young_fir, silver_birch, elder_birch, moon_oak, ancient_oak. Высоты выбранных стволов до 36. Инеевый цвет получен biome tint для oak_leaves; дерево имеет spruce/birch/oak/dark_oak ствол, листва persistent. Пале-мох и attached lantern decorators работают штатно в native tree codec, без NMS/дополнительного plugin generator. Native кроны могут пересекать чанки; ограничение 11×11/512 относится только к поздним объектам.

Pinned official bundle SHA-1 и извлечённые ресурсы проверены. Граф всех vanilla biome/configured/placed references обойдён консервативно, включая неоднозначные ID обоих видов.12 замен имеют origins только в allowlist. Общие trees_water/plains/snowy не трогались, поскольку используются и вне списка. Непосредственных excluded biome overrides нет. Полный набор crown-spill edges не проверен: origin allowlist не означает, что каждая ветвь останется в том же биоме.

## Точная проверка координат

Новые зимние ветки 0.3.0 и сохранённые vanilla-контроли:825180311/42,64+256 пар ROI, initial/restart. Скрипт storybook-invariants.py читает реальные сохранённые Anvil NBT и сравнивает полные состояния одинаковых координат, без material нормализации. **7 864 241raw states совпали** в пересечении окон −64..min(31,baselineHeight−8,winterHeight−8). Все сравниваемые *_ore позиции/состояния на всей высоте, bbox структур и block entities совпали. Исходные снимки/логи находятся в runtime/v0.3.0/.

Variable-window диагностический FAIL не скрыт: исходный probe отдельно использовал высоту vanilla и нового ствола, поэтому в 131,135 получились разные множества координат и материалы STONE+9/DIORITE+5. Фиксированный Y≤31 exact diff этого чанка показал только 3AIR→SNOW на открытой поверхности Y31. Исходные hashes не переименованы в PASS. Собственный одинаковый координатный инвариант — отдельный тест с другим, явно описанным критерием.

На 256 ROI seed42 после restart excluded surface hashes совпали.37 биомов/климат и 11 структур повторно найдены в каждом новом Paper интеграционном стеке. Nether/End не переопределяются; полный практический обход Nether/End0.3.0 отдельно не повторялся.

Поздний проход остался ChunkPopulateEvent→getChunkAtAsync(current,false), main thread, без постоянных tickets декора/FULL-halo загрузок. WG/структуры/block entities/игроки/Luna проверяются для поздних объектов. Native-деревья используют обычные правила worldgen и не являются WG-декоратором. Глобальный biome registry относится и к другим мирам сервера; plugin allowed-worlds не ограничивает датапак.

Реальные выбранные виды — screenshots-storybook/. Шаблоны кристаллов и навесов обновлены. Навесы сняты в игре; успешное natural crystal placement в этом прогоне не получено и не объявляется PASS.
