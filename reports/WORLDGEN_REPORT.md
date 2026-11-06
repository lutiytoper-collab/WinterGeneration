# Генерация 0.6.0

По последнему уточнению пользователя новый рельеф действует во всём обычном мире, а жаркие биомы сохраняют незимний климат. Новый профиль меняет только final density в minecraft:overworld noise settings. Добавлены три native noise поля и ограниченный ациклический граф density functions. Отдельный профиль без terrain содержит прежний native лес/климат. Bootstrap выбирает один законченный pack, не два одновременно.

Не меняются biome source/router inputs, preliminary_surface_level, aquifers/ore-vein inputs, sea level, world height, surface rules, ore/carver/structure definitions. Новая depth-поправка равна нулю до Y48 и плавно включается до Y88. Nether/End resources не подменяются. Море защищено continental land mask; все океанские границы не обследованы.

Сохраняются 23 зимних biome overrides и 12 лесных placed-feature замен с исходными списками/порядком biome features. Шесть JSON/NBT композиций обычной палитры размещает плагин через прежний preflight. Позднего изменения рельефа/перемещения готовых деревень нет. Native структуры используют штатные heightmaps/pools и terrain adaptation.

Результаты:основная 64 ROI матрица 825180311,16 384 колонки, mean absolute delta65.54;новая natural ground mean128.24 против 62.71. Независимая wide sparse map4356 точек:delta35.34, max145, новый диапазон Y9..255. Сверка полостей/жидкостей на 1 572 864 одинаковых координатах Y−64..31 PASS;строгие raw material/ore сравнения FAIL сохранены. Не заявляется идентичность старых рудных координат/растительности.

Две деревни проверены по реальным дверям/опорам. Четыре дальнейшие структуры — по starts/pieces на FULL chunks. Три cave biome найдены. Полной проверки interiors/всех дорог/всех seed нет. Native F2-галерея показывает выборочные гряды, долину и деревни;это не визуальная приёмка всех биомов.
