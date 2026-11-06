# Результаты 0.2.0

**TEST VERIFIED в указанном объёме: PASS 242, FAIL 2, BLOCKED 0, NOT RUN 8.**

146 Java unit/contract и 6 Python unit, без skipped. Официальный опубликованный Paper API 1.21.11; Luna contract использует предоставленный JAR. Полный каталог: test-results.json.

| Группа | PASS | FAIL | NOT RUN |
|---|---:|---:|---:|
| Java unit/contract | 146 | 0 | 0 |
| Purpur smoke | 1 | 0 | 0 |
| Python unit | 6 | 0 | 0 |
| artifacts | 1 | 0 | 0 |
| graphical samples | 5 | 0 | 0 |
| graphical world generation | 6 | 0 | 0 |
| integration fixtures | 38 | 0 | 0 |
| natural Luna | 4 | 0 | 0 |
| real protocol client | 6 | 0 | 0 |
| remaining scope | 0 | 0 | 8 |
| server codec/load | 4 | 0 | 0 |
| server lifecycle | 4 | 0 | 0 |
| strict restart diagnostic | 3 | 2 | 0 |
| unchanged climate/reference | 4 | 0 | 0 |
| worldgen final comparison | 10 | 0 | 0 |
| worldgen persistence | 4 | 0 | 0 |

Два FAIL оставлены явно:

1. Paper seed 42: initial/restart меняет raw state hash в шести чанках; геометрический fingerprint меняется в одном, material delta GLOW_LICHEN −1/AIR +1. Поверхность, PDC и block entities стабильны. После одинакового restart raw underground hashes всех 256 зимних ROI точно совпадают с vanilla. Полное равенство initial/restart не заявляется.
2. Отдельная Purpur-матрица остановилась на несовпадении surfaceByBiome в vanilla-контроле, до запуска зимней ветки. Дополнительный smoke текущего плагина на Purpur 2568/четырёх чанках прошёл; полной матрицей это не считается.

Финальная Paper-матрица: seeds825180311/42, 64+256 ROI на вариант, 640 уникальных ROI и 1280 запросов с restart. Сохранённые vanilla-контроли использованы повторно; зимние ветки запущены с текущим JAR. После restart underground raw states, геометрия, структуры и block entities совпадают во всех 320 парах; поверхность исключённых биомов также совпадает. Сравнение подземелья ограничено Y −64..min(31,surface−8).

Настоящие интеграции Paper: WorldGuard+Luna+WorldEdit 7.3.19 и отдельно FAWE 2.16.2; в каждой 19 fixtures,3 protocol-client проверки, снежная природная площадка, End cleanup0tickets, Chunky и чистый stop. Графический официальный клиент 1.21.11 дал 5 неизменённых F2 PNG. Полной художественной приёмки всех модулей и нагрузочного прогона 50–100 игроков нет.

Исторические неудачные варианты с постоянными tickets, прежние seed и их ошибки сохранены. Они не объявляются текущими PASS.
