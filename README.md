# Platinum Vault (аддон для Create Propulsion: Simulated)
NeoForge 1.21.1. Добавляет «Платиновое хранилище предметов» (`platinumvault:platinum_item_vault`):
мультиблок как у хранилища Create, но вдвое больше слотов на блок.

Рецепт (по образцу платинового бака): платиновый лист / хранилище предметов Create / платиновый лист (столбиком).
Файл: src/main/resources/data/platinumvault/recipe/platinum_item_vault.json
Если ID листа не `createpropulsion:platinum_sheet`, поправьте его там.

Сборка: ./gradlew build (нужен Gradle wrapper или установленный gradle 8.x, JDK 21).
Текстуры — заглушки (перекрашенные текстуры Create), замените на свои в assets/platinumvault/textures/block.
