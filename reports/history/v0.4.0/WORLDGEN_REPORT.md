# Worldgen Fantasy 0.4

Native pack встроен в главный JAR под fantasy-pack/. Публичный Paper bootstrap обнаруживает MidgardWinterWorld/fantasy до загрузки миров и повторно при restart. Вручную устанавливать внешний ZIP не требуется. Пак содержит 23 biome JSON,12 forest placed overrides и 23 собственных configured/placed ресурса:8 деревьев,8 checked placements и 7 смесей. Exact Minecraft 1.21.11/94.1.

К прежним формам добавлена winter_blossom: ветвящийся cherry ствол и постоянная розовая листва в flower_forest. Остальные biome palettes различаются. Biome feature lists/order, carvers/spawners/spawn_costs, ore/noise/dimension/structure definitions сохраняются; содержимое 12 лесных placed features меняется намеренно. Их conservative transitive reachability проверена по pinned официальному серверу. Общие water/plains/snowy features не меняются, поскольку используются вне allowlist.

Поздние ивы/грибы/лунные камни — новые bounded blueprints, до 11 ширины/24 высоты и 512 записей. Все исходные trunk blocks/игровые препятствия/защиты запрещают запись. Основная поверхность не выравнивается. Mineral fitter поднимает композицию и опускает каждую её опору до местного грунта при перепаде≤2, без разрушения грунта. Кристальная сетка смещена относительно полян; неудача одного вида не блокирует дальнейших кандидатов. Прозрачные и светящиеся детали — обычные block states, не entities.

Сохранились ChunkPopulateEvent→bounded queue→getChunkAtAsync(current,false), main-thread проверка/записи, PDC и отсутствие постоянных tickets позднего декора. Native деревья строит штатный worldgen engine и могут пересекать чанки/биомные края. WG и plugin allowed-worlds относятся к позднему проходу; registry native-пака глобален. Все края не проверены.

Текущая 0.4 матрица:825180311,64 пары ROI. Same-coordinate NBT checker сравнил 1 572 785 полных states в пересечении −64..min(31,baselineHeight−8,winterHeight−8), без нормализации материалов; всё совпало. Все сравниваемые *_ore позиции/состояния на всей высоте, bbox и block entities тоже совпали. Original variable-window fingerprint одного чанка остаётся FAIL из-за несовпадающего высотного окна. Baseline повторно использован, зимняя ветка новая с текущим JAR.

Surface/PDC initial/restart стабильны на Paper matrix и Purpur 4ROI smoke. Новые биомы и полный high-altitude/boundary набор не объявляются проверенными по старым 0.3 данным. Визуальный текущий мир отдельно подтвердил все новые шаблоны и native flower_forest; F2 PNG лежат в screenshots-fantasy04/.
