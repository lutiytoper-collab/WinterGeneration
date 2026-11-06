# Совместимость 0.3.0

Строго **Minecraft 1.21.11 / Java 21**, опубликованный Paper API. Production NMS/CraftBukkit нет. Storybook pack format94.1; чужие JAR не патчились и не распространяются.

| Стек | Реальное подтверждение |
|---|---|
| Paper 132 + Storybook + текущий плагин |2seed,320 пар ROI,initial/restart; same-coordinate states/ores/structures/block entities совпали; variable-window diagnostic1 FAIL |
| Purpur 2568 | Текущий native pack/JAR загрузились,4 ROI smoke |
| Пользовательские WorldGuard+Luna + WorldEdit 7.3.19 |37 биомов,11 структур,19 fixtures,3protocol-client проверки, природный снег и cleanup,Chunky,clean stop |
| Пользовательские WorldGuard+Luna + FAWE 2.16.2 | Отдельный полный повтор того же набора |
| Chunky 1.4.40 | Обе интеграционные задачи и 625visited в графическом мире завершены |
| Официальный vanilla1.21.11 клиент |6 native F2 PNG,1600×900, без shader/resource pack, локальный offline-auth |

JSON/логи итоговых интеграций: story03-worldedit-* и story03-fawe-*. Полные 37/11 location audits в 0.3.0 повторены, не перенесены из 0.2.0. JAR/native pack проверены на побайтное совпадение в 6 тестовых папках; доказательство — story03-runtime-artifact-identity.json.

Luna public Generator/events, реальные Start/Unlock/End, unsafe cancellation, снежная опора сохранена,54 слота лута открыты реальным клиентом. В природном QA radius1/max-y128/snow-only blacklist, тогда как поставочный профиль сохраняет radius2. Hot reload/dynamic creation, активный restart, большие схематики и customY кроме 1 не проверены. Full Purpur plugin stack не проверен.

Native-датапак не требует WG/WE/FAWE/Luna для своих деревьев. Защиты этих плагинов относятся к позднему Bukkit-декору; native origins/branches следуют обычным Minecraft worldgen правилам. Применение biome registry глобально для сервера, а не по plugin allowed-worlds. Публичная Microsoft-аутентификация не проверена; loopback offline QA и сетевые ошибки refreshYggdrasil отдельно отражены в логах.
