# Thaumcraft 4: Reforged (mc/1.21.1)

Бэкпорт порта Thaumcraft 4.2.3.5 с ветки `main` (MC 26.3) на Minecraft 1.21.1. Игровая логика приходит с `main`, в этой ветке меняются только версионно-зависимые части.

Этот файл описывает, как синхронизировать и поддерживать бэкпорт. Статусы, счётчики тестов, даты прогонов и пути к логам сюда не пишутся. Новая строка добавляется только если без неё агент сделает ошибку. Общие правила порта (принципы, генераторы, архитектура) описаны в `CLAUDE.md` ветки `main`.

## Версии

- Minecraft 1.21.1, NeoForge 21.1.256, Java 21, Parchment 1.21.1 / 2024.11.17, ModDevGradle 2.0.148
- JEI 19.57.0.451, TerraBlender 1.21.1-4.1.0.1, Curios 9.5.1+1.21.1

## Окружение

- Worktree `~/projects/thaumcraft-4-reforged/.worktrees/mc-1.21.1`, основной checkout `~/projects/thaumcraft-4-reforged`.
- Gradle запускается на Java 25 (`export JAVA_HOME=/opt/homebrew/opt/openjdk@25`), тулчейн 21 из `gradle.properties`.
- Справочники лежат в основном checkout (gitignore): `reference/decompiled` (TC4), `reference/mc1710`, `reference/1.21.1/{mc,neo}`. Порт тех же классов TC3 в ветке `mc/1.21.1` репозитория `~/projects/thaumcraft-reforged`: класс, который там есть, берётся оттуда.

## Синхронизация с main

Последний синхронизированный коммит `main`: `0c59401`.

1. `git log --reverse <sha>..main`.
2. Коммит только с ресурсами, lang или `tools/`: `git cherry-pick`, затем `python3 tools/gen_resources.py --mc 1.21.1`.
3. Коммит с Java: `git show <sha> | python3 tools/backport/rewrite.py --target 1.21.1 | git apply -3`, конфликты руками, `./gradlew compileJava`.
4. Коммит с тем же сообщением, обновить sha выше.
5. Новое правило замены API добавлять в `tools/backport/rewrite.py`.

## Где ветка отличается

- Правятся только `platform/`, `client/platform/`, `compat/` (`ValueInput`/`ValueOutput`, `ItemStackTemplate`), регистрация и ресурсы профиля. Правка игровой логики здесь значит, что на `main` не хватает шва: шов добавляется на `main`, затем синхронизируется.
- Бижутерия на Curios (`platform/BaublesCurios`, слоты генерирует `tools/gen_curios.py`), своих слотов `main` нет.
- GameTest через `@GameTestHolder` + `@PrefixGameTestTemplate`.
- Модели предметов через `models/item` с `overrides`, special-рендер через `BlockEntityWithoutLevelRenderer` + `IClientItemExtensions`, tint через `RegisterColorHandlersEvent.Item`.
- Attachments без `.sync()`, `DataAttachments` шлёт свой payload.

## Проверки

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@25
./gradlew build
./gradlew runGameTestServer
python3 tools/check_seams.py
python3 tools/validate_ids.py --mc 1.21.1
python3 tools/gen_resources.py --mc 1.21.1 && git status --short src/generated
```

- Повторная генерация не должна менять файлы.
- Сервер: `tail -f cmds | ./gradlew runServer`, проверять `/reload`, сохранение и перезапуск мира. Визуальную сверку с 26.3 делает пользователь.

## Намеренные отступления от TC4

- Жидкая смерть сохраняет конечный объём (4 четверти на ведро), но ищет кратчайший путь к спуску в пределах 4 горизонтальных шагов. Даже одна четверть может двигаться по этому пути; без доступного спуска остаётся выравнивание TC4.

## Код-стайл

- Java: 4 пробела, без комментариев.
- Python: 4 пробела, двойные кавычки, без комментариев, `pathlib`.
- Коммиты на английском `feat:`/`fix:`, без строк соавторства.
