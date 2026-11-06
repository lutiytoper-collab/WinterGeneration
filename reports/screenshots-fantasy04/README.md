# Настоящие игровые F2 — Fantasy 0.4

Официальный vanilla Minecraft 1.21.11, Paper 132, Java 21, новый winter_test seed825180311. **Установлен один текущий JAR, ноль внешних datapack ZIP.** Встроенный pack подключён до генерации. Ни шейдеров/resource pack, ни обработки изображения/image generation, ни ручного строительства сцены нет.

| PNG | Вид |
|---|---|
| [01-lantern-willow.png](01-lantern-willow.png) | Процедурная розовая ива с фонарями, лунные камни и инеевый лес |
| [02-moon-mushroom.png](02-moon-mushroom.png) | Бирюзовый светящийся гриб со снежной шляпкой и стеклянными пластинами |
| [03-crystal-grove.png](03-crystal-grove.png) | Голубой лёд/аметистовый кластер с адаптированными к склону опорами |
| [04-winter-blossom-coast.png](04-winter-blossom-coast.png) | Native цветущая зимняя роща с розовыми кронами у побережья |
| [05-f3-proof.png](05-f3-proof.png) | Нативный F3:1.21.11,Java 21,Paper,координаты и llvmpipe |

PNG сохранены Minecraft через F2, проверены chunkCRC/IEND и SHA, скопированы побайтно. [provenance.json](provenance.json) содержит исходные F2-имена, UTC, камеру, SHA-256 JAR/PNG и официальный clientSHA-1.1600×900, render10,gamma0.6, Xvfb/Mesa llvmpipe/OpenGL4.5. Camera spectator; время/погода/телепорты командами сервера. FPS программного клиента не является serverTPS.

Первые 625Chunkyvisited завершены до входа игрока; flower_forest дополнительно подготовлен 169visited задачей, плюс соседние/камерой загруженные чанки. Counters позднего прохода не считаются числом native деревьев. Логи — ../logs/fantasy04-client-console.log и fantasy04-first-console.log. Auth — только loopback offline QA.

Все новые типы действительно размещены генератором и сняты в своих естественных местах; интеграционные fixtures создавались в других QA-папках и не использовались для этих кадров. Полной художественной приёмки всех биомов/переходов по 5 кадрам не заявляется.
