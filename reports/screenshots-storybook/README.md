# Настоящие F2-кадры Storybook 0.3.0 —6 ноября 2026

Официальный немодифицированный Minecraft 1.21.11, Paper 132, Java 21, новый winter_test seed825180311. Storybook pack установлен до первой генерации; профиль config-storybook.yml. **Шейдеров, resource pack, image generation, ретуши и ручного строительства сцены нет.** Все 6PNG сохранены самой игрой через F2, проверены chunkCRC/IEND и скопированы без изменения байтов.

| Кадр | Вид |
|---|---|
| [01-frost-cathedral.png](01-frost-cathedral.png) | Высокий native инеевый хвойный лес с холодной почти белой листвой |
| [02-lantern-under-giants.png](02-lantern-under-giants.png) | Процедурный снежный навес и тёплый свет под гигантскими кронами |
| [03-silver-grove.png](03-silver-grove.png) | Крупные ветвящиеся берёзы и ели на склоне |
| [04-birch-walk.png](04-birch-walk.png) | Вид с высоты ходьбы: белая кора, широкий полог и зимние цветы |
| [05-blue-hour.png](05-blue-hour.png) | Сумерки над лесом; обычное Minecraft block lighting от фонарей |
| [06-f3-proof.png](06-f3-proof.png) | Нативный F3 с 1.21.11,Java 21,Paper,координатами и llvmpipe |

1600×900, render distance10,gamma0.6, Mesa llvmpipe/OpenGL4.5, Xvfb. Камера spectator, погода/время выставлены серверными командами. FPS программного клиента не является TPS сервера. Локальный loopback offline-auth, без заявления публичной Microsoft-аутентификации.

[provenance.json](provenance.json): SHA-256 JAR/Storybook/PNG, официальный clientSHA-1, исходное имя F2, UTC и камера. Скриншоты побайтно совпадают с завершёнными оригиналами игры. В кадре 03 скрипт сначала заметил ещё незаписанный PNG; после окончания штатного кодирования скопирован тот же полный оригинал. Повторная отрисовка/обработка картинки не выполнялась.

Логи: ../logs/story03-client-console.log и ../logs/story03-preview2-console.log. Название runtime-папки preview2 отражает второй запуск после исправления codec; JAR/pack побайтно совпадают с поставкой и шестью конечными QA-площадками. Chunky 625visited чанков завершён до съёмки, затем загружены новые области камерой. Поздние counters ../story03-visual-metrics.json не являются подсчётом native деревьев.

Ранние галереи ../screenshots/ и ../screenshots-v2/ сохранены. Это выбранные реальные места, а не полная художественная приёмка всех биомов/модулей. Кристаллическая композиция в данной галерее не снята.
