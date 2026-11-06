# Архитектура 0.3.0

Система имеет два режима. winter-datapack.zip — прежний климатический слой 23 биомов. storybook-datapack.zip — отдельный opt-in climate+palette+native forest слой с 12 исходными tree placed overrides и 20 своими configured/placed ресурсами. Вместе два biome-пака в одном мире не нужны. Сборка native Storybook — scripts/storybook.py, официальные pinned references — vendor/storybook/.

Native-деревья создаёт штатный worldgen Minecraft. Biome feature lists/order не меняются, но определения 12 лесных placed features меняются намеренно. Их transitive origins ограничены allowlist; crowns могут пересекать чанки/биомные края. Пак не содержит dimension/noise/carver/ore/structure overrides. Цвета/климат server biome registry глобальны и не ограничены worlds плагина. Подробные границы — STORYBOOK.md.

Поздний плагинный слой: ChunkPopulateEvent новых чанков→PDC processed_v1 pending→bounded queue→getChunkAtAsync(current,false). Callback обрабатывается на main thread. Нет permanent decoration tickets или явного запроса FULL соседей. World.getStructures предоставляет metadata для проверки; известные глубокие структуры используют реальный vertical bbox, поверхностные/неизвестные защищаются по всей вертикали. Ошибки/неизвестные данные, WG, block entities, игроки/dirty events и Luna reservations закрывают размещение. TTL/queue/write limits сохраняются.

DecorationPlan — immutable blueprint. PlacementValidator/ForestPlacementValidator проверяют всю запись до начала. FoundationFitter подстраивает навес к перепаду до 2 блоков без срезания почвы. Предел 512 позиций/11 ширины относится только к этим поздним объектам. Storybook profile отключает overlay forest, оставляя native лес, сугробы/тропы/снег на кронах/навесы/редкий лёд. Кристаллы проверены preflight, runtime visual acceptance не получена.

Luna public Generator/events: bounded async подготовка, main-thread окончательная проверка, owner-refcount tickets, очистка End. Ничьего JAR patch/NMS нет. Активные точки резервируются. Native-дерево, генерируемое при подготовке нового чанка, завершается до итоговой FULL-проверки места.

ClimateBoundaryGuard сохраняет математический biome zoom, public vanillaBiomeProvider и original temperatures. Он исправляет однозначное нативное замерзание вне климатического allowlist в новых чанках, без загрузки соседей. Полные высотные края не покрыты.

QA: исходный variable-window probe, независимый same-coordinate NBT сравниватель и all-height *_ore states, реальные интеграции, графический vanilla-клиент/F2. Отчёт сохраняет исходный diagnostic FAIL, отдельно показывает совпадение 7 864 241 общего underground state. Сбой между записями не транзакционен; автоматического rollback/ретрогенерации нет.
