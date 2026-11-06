# Результаты 0.3.0 Storybook

**PASS 416, FAIL 1, BLOCKED 0, NOT RUN 11. TEST VERIFIED только в указанном объёме.**

149 Java unit/contract,6 климатических и 10 Storybook Python тестов, без skipped. Полный каталог: test-results.json.

| Группа | PASS | FAIL | NOT RUN |
|---|---:|---:|---:|
| Java unit/contract | 149 | 0 | 0 |
| Purpur smoke | 1 | 0 | 0 |
| Python unit | 16 | 0 | 0 |
| artifacts | 2 | 0 | 0 |
| biome location | 74 | 0 | 0 |
| boundary sample | 1 | 0 | 0 |
| climate runtime | 74 | 0 | 0 |
| graphical samples | 6 | 0 | 0 |
| integration fixtures | 38 | 0 | 0 |
| native pack/reference | 2 | 0 | 0 |
| natural Luna | 4 | 0 | 0 |
| raw variable-window diagnostic | 1 | 1 | 0 |
| real protocol client | 6 | 0 | 0 |
| remaining scope | 0 | 0 | 11 |
| restart persistence | 4 | 0 | 0 |
| same-coordinate worldgen | 8 | 0 | 0 |
| server codec/load | 4 | 0 | 0 |
| server lifecycle | 4 | 0 | 0 |
| structure locate/API | 22 | 0 | 0 |

Оригинальный variable-window probe сохранил FAIL seed825180311 в чанке 131,135: разные высоты новых стволов меняют границу выборки MOTION_BLOCKING_NO_LEAVES−8. Числа STONE+9/DIORITE+5 означали дополнительные выбранные позиции, а не замену породы. Точное фиксированное Y≤31 сравнение этого чанка нашло только 3AIR→SNOW на открытой поверхности Y31.

Независимое сравнение **одинаковых координат**, без нормализации materials/states: seeds825180311/42,320 пар ROI,7 864 241 общая подземная позиция — полное совпадение raw block states. Отдельно все рудные позиции/состояния на всей сохранённой высоте, структуры и block entities совпали. Объём подземелья — пересечение исходных окон от−64 доmin(31,baselineHeight−8,winterHeight−8), а не весь объём мира. Сырые переменные fingerprints не переименованы в PASS.

640 уникальных vanilla/winter ROI,1280 запросов с restart. Vanilla-контроли использованы повторно; зимние ветки новые с текущим JAR/Storybook. На выбранных 256 пограничных ROI seed42 поверхность исключённых биомов совпала; все возможные переходы крупных крон не проверены.

Каждый Paper стек WorldGuard/Luna+WorldEdit 7.3.19 либо FAWE 2.16.2:37 биомов/климат,11 типов структур,19 fixtures,3 реальных protocol-client проверки, природный снег,54 слота лута,End cleanup0 tickets,Chunky и чистый stop. Purpur 2568:4 ROI smoke с текущим native pack, полный стек NOT RUN.

Шесть настоящих F2 PNG сняты официальным 1.21.11 клиентом, без шейдеров/обработки. Это осмотр выбранных мест, не исчерпывающая художественная приёмка. Кристаллы проверены unit/preflight, но успешное природное размещение и отдельный игровой кадр кристаллов в этом прогоне не получены.

Первый native codec запуск отклонён из-за crown_height28 при максимуме 24; параметр исправлен до 22..24 и финальный pack загрузился на всех площадках. Ошибка сохранена в logs/story03-preview-console.log. Исторические 0.1/0.2 и промежуточные неудачи не объявляются новыми PASS.
