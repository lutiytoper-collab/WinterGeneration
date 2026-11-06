# Проверки Anarchy Fantasy 0.5.0

**PASS 301, FAIL 1, BLOCKED 0, NOT RUN 11. TEST VERIFIED в указанном объёме.**

177 Java +23 Python без skipped. Текущий конечный JAR; предварительные audit05-прогоны до смены палитры не засчитаны как текущие.

| Группа | PASS | FAIL | NOT RUN |
|---|---:|---:|---:|
| Java unit/contract | 177 | 0 | 0 |
| Python unit | 23 | 0 | 0 |
| artifacts | 4 | 0 | 0 |
| lifecycle | 6 | 0 | 0 |
| natural Luna | 4 | 0 | 0 |
| natural fantasy objects | 4 | 0 | 0 |
| non-OP grief client | 6 | 0 | 0 |
| one-JAR bootstrap | 4 | 0 | 0 |
| raw variable-window diagnostic | 0 | 1 | 0 |
| real graphical samples | 4 | 0 | 0 |
| real integration fixtures | 56 | 0 | 0 |
| real protocol client | 6 | 0 | 0 |
| remaining scope | 0 | 0 | 11 |
| restart persistence | 3 | 0 | 0 |
| same-coordinate worldgen | 4 | 0 | 0 |

Исходный variable-window fingerprint сохранён отдельно. Полные raw states сравниваются на одинаковых координатах пересечения окон −64..min(31,baselineHeight−8,winterHeight−8); руды — на всей сохранённой высоте. См. anarchy05-matrix-coordinate-invariants.json. Это один seed и 64 ROI на вариант, vanilla-контроль повторно использован.

Каждый Paper WG/Luna+WE/FAWE стек: 28 fixtures, три клиентские проверки Luna, природный снег, End cleanup, Chunky и stop. Поршень проверен публичным синтетическим событием, не физическим redstone. Все шесть NBT загружены StructureManager. Purpur проверяет enabled→disabled restart, а не включённый pack на обоих запусках.

Отдельный не-OP клиент проверяет ломание/строительство/взрыв в локальном QA-мире; это не обещание обойти правила сторонних плагинов на другом сервере. F2 кадры сняты vanilla-клиентом без обработки и ручной постройки сцены.
