# Итоговые результаты тестов

**TEST VERIFIED в указанном объёме. PASS 287, FAIL 1, BLOCKED 0, NOT RUN 8.**

Полный каталог: test-results.json. Java unit/contract: 117, Python unit: 6. Сборка использует опубликованный официальный Paper API 1.21.11; контракт Luna выполнен с предоставленным JAR, без пропущенных Java-тестов.

| Группа | PASS | FAIL | NOT RUN |
|---|---:|---:|---:|
| Chunky | 2 | 0 | 0 |
| Java unit/contract | 117 | 0 | 0 |
| Purpur | 2 | 0 | 0 |
| Python unit | 6 | 0 | 0 |
| artifact | 1 | 0 | 0 |
| biome location | 37 | 0 | 0 |
| climate | 37 | 0 | 0 |
| integration fixtures | 38 | 0 | 0 |
| natural Luna | 4 | 0 | 0 |
| real client | 6 | 0 | 0 |
| remaining scope | 0 | 0 | 8 |
| server codec/load | 6 | 0 | 0 |
| server lifecycle | 2 | 0 | 0 |
| static/reference | 3 | 0 | 0 |
| strict diagnostic | 2 | 1 | 0 |
| structure locate/API | 11 | 0 | 0 |
| worldgen invariants | 12 | 0 | 0 |
| worldgen regression | 1 | 0 | 0 |

FAIL относится к строгому равенству снимков seed -892041: пять seagrass/water и дополнительное BEE_NEST. Геометрический/рудный инвариант, сохранность исходных block entities и restart прошли. Нормализация водных растений явно отделена от сырых различий; доказательства не скрыты. Это препятствует заявлению полного поблочного равенства.

NOT RUN: художественный графический осмотр; 50–100 игроков/soak; полный Purpur набор интеграций; все высотные пограничные случаи; аварийная атомарность; restart активного Luna; hot reload/dynamic drops; крупные схематики/custom Y offsets.

Исторические ошибки (сеть, несовместимый Java 25 WorldEdit, ранняя утечка льда, жидкостная нестабильность vanilla, старые поисковые/cleanup прогоны Luna) сохранены в исходных JSON/логах. Они не являются дополнительными текущими PASS и не удалялись после исправления. Метаданные отдельных старых JSON отражают свой этап, а не финальный статус.

Клиентский тест — реальный Minecraft протокол 1.21.11, локальный offline-auth, открытие 54-слотового Luna-инвентаря с предметами. Графический renderer отсутствует. Матрица миров — 1536 уникальных ROI-чанков, 3072 запроса с restart; дополнительные spawn/halo чанки не выданы за измеренные ROI.
