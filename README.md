# Platinum Vault (аддон для Create Propulsion: Simulated)
NeoForge 1.21.1. Добавляет «Платиновое хранилище предметов» (`platinumvault:platinum_item_vault`):
мультиблок как у хранилища Create, но вдвое больше слотов на блок.

Рецепт (по образцу платинового бака): платиновый лист / хранилище предметов Create / платиновый лист (столбиком).
Файл: src/main/resources/data/platinumvault/recipe/platinum_item_vault.json
Если ID листа не `createpropulsion:platinum_sheet`, поправьте его там.

Сборка: ./gradlew build (нужен Gradle wrapper или установленный gradle 8.x, JDK 21).
Текстуры — заглушки (перекрашенные текстуры Create), замените на свои в assets/platinumvault/textures/block.

## Платиновое улучшение
Предмет `platinumvault:platinum_upgrade`: ПКМ по жидкостному баку, латунному шлюзу, хранилищу предметов Create
или латунному генератору Better Motors заменяет их платиновыми версиями. Баки и хранилища улучшаются целиком,
на каждый блок уходит одно улучшение.

## Платиновый генератор переменного тока (Create: Better Motors)
Блок `platinumvault:platinum_alternator` появляется, если установлены Create: Better Motors и Create Crafts & Additions.
Буфер, выдача FE/t, FE на оборот и нагрузка — вдвое выше, чем у латунного генератора (значения берутся из его конфига).
Текстура — перекраска текстуры из Create: Better Motors (лицензия MIT, Anoop Singh).
