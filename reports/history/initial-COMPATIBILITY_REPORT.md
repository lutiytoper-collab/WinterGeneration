# Совместимость

| Компонент | Фактическое состояние |
|---|---|
| Paper 1.21.11 | BUILD VERIFIED против немодифицированных official source API; compile-dependencies имеют документированные отличия. Официальный Maven path BLOCKED 403. Runtime NOT RUN |
| Purpur 1.21.11 | NOT RUN: server JAR отсутствует |
| LunaAirDrop 1.0.3-MidgardFix | 2 STATIC CONTRACT TESTS PASS на настоящем JAR: cancellable Start/End, отмена до модификации мира. Адаптер компилируется; INTEGRATION NOT RUN |
| MidgardWorldGuardRU / WorldGuard 7.0.16+2355-f7fded2 | JAR metadata inspected, optional adapter compiled. Runtime NOT RUN: отсутствует WorldEdit и сервер |
| Chunky | NOT RUN: JAR не предоставлен; команды и генерационная матрица подготовлены |
| FAWE / WorldEdit | NOT RUN: JAR не предоставлен; обязателен для реальных Luna/WG |

Хеши предоставленных плагинов: supplied-plugins.json. Их JAR не модифицированы и не включены в распространяемый архив. Компиляция и unit-тесты не подтверждают реальное появление или открытие дропа.
