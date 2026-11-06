# Совместимость 0.6.0

Java 21, официальный Paper API 1.21.11, Paper 132 и Purpur 2568. Текущий основной JAR одинаков на 8 конечных QA-площадках, без внешних datapack ZIP;хеши — terrain06-artifact-identity.json. Production NMS/CraftBukkit нет.

Оба Paper стека:предоставленные WorldGuardRU и LunaAirDrop 1.0.3 MidgardFix, отдельно WorldEdit 7.3.19 и FAWE 2.16.2, Chunky 1.4.40. По 28 fixtures,3 настоящие проверки открытия/лут-инвентаря/cleanup, природный снег, площадка 5×5, End cleanup0 tickets, Chunky и чистый stop. QA generator max-y 280;без этого установленная Luna может искать только низкие земли. Конфиг чужого плагина автоматически не меняется.

Purpur:standalone native bootstrap и enabled→disabled restart4 ROI. Paper:classic terrain-off профиль с 4 ROI initial/restart и seed 42 smoke/restart4 ROI. Полный Purpur WG/Luna стек не проверен. Полная основная матрица — один seed 825180311,64 ROI на вариант с restart, старый vanilla baseline повторно использован.

Single pack bootstrap устраняет обнаруженный Paper 132 Adventure/title serialization сбой при двух одновременно обнаруживаемых plugin packs. Оба законченных профиля входят в один JAR;меняется выбранная resource directory, а не число packs.

Генератор не создаёт приватов и не отменяет grief/build/TNT. Это подтверждено actual non-OP survival-клиентом текущей версии. Правила WorldGuard/Luna и других плагинов не переопределяются. Большие здания/мосты/сеть дорог не реализованы. Пользовательские/серверные JAR не распространяются.
