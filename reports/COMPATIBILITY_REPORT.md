# Совместимость Fantasy 0.4

**Paper/Purpur 1.21.11, Java 21.** Главный файл теперь Paper plugin с bootstrapper в paper-plugin.yml, DATAPACK_DISCOVERY и command lifecycle. Это публичные Paper API, production NMS/CraftBukkit нет. Bukkit plugin.yml сохранён как метаданные, native bootstrap требует Paper/Purpur.

| Площадка | Фактический результат |
|---|---|
| Paper 132, чистая one-JAR установка | Pack обнаружен до создания мира, default config создан, native лес и новые wonders появились, Chunky 625+169visited завершён |
| Paper 132 WG/Luna + WorldEdit 7.3.19 |19fixtures, реальный 54slot client, природный снег/End cleanup0tickets, Chunky/stop PASS |
| Paper 132 WG/Luna + FAWE 2.16.2 | Отдельный полный повтор указанного набора PASS |
| Purpur 2568 | Чистая one-JAR установка,4ROI initial/restart, pack rediscovered и поверхности/PDC стабильны |
| Paper 132 matrix |64 пары ROI, initial/restart, same-coordinate states/ores/structures/block entities совпали; original window diagnostic1 FAIL |
| Vanilla1.21.11 клиент |5 настоящих F2 PNG, без shaders/resource pack, локальный offline-auth |

Во всех 5 папках один текущий JAR и ноль внешних datapack ZIP. Хеши — fantasy04-artifact-identity.json. Сложные plugin classloader мосты WG/Luna проверены после перехода на Paper plugin; чужие JAR не патчились. Полный Purpur plugin stack не запускался.

Native pack глобален для серверного biome registry и не ограничивается plugin allowed-worlds. Старые внешние climate/Storybook ZIP не требуются; при обновлении их рекомендуется не смешивать со встроенным паком в новой тестовой установке. Все комбинации приоритетов старых паков и native hot reload не проверены.

Luna natural QA:radius1/max-y128/snow-only blacklist, в поставке radius2. Активный restart,hot reload/dynamic drops,customY кроме 1 и большие схематики остаются NOT RUN. Проверки выполнены с конкретными пользовательскими Luna/WG JAR. Публичная Microsoft-аутентификация не проверена; loopback QA и сетевые ошибки Yggdrasil refresh отражены отдельно.
