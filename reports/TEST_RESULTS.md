# Проверки Fantasy Terrain 0.6.0

**PASS 336, FAIL 2, BLOCKED 0, NOT RUN 13. TEST VERIFIED в указанном объёме, не RC.**

182 Java + 32 Python без skipped. Полный машинный каталог — test-results.json.

| Группа | PASS | FAIL | NOT RUN |
|---|---:|---:|---:|
| Java unit/contract | 182 | 0 | 0 |
| Python unit | 32 | 0 | 0 |
| actual grief/build/TNT | 6 | 0 | 0 |
| artifacts | 6 | 0 | 0 |
| bootstrap/restart | 13 | 0 | 0 |
| lifecycle | 8 | 0 | 0 |
| native game samples | 5 | 0 | 0 |
| native structures/cave biomes | 14 | 0 | 0 |
| natural Luna | 4 | 0 | 0 |
| raw material diagnostic | 0 | 2 | 0 |
| real WG/Luna fixtures | 56 | 0 | 0 |
| real protocol client | 6 | 0 | 0 |
| remaining scope | 0 | 0 | 13 |
| terrain/cave geometry | 4 | 0 | 0 |

Два FAIL оставлены у исходных строгих сравнений материалов/руд под землёй. Меняется геометрия поверхности, а фактические размещения жил/пород/пещерной растительности местами отличаются. Настройки ore features и cave/structure definitions не меняются. Эти результаты не переименованы в PASS.

Отдельная сверка класса полостей/жидкостей на 1 572 864 одинаковых координатах Y −64..31 прошла: открытое пространство, вода и лава совпали. Неплотные cave vines и сухой glow lichen отнесены к открытому пространству, waterlogged lichen — к воде; исходные raw block states сохранены. Это не утверждение полного поблочного совпадения мира или всех пещер на всех высотах/seed.

Основная матрица — seed 825180311, 64 ROI на вариант с restart, vanilla baseline повторно использован. Второй seed 42 — отдельный Paper smoke 4 ROI с restart, без независимой полной сверки пещер. Широкая sparse map:4356 одинаковых XZ, четыре колонки на чанк, не сплошной heightmap.

Две деревни:10+ 8 дверей с полом, sampled foundation max air gap1/5. Проверяется опора, не прохождение всех дорог. Stronghold/mineshaft/ancient_city/trial_chambers подтверждены нативными starts/pieces на FULL chunks; полные interiors не сканировались. Три cave biome найдены native locate.

Luna на обоих стеках прошла с radius2(5×5), снег сохранён, End cleanup0 tickets. QA generator max-y поднят до 280; конфиг установленной отдельно Luna автоматически не меняется. Протокол открытия 54-слотового лута реальный. Поршневой regression — синтетическое публичное событие, не физическая redstone-схема.

Все выбранные PNG — native F2, без обработки/ручной постройки сцен. Полные источники, камеры, хеши и ограничения — screenshots-terrain06/provenance.json.
