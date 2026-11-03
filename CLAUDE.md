# Thaumcraft 4: Reforged

Порт Thaumcraft 4.2.3.5 (MC 1.7.10, Azanor) на современный Minecraft. Порт завершён, идёт поддержка: исправления и точечные правки. Логика, числа, рецепты, тексты и ассеты оригинала сохраняются 1:1, техническая часть на современных API.

Сюда пишутся только правила, без которых агент ошибётся: неочевидное поведение API, обязательный порядок шагов, намеренные отступления от TC4. Статусы, счётчики, даты и история сюда не пишутся.

## Принципы

- Источник истины по поведению: декомпилят TC4 в `reference/decompiled`. Код TC3 Reforged (`~/projects/thaumcraft-reforged`) годится как заготовка для классов с общей родословной, логика сверяется с TC4.
- Баги TC4 чиним только если они ломают игру (дюп, потеря предметов, NPE, рассинхрон, краш сервера). Игровые особенности оставляем.
- Сервер проверяет то, чему TC4 верил с клиента. Клиентские классы на сервер не протекают, всё работает на выделенном сервере.
- GameTest-ы не пишутся ни в одной ветке. Логику проверяет пользователь в `runClient` и `runServer`.

## Ветки

- `main`: MC 26.3, NeoForge. `mc/26.1.2`: NeoForge, Java 25. `mc/1.21.1`: NeoForge, Java 21, Curios. `mc/1.19.2` (Forge через legacyforge, Java 17) планируется.
- Версии MC, загрузчика и зависимостей смотреть в `gradle.properties` нужной ветки. CI (`.github/workflows/build.yml`) публикует релиз с тегом `<mc>`.

## Окружение

- JDK: `/opt/homebrew/opt/openjdk@{25,21,17}`, системной `java` нет. Перед Gradle `export JAVA_HOME=/opt/homebrew/opt/openjdk@25` на любой ветке, тулчейн выбирает сборка.
- В облаке (Linux): JDK из Adoptium с симлинками на те же пути, `gradle.properties` не менять.
- Python 3, скрипты `tools/` без внешних зависимостей.

## Справочники (gitignore)

- `Thaumcraft-1.7.10-4.2.3.5.jar` в корне (в облаке cursemaven `thaumcraft-223628/2227552`).
- `reference/decompiled/`: TC4 (Vineflower 1.12 + MCP `stable_12-1.7.10`), ассеты оригинала в `reference/decompiled/assets/`, оставшиеся SRG-имена в `reference/decompiled-unmapped.txt`.
- `reference/mc1710/`: MC 1.7.10, исходники Forge 10.13.4.1614 и патчи Forge диффами (к ванильным классам не применены, читать рядом).
- `reference/mc`, `reference/neo`: исходники MC и NeoForge версии `main`. `reference/26.1.2`, `reference/1.21.1`, `reference/1.19.2` для веток.
- `tools/data/id_map.json` (meta TC4 → id и состояние), `vanilla_ids.json` (ванильные id 1.7.10), `entity_ids.json` (имена сущностей 1.7.10): справочники для ручного переноса, скрипты их не читают. Meta TC4 раскладывается в отдельные id или свойства blockstate по `id_map.json`.

## Архитектура

- `platform/` и `client/platform/` содержат всё, где API версий расходится: сеть (`ModNetwork`, `ClientNetwork`), данные стаков (`StackData`/`StackKey`), attachments (`DataAttachments`/`DataKey`), `Transfer`, `Teleport`, `BiomeEdit`, `Registration` (инструменты, броня, еда, топливо), `Baubles`, `Menus`, `GolemActions`, `OuterLandsData`, рендер-типы и пайплайны. Игровая логика ходит только через них, `tools/check_seams.py` (в CI) падает на запрещённых импортах.
- Рендер: `client/geom` считает вершины и пишет в `QuadSink` без классов Minecraft (только JOML), `client/platform` реализует sink-и под версию. BER, рендереры и частицы тонкие.
- Регистрация в `registry/` (`DeferredRegister`), пакеты в `network/ModPayloads`, клиентские хендлеры в `client/ClientPayloadHandlers`, capabilities в `registry/ModCapabilities`.
- Знания игрока лежат в attachment `research/Knowledge` (ключи скана строками), владельцы и связи хранятся по UUID и `GlobalPos`, рецепты TC данными в `crafting/TcRecipes` по ключам TC4.
- Публичные хуки для аддонов описаны в `docs/ADDON_API.md`. Их не удалять как неиспользуемые, а при изменении сигнатуры обновлять документ.

## Ресурсы

- `src/generated/resources` целиком перезаписывает `python3 tools/gen_resources.py [--mc <версия>]`, руками не править. Профили форматов по версиям в `tools/mcformat.py`.
- Генераторы в `tools/gen/`, модуль на домен (`blocks`, `items`, `recipes`, `nodes`, `wands`, `arcane`, `alchemy`, `equipment`, `devices`, `machines`, `taint`, `entities`, `golems`, `eldritch`, `biomes`, `world` и т.д.). Общие хелперы в `gen/context.py` (`Context`) и `gen/common.py`. Порядок шагов задан списком `STEPS` в `gen/__init__.py`.
- `recipes.tc_recipes` идёт после всех генераторов предметов: он проверяет, что предметы из `tools/data/tc_recipes.json` определены, а рецепт не дублирует уже записанный. Дубль или неизвестный предмет роняет генерацию. Рецепт, который уже пишет другой генератор, оформляется в JSON как `alias`.
- Руками правятся: `aspect/ConfigAspects.java`, `aspect/ModernAspects.java`, `research/ConfigResearch.java`, `crafting/ConfigRecipes.java` (вместе с `tools/data/tc_recipes.json`), `client/render/armor/ArmorLayers.java`, ассеты и lang в `src/main/resources/assets/thaumcraft`.
- Lang: вручную поддерживаются en_us и ru_ru. Строки, захардкоженные в TC4, выносятся в оба файла.

## Порт механики из TC4

1. Найти класс в `reference/decompiled`, сверить ванильное поведение 1.7.10 по `reference/mc1710`.
2. Id и состояния взять из `tools/data/id_map.json` и `tools/data/vanilla_ids.json`.
3. Версионно-зависимые вызовы спрятать в шов, геометрию вынести в `client/geom`.
4. Ресурсы через генератор, тексты по ключам lang.

## Проверки

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@25
./gradlew build
python3 tools/check_seams.py
python3 tools/gen_resources.py && git status --short src/generated
```

- Повторная генерация не должна менять файлы.
- `tools/validate_ids.py --mc <версия>` сверяет id с ванильными данными. Для 1.21.1 и 1.19.2 он скачивает их сам, для 26.x нужны `--reports-dir`, `--server-jar`, `--client-jar`.
- Сервер: `tail -f cmds | ./gradlew runServer` (`cmds` в gitignore, в `run/server.properties` `pause-when-empty-seconds=-1`). Проверять `/reload`, сохранение и перезапуск мира.
- Визуальную проверку в `runClient` и игру двумя клиентами делает пользователь.

## Бэкпорт

- Цепочки `main` → `mc/26.1.2` и `main` → `mc/1.21.1` → `mc/1.19.2`. Ветки открыты в worktree `.worktrees/mc-26.1.2` и `.worktrees/mc-1.21.1`, команды бэкпорта выполняются там.
- Sha последней синхронизации записан в `CLAUDE.md` ветки. Коммиты `main` после него переносятся по порядку с тем же сообщением: Java через `git show <sha> | python3 tools/backport/rewrite.py --target <версия> | git apply -3` (скрипт лежит только в ветках), ресурсы через `gen_resources.py --mc <версия>`, затем sha обновляется.
- Последний синхронизированный коммит `main` в `mc/26.1.2`: `325c922`.
- В ветке правятся только швы, `compat/`, регистрация, профиль ресурсов и `tools/backport/`. Правка игровой логики в ветке значит, что на `main` не хватает шва.

## Подводные камни 26.3

- Блоки модовых жидкостей добавляются генератором в `minecraft:washed_away_by_fluids`, иначе ванильный `FlowingFluid` не может обновлять уже существующий поток.
- Curios под 26.3 падает на NeoForge 26.3.0.37+ (`ModConfig.Type.COMMON` удалён в FML 12), поэтому на `main` свои слоты за `platform/Baubles`. Конфиги только SYNCED, STARTUP, CLIENT.
- SDL3: левая кнопка мыши `1`, проверять `InputConstants.MOUSE_BUTTON_LEFT`.
- Data components неизменяемые: стеки хранить как `ItemStackTemplate`, наборы как `ItemContainerContents`.
- Цвета в `items/*.json` ARGB: `default` у `minecraft:dye` без альфы делает некрашеный слой прозрачным.
- Еда только через `Registration.food` (Consumable вне `platform/` запрещён). BE сохраняются через ванильные `ValueInput`/`ValueOutput`.
- `onEquipped`/`onUnequipped` бижутерии вызываются только на сервере.
- Виньетка варпа работает как непрозрачная RGB-маска: смешивание `ZERO / ONE_MINUS_SRC_COLOR`, сила задаётся RGB, а альфа не участвует. Использовать `RenderPipelines.VIGNETTE`, обычный alpha-blit закрывает обзор чёрным центром текстуры.
- Блендинг GL_ONE, texgen и `glLineWidth` заменены приближениями (`GuiLines`, вращающиеся UV, шейдер портала Края). Сверка остаётся визуальной.
- Неизученные исследования пишутся шрифтом `thaumcraft:galactic` (`minecraft:alt` плюс кириллица на глифах SGA), потому что в `minecraft:alt` кириллицы нет.

## Намеренные отступления от TC4

Эти отличия сделаны специально, обратно к TC4 их не «чинить».

- Жидкая смерть сохраняет конечный объём (4 четверти на ведро), но ищет кратчайший путь к спуску в пределах 4 горизонтальных шагов. Даже одна четверть может двигаться по этому пути; без доступного спуска остаётся выравнивание TC4.
- Руды адаптированы к глубине мира: кристаллы от нижней границы до поверхности минус 5, число попыток масштабируется относительно 8 попыток TC4; киноварь в нижней пятой части мира с плотностью 18 попыток на 51 слой. Заменяются обычные и глубинные рудные породы, включая туф. Янтарь сохраняет 20 попыток у поверхности. Только новые чанки, без ретрогенерации.
- Shift+ПКМ жезлом опустошает обычную и пустотную банки эссенции, сохраняя этикетку и фильтр. Действия пустой рукой остаются как в TC4.
- Колокольчик перепривязывает выбранного голема к инвентарю через Shift+ПКМ по его грани. Обычный ПКМ сохраняет назначение рабочих меток, как в TC4.
- Руки моделей брони TC4 вытянуты по оси руки (`TcArmorModel.SLEEVE_SCALE`): рукава и перчатки TC4 кончаются на y=9.5, рука игрока на y=10, и при взмахе из них торчит кисть.
- `allow_cheat_sheet` по умолчанию `false`, веса регионов TerraBlender 3 и 1.

## Код-стайл

- Java: 4 пробела, без комментариев.
- Python: 4 пробела, двойные кавычки, без комментариев, `pathlib`.
- Коммиты на английском `feat:`/`fix:`, без строк соавторства.
