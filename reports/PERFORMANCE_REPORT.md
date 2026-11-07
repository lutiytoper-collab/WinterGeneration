# Производительность0.7.0

Paper132, Java21, seed825180311.64 измеряемых чанка, initial/restart;36 дополнительных FULL чанков QA исключены из latency samples. В production такого удержания нет. Pure vanilla baseline сохранён из прежнего прогона.

| Вариант | Чанков | Mean request ms | Median | P95 | Max |
|---|---:|---:|---:|---:|---:|
| vanilla | 64 | 117.34 | 0.12 | 796.27 | 1706.83 |
| winter | 64 | 478.90 | 0.11 | 1863.90 | 7265.85 |

Request-to-completion latency включает очередь и подготовку соседей, а не только generation CPU. Другие QA-процессы работали параллельно; таблица не доказывает процент overhead.

Пять native2D шумов кэшируются. Поляны изменяют штатную depth, сохраняя детали vanilla sloped_cheese. Поздний декор не держит постоянных tickets и не промотирует FULL halo; bounded leases используются только Luna. Лимиты изменений не относятся к native деревьям и рельефу.

50–100 player soak, стабильные20TPS, peakRSS и боевой plugin stack не измерены. FPS программного графического клиента не является измерением serverTPS.
