# Совместимость 0.5.0

Java 21, официальный Paper API 1.21.11, Paper 132, Purpur 2568. Текущий финальный JAR одинаков на пяти QA-площадках, без внешних datapack ZIP; подробности anarchy05-artifact-identity.json.

Paper: предоставленные LunaAirDrop 1.0.3 MidgardFix и WorldGuardRU, отдельно WorldEdit 7.3.19 и FAWE 2.16.2, Chunky 1.4.40. По 28 runtime fixtures, настоящий protocol-клиент Luna, естественный снег, cleanup, Chunky и stop PASS. Для шаблонов WorldEdit не нужен.

Purpur: standalone bootstrap и enabled→disabled restart, 4 ROI; полного WG/Luna stack на Purpur нет. NMS/CraftBukkit в production нет. Pack discovery через публичный Paper lifecycle; требуются точная версия сервера и полный restart при смене bootstrap-переключателей.

Правила разрешений сторонних плагинов не переопределяются. Survival grief/build/TNT проверены не-OP клиентом в отдельном локальном visual QA-мире. Пользовательские/серверные JAR не распространяются.
