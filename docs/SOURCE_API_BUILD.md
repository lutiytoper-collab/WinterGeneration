# Исторический запасной путь: компиляция API из исходников

Итоговый плагин уже собран с опубликованным Paper API и испытан на официальном Paper/Purpur. Ниже сохранено описание ранее проверенного compile-only обходного пути; он не является текущим сетевым блокером.

Повторяемая команда: `bash scripts/build-source-api.sh`, затем `gradle -PpaperApiClasspath=/workspace/tools/paper-api-build/build :plugin:build :probe:jar` после source scripts/env.sh.

Paper commit c5eb0790f199da6c38d0a650e1e5cd5415b28185, ветка ver/1.21.11. Архив SHA-256 4dc0372ef8618b8af832da9d33276d1d8552dc74dc2ba003d0c27b78f148abaa. Полные оригинальные src/main/java и src/generated/java компилируются без изменений. Minecraft server-код не компилируется и не подменяется.

Brigadier: официальный Mojang/brigadier commit 9ba4f13c0fe82b07c08c2dc2d8043f075ffd0d98, SHA-256 архива 7f454f947bea3ead05a4973ce2b3ed061c1600259a0b16fbdde39b8c9cf69683. Это source build вместо недоступного Maven binary 1.3.10.

Из зависимостей Paper использована опубликованная в Maven Central net.md-5:bungeecord-chat:1.21-R0.2 вместо недоступного 1.21-R0.2-deprecated+build.21. Остальные версии compile-зависимостей взяты из upstream paper-api/build.gradle.kts. Компиляция всего API прошла; предупреждения deprecated API сохранены в логах. Бинарную идентичность официальному Maven Paper API это не доказывает.

Сценарий проверяет фиксированные SHA-256 архивов через штатный HTTPS proxy, не отключает TLS, извлекает только обычные source/license файлы с проверкой путей и запускает собственный минимальный Gradle compile build. Оригинальные upstream build scripts не выполняются. Источники/зависимости сохраняются в /workspace/tools/paper-api-build; JAR API и серверные классы НЕ включаются в MidgardWinterWorld.jar.

Доказательства: reports/source-api-provenance.json, logs/paper-api-source-build.log, logs/final-build.log, artifact-check.json. Статус позволяет подтвердить компиляцию проекта Java 21 против исходников точной ветки. Стандартная сборка с опубликованным API и загрузка на Paper 1.21.11 впоследствии выполнены; актуальные доказательства — reports/FINAL_REPORT.md. Статус RELEASE CANDIDATE не присвоен по отдельным указанным там причинам.
