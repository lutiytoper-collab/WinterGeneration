# Архитектура 0.4

Главный Paper plugin объявляет WinterBootstrap в paper-plugin.yml. Bootstrap без Bukkit-world доступа читает только стартовую настройку fantasy-pack-enabled и регистрирует jar:/fantasy-pack через публичный LifecycleEvents.DATAPACK_DISCOVERY. Pack rediscovered при каждом запуске/обнаружении; данные добавлены в JAR processResources. Native worldgen появляется до создания первого мира. Команда winterworld регистрируется через публичный command lifecycle; per-subcommand permissions сохранены. Optional bridges WG/Luna/FAWE имеют необязательный server classpath, runtime проверен.

Native engine строит 8 форм деревьев через 12placed overrides;23biome colours/climate и original feature lists/order сохраняют границы принятой native схемы. Новая blossom форма использует cherry wood/leaves. Vanilla ore/carver/noise/dimension/structure definitions не переопределены. Registry глобален для сервера, native деревья пересекают чанки, все biome-crown edges не покрыты.

Поздний проход: ChunkPopulateEvent новых чанков→PDC→bounded queue→getChunkAtAsync(current,false), callback на main. Permanent decoration tickets/FULL-halo запросов нет. Metadata структур, WG, players/block entities,dirty events,Luna reservations,TTL/queue/budget остаются fail-closed. Старые processed_v1 маркеры сохраняются.

FantasyPlan создаёт immutable схемы ив/грибов/лунных камней; DecorationPlan содержит лёд/навесы и прежние варианты. ForestPlacementValidator выполняет полный preflight до записи. FoundationFitter.fitMineral сохраняет связность композиции и продлевает её нижние опоры до местной почвы при slope≤2, не срезая почву. Кристальные candidates сдвинуты относительно полян; неудача кандидата позволяет следующий вид. Поздние схемы≤11 ширины/24 высоты/512 записей. Их lighting — обычные блоки, дополнительных entities/particle loops нет.

Luna public Generator/events подготавливает bounded чанки async, проверяет на main и использует refcount tickets/End cleanup. WG/Luna JAR не патчатся. ClimateBoundaryGuard сохраняет public biome provider/математику zoom и консервативную коррекцию однозначных границ.

QA различает raw variable-window fingerprints и независимый same-coordinate NBT/ore comparison. Один исходный диагностический FAIL сохранён. One-JAR тесты выполнялись без внешних pack ZIP, включая Purpur initial/restart. Crash rollback, ретрогенерация, полная нагрузка и все внешние pack priority сочетания не реализованы/не проверены в заявленном объёме.
