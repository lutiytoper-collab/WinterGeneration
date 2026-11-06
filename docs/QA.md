> Актуальный0.3.0: native Storybook pack, профиль и6F2-кадров описаны в STORYBOOK.md и reports/TEST_RESULTS.md. Ниже базовые команды; сохранённые0.1/0.2запуски являются историей.

# Воспроизведение проверки

Рабочий checkout: `/workspace/WinterGeneration`. Пользователь уже принял Minecraft EULA для данного облачного тестового сервера. Скрипты на новой площадке требуют явного `--accept-eula`; они не скачивают и не принимают EULA молча.

## Сборка

`source scripts/env.sh`, затем `LUNA_JAR=/absolute/path/Luna.jar bash scripts/check.sh full`. Всего 149 Java и 16 Python тестов. Без Luna JAR два контрактных теста SKIPPED. Опубликованный API используется по умолчанию. Официальные серверные JAR и пользовательские плагины не включены в sources.zip.

## Матрица миров

`python3 scripts/server-matrix.py --paper /path/paper.jar --sha256 OFFICIAL_SHA256 --accept-eula --side 16 --run-name fresh-qa`.

По умолчанию seeds 825180311, 42, -892041; сервер слушает loopback:25575. `--runtime-cache` разрешает повторно использовать cache/libraries/versions того же сервера, `--port` меняет порт. Для уже существующей завершённой матрицы есть `--resume`; хеши артефактов и наличие tick freeze проверяются. Текущий основной JAR может отличаться от ранее снятой матрицы изменениями Luna: такие старые каталоги не следует объявлять вновь выполненными.

Каждая ветка создаёт новые миры, последовательно запрашивает 16×16 чанков с начала 128,128, удерживает plugin tickets, ждёт отложенного декора, записывает измерения, сохраняет и перезапускает сервер. После restart сравниваются геометрия, поверхность, block entities, PDC. Полные bbox сравниваются между vanilla/winter после одинакового reload: vanilla shipwreck сам уточняет Y bbox при сохранении.

Для сохранённых здесь трёх итоговых dataset выполняется `python3 scripts/check-invariants.py`. Скрипт хранит сырые различия seagrass/water отдельно от геометрии твёрдых блоков. `compare-region.py` позволяет читать реальные Anvil/NBT без серверного NMS. Нельзя называть этот тест полным поблочным равенством всех миров.

## Интеграция Luna, WG, WorldEdit/FAWE и Chunky

Сначала проверьте официальные контрольные суммы скачанных JAR. Из новой площадки:

```bash
python3 scripts/prepare-integration.py --paper /path/paper.jar --luna /path/Luna.jar --worldguard /path/WorldGuard.jar --worldedit /path/WorldEdit.jar --fawe /path/FAWE.jar --chunky /path/Chunky.jar --natural-snow --accept-eula
bash scripts/install-qa-client.sh
QA_PREFIX=own-worldedit- QA_COMMAND='winterprobe fixtures' QA_NATURAL=1 python3 scripts/integration-server.py
QA_VARIANT=fawe QA_PREFIX=own-fawe- QA_COMMAND='winterprobe fixtures' QA_NATURAL=1 python3 scripts/integration-server.py
```

`--natural-snow` явно задаёт **QA**-профиль: radius=1, max-y=128, SNOW удалён только из material blacklist Luna; для целевого snow-only сценария сухие материалы опоры дополнительно внесены в blacklist, biome blacklist дополнен биомами вне зимнего набора. Поставляемый основной config.yml остаётся radius=2. Без флага подготовка сохраняет профиль из пользовательского JAR.

Каталоги `.runtime/integration` и `.runtime/fawe-integration` должны отсутствовать перед подготовкой; скрипт не затирает существующие запуски. У каждого запуска используйте новый QA_PREFIX. Не запускайте два процесса в одной папке. QA_XMX по умолчанию 4G; сервера используют Java 21 из подготовленной среды, системный CA и proxy. Node/Mineflayer устанавливаются по lockfile.

Сначала выполняются контролируемые 19 проверок в отдельном flat-мире winter_fixture, затем настоящий публичный Luna startCommand ищет природную точку в winter_test. Серверный тест фиксирует реальный StartEvent, unlock, blockdata снега до/после. Клиент открывает инвентарь с предметами, делает stop и проверяет AIR; сервер проверяет сохранность поверхности и нулевые tickets. Затем выполняется реальная ограниченная задача Chunky. Ошибки/таймауты завершают runner с ненулевым статусом; никаких заранее записанных PASS.

Это локальный offline-auth QA на 127.0.0.1:25576/25577 с max-players=2. Эти настройки не предназначены для публичного сервера. Графические скриншоты не создаются.

## Сбор доказательств

Итоговые JSON и console/client logs находятся в reports; worldgen measurement-initial/restart и console logs дополнительно скопированы в reports/runtime. Исторические диагностические FAIL сохранены. В `test-results.json` учитываются только явно перечисленные итоговые проверки; не смешивайте их с количеством строк в логах или количеством просмотренных блоков. `python3 scripts/report.py` проверяет сохранённые доказательства и пересобирает сводку, но не заменяет запуск серверов/тестов. `python3 scripts/package.py` упаковывает существующие файлы и обновляет SHA256SUMS.
