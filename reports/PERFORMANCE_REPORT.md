# Производительность 0.5.0

Paper 132, Java 21, view/simulation 3, Xms512M/Xmx2G. Один seed 825180311, 64 ROI на вариант, initial/restart. Vanilla baseline повторно использован; winter новый. Во время замеров работали другие QA-процессы: таблица не изолирует CPU и не доказывает процент overhead.

| Вариант | ROI | Mean request ms | Median | P95 | Max |
|---|---:|---:|---:|---:|---:|
| vanilla | 64 | 117.34 | 0.12 | 796.27 | 1706.83 |
| winter | 64 | 467.99 | 0.12 | 2367.79 | 6740.89 |

Request latency включает очередь/native подготовку и уже готовый halo, это не CPU одного чанка. Raw height-window FAIL сохранён отдельно от сверки одинаковых координат.

TickWorkBudget теперь ограничивает фактические callbacks в одном тике, а не только выдачу async requests. Лимит 2 ms мягкий, один начатый preflight/объект может дольше; лимит 512 записей не распространяется на native деревья. Декор не удерживает постоянные plugin tickets и не загружает FULL halo. Только bounded Luna preparations используют refcount leases.

Полный 50–100 player soak, стабильные 20 TPS, peak RSS и боевой plugin stack не измерены. Предварительная генерация порциями предпочтительна. Клиентский FPS llvmpipe не является server TPS. Подробные исходные логи сохранены; старые performance snapshots 0.4 находятся в history/v0.4.0/.
