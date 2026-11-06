# Проверки Fantasy0.4.0

**PASS 255, FAIL 1, BLOCKED 0, NOT RUN 11. TEST VERIFIED в указанном объёме.**

161 Java и 16 Python, без skipped. Публичный Paper API1.21.11, Java21. Контракт Luna выполнен с предоставленным JAR. Полный каталог: test-results.json.

| Группа | PASS | FAIL | NOT RUN |
|---|---:|---:|---:|
| Java unit/contract | 161 | 0 | 0 |
| Python unit | 16 | 0 | 0 |
| artifacts | 2 | 0 | 0 |
| lifecycle | 4 | 0 | 0 |
| native static | 1 | 0 | 0 |
| natural Luna | 4 | 0 | 0 |
| natural fantasy objects | 5 | 0 | 0 |
| one-JAR bootstrap | 6 | 0 | 0 |
| raw variable-window diagnostic | 0 | 1 | 0 |
| real graphical samples | 5 | 0 | 0 |
| real integration fixtures | 38 | 0 | 0 |
| real protocol client | 6 | 0 | 0 |
| remaining scope | 0 | 0 | 11 |
| restart persistence | 3 | 0 | 0 |
| same-coordinate worldgen | 4 | 0 | 0 |

Один FAIL сохранён: исходный height-window fingerprint825180311/131,135. Разная высота native-стволов меняет набор выбранных координат. Как в 0.3, независимая сверка **одинаковых координат** прошла:1 572 785 raw underground states, все сравниваемые *_ore позиции/состояния на всей сохранённой высоте, bbox структур и block entities совпали. Сырые fingerprints не переименованы в PASS. Подземелье ограничено пересечением окон −64..min(31,baselineHeight−8,winterHeight−8).

Финальная 0.4 матрица — один seed,64ROI на вариант,128 уникальных vanilla/winter ROI и 256 запросов initial/restart. Vanilla-контроль повторно использован. Два seed/320 пар/7,9 млн 0.3 не объявляются новым прогоном 0.4.

Оба текущих Paper стека WG/Luna+WE7.3.19 и отдельно FAWE2.16.2:19fixtures,3 реальных protocol-client проверки,54-слотовый лут, природный снег,End cleanup0tickets,Chunky/stop. В 0.4 запущены fixtures, не повторный поиск 37 биомов/11 структур. Purpur2568:4ROI, initial/restart и повторное обнаружение встроенного pack. Все 5 площадок используют один текущий JAR, без внешних datapack ZIP.

В игре подтверждены ивы, светящиеся грибы, лунные камни и кристаллы; новый flower_forest с зимними розовыми кронами снят отдельным кадром.5native F2 PNG, без image editing/шейдеров/ручного строительства сцены.
