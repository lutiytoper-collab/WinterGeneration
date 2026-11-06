# Генерация 0.5.0

23 allowlisted biome overrides и 12 лесных placed-feature замен. Исходные списки/порядок biome features сохранены, ore/carver/noise/dimension/worldgen structure definitions не меняются. Восемь native форм деревьев и семь mixes; железные lantern decorators удалены. Гигантские кроны могут пересекать чанки/биомы, все края не проверены.

Шесть bounded JSON-планов и vanilla NBT assets: две CC0-арки, два собственных святилища и две руины. Native structure assets не добавляют worldgen definitions. Постройки естественно размещает плагин; грунт не срезается, проверки fail closed. Общая палитра обычная, драгоценные/редкие декоративные блоки запрещены белым списком.

Матрица 825180311: 64 ROI на вариант с restart, vanilla baseline повторно использован. Полные результаты anarchy05-matrix-coordinate-invariants.json: 1 572 785 совпавших raw states на одинаковых подземных координатах пересечения окон; сравниваемые руды на всей сохранённой высоте, структуры и block entities совпали. Исходный variable-window fingerprint 131,135 сохраняет FAIL отдельно.

Природное размещение в visual QA подтверждено counters/samples и выборочными F2-кадрами. Не считается проверкой всех биомов или всех сценариев естественной генерации.
