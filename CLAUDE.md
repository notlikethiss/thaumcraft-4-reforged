# Thaumcraft 4: Reforged

Порт Thaumcraft 4.2.3.5 (MC 1.7.10, Azanor) на современный Minecraft. Логика, числа, рецепты, тексты и ассеты оригинала переносятся 1:1, техническая часть пишется на современных API.

Этот файл описывает, как обновлять и портировать мод. Статусы фаз, счётчики тестов, даты прогонов и пути к логам сюда не пишутся. Новая строка добавляется только если без неё агент сделает ошибку: неочевидное правило API, обязательный порядок шагов или намеренное отступление от TC4, которое иначе «починят» обратно.

## Принципы

- Источник истины по поведению: декомпилят TC4 в `reference/decompiled`. Код TC3 Reforged (`~/projects/thaumcraft-reforged`) можно брать как заготовку для классов с общей родословной, логику сверять с TC4.
- Баги TC4 чиним только если они ломают игру (дюп, потеря предметов, NPE, рассинхрон, краш сервера). Игровые особенности оставляем.
- Сервер проверяет то, чему TC4 верил с клиента. Клиентские классы на сервер не протекают, всё должно работать на выделенном сервере.
- Ресурсы делают Python-генераторы в `tools/`, datagen не используется.

## Ветки и версии

| Ветка | MC | Загрузчик | Java | Прочее |
|---|---|---|---|---|
| `main` | 26.3 | NeoForge 26.3.0.57-beta | 25 | JEI 31.9.0.58, TerraBlender 26.3-26.3.0.0.9, свои слоты бижутерии |
| `mc/26.1.2` | 26.1.2 | NeoForge 26.1.2.122 | 25 | JEI 29.47.0.111, TerraBlender 26.1.2-26.1.2.0.3, свои слоты бижутерии |
| `mc/1.21.1` | 1.21.1 | NeoForge 21.1.256 | 21 | Parchment 2024.11.17, JEI 19.57.0.451, TerraBlender 1.21.1-4.1.0.1, Curios 9.5.1 |
| `mc/1.19.2` | 1.19.2 | Forge 43.x (legacyforge) | 17 | планируется, Parchment 2022.11.27 |

Версии и имя мода в `gradle.properties`. Сборка ModDevGradle 2.0.148. CI (`.github/workflows/build.yml`) публикует релиз с тегом `<mc>`.

## Окружение

- JDK: `/opt/homebrew/opt/openjdk@{25,21,17}`, системной `java` нет. Перед Gradle: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25` (на всех ветках, тулчейн выбирается сборкой).
- В облаке (Linux): JDK из Adoptium и симлинки на те же пути, `gradle.properties` не менять.
- Python 3, скрипты `tools/` без внешних зависимостей.

## Справочники (gitignore)

- `Thaumcraft-1.7.10-4.2.3.5.jar` в корне (в облаке cursemaven `thaumcraft-223628/2227552`).
- `reference/decompiled/` TC4 (Vineflower 1.12 + MCP `stable_12-1.7.10`), ассеты оригинала в `reference/decompiled/assets/`, оставшиеся SRG-имена в `reference/decompiled-unmapped.txt`.
- `reference/mc1710/` MC 1.7.10, исходники Forge 10.13.4.1614 и патчи Forge диффами (к ванильным классам не применены, читать рядом).
- `reference/mc`, `reference/neo`: исходники MC и NeoForge текущей версии `main` (из `build/moddev/artifacts` и `neoforge-*-sources.jar`). `reference/26.1.2`, `reference/1.21.1`, `reference/1.19.2` для бэкпортов.
- Пересборка: `tools/fetch_1710.py` (загрузки в `tools/cache/`), затем `tools/decompile.py tc4|mc1710`.

## Архитектура

- `platform/` и `client/platform/` содержат всё, где API версий расходится: сеть (`ModNetwork`, `ClientNetwork`), данные стаков (`StackData`/`StackKey`), attachments (`DataAttachments`/`DataKey`), `Transfer`, `Teleport`, `BiomeEdit`, `Registration` (инструменты, броня, еда, топливо), `Baubles`, `Menus`, `GolemActions`, `OuterLandsData`, рендер-типы и пайплайны. Игровая логика ходит только через них. `tools/check_seams.py` (в CI) падает на запрещённых импортах вне швов.
- Рендер: `client/geom` считает вершины и пишет в `QuadSink` без классов Minecraft (только JOML), `client/platform` реализует sink-и под версию. BER, рендереры и частицы тонкие.
- Виньетка варпа — непрозрачная RGB-маска: смешивание `ZERO / ONE_MINUS_SRC_COLOR`, сила эффекта задаётся RGB, не альфой. На 26.3 использовать `RenderPipelines.VIGNETTE`; обычный alpha-blit закрывает обзор чёрным центром текстуры.
- Регистрация: `registry/` (`DeferredRegister`), пакеты в `network/ModPayloads`, клиентские хендлеры в `client/ClientPayloadHandlers`, capabilities в `registry/ModCapabilities`.
- Meta TC4 раскладывается в отдельные id или свойства blockstate по `tools/id_map.json` (уникальна пара id + state).

## Генерируемые файлы (руками не править)

| Файл | Генератор |
|---|---|
| `aspect/ConfigAspects.java` | `tools/convert_tags.py` |
| `aspect/ModernAspects.java` | `tools/gen_modern_aspects.py` из `tools/modern_aspects/` |
| `research/ConfigResearch.java` | `tools/convert_research.py` |
| `crafting/ConfigRecipes.java`, `tools/tc_recipes.json` | `tools/convert_recipes.py` |
| `client/render/armor/ArmorLayers.java` | `tools/convert_armor_models.py` |
| `src/main/resources/assets/thaumcraft` (текстуры, звуки, `.obj`, lang) | `tools/convert_assets.py` (+ `tools/assets_extra/`, `tools/lang/`) |
| `src/generated/resources` (перезаписывается целиком) | `tools/gen_resources.py [--mc <версия>]` |
| `tools/vanilla_ids.json` | `tools/vanilla_ids.py` |
| `INVENTORY.md` (локально, gitignore) | `tools/inventory.py` |

- После правки lang: `convert_assets.py`, затем `gen_resources.py`. Ручные переводы только en_us и ru_ru в `tools/lang/`.
- Новый генератор фазы подключается в `gen_resources.main()` до финального `gen_phase5.recipes` (иначе рецепты на его предметы считаются ждущими).
- `tools/id_map.json` правится руками, ручные правки `inventory.py` сохраняет. Имена сущностей 1.7.10 в `tools/entity_ids.json`.
- Профили форматов ресурсов по версиям в `tools/mcformat.py`.

## Порт механики из TC4

1. Найти класс в `reference/decompiled`, сверить ванильное поведение 1.7.10 по `reference/mc1710`.
2. Id и состояния взять из `tools/id_map.json`, ванильные id из `tools/vanilla_ids.json`.
3. Версионно-зависимые вызовы спрятать в шов, геометрию вынести в `client/geom`.
4. Ресурсы через генератор, тексты по ключам lang (строки, захардкоженные в TC4, выносить в `tools/lang/ui`).
5. GameTest на ключевую логику в `test/`.

## Проверки

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@25
./gradlew build
./gradlew runGameTestServer
python3 tools/check_seams.py
python3 tools/validate_ids.py --mc 26.3 --reports-dir <dir> --server-jar <jar> --client-jar <jar>
python3 tools/inventory.py --check
python3 tools/gen_resources.py && git status --short src/generated
```

- `validate_ids.py` для 26.3 и 26.1.2 требует явные отчёты ванильного data generator и jar-ы, для 1.21.1 и 1.19.2 скачивает их сам (`--mc 1.21.1`).
- Последняя команда: повторная генерация не должна менять файлы.
- Сервер: `tail -f cmds | ./gradlew runServer` (`cmds` в gitignore, в `run/server.properties` `pause-when-empty-seconds=-1`). Проверять `/reload`, сохранение и перезапуск мира.
- Визуальную проверку в `runClient` и игру двумя клиентами делает пользователь.

## Бэкпорт

- Цепочки `main` → `mc/26.1.2` и `main` → `mc/1.21.1` → `mc/1.19.2`. Ветки в worktree `.worktrees/mc-26.1.2` и `.worktrees/mc-1.21.1`.
- Процедура, sha последней синхронизации и отличия ветки в `BACKPORT.md` этой ветки: Java-коммиты через `git show <sha> | python3 tools/backport/rewrite.py --target <версия> | git apply -3`, ресурсы через `gen_resources.py --mc <версия>`.
- В ветке правятся только швы, `compat/`, регистрация и ресурсы профиля. Правка игровой логики в ветке значит, что на `main` не хватает шва.
- Класс, уже портированный в TC3 `mc/1.21.1`, берётся оттуда.

## Подводные камни 26.3

- Блоки модовых жидкостей добавляются генератором в `minecraft:washed_away_by_fluids`: иначе ванильный `FlowingFluid` не может обновлять уже существующий поток.
- Curios под 26.3 падает на NeoForge 26.3.0.37+ (`ModConfig.Type.COMMON` удалён в FML 12), поэтому на `main` свои слоты за `platform/Baubles`. Конфиги только SYNCED, STARTUP, CLIENT.
- SDL3: левая кнопка мыши `1`, проверять `InputConstants.MOUSE_BUTTON_LEFT`.
- Data components неизменяемые: стеки хранить как `ItemStackTemplate`, наборы как `ItemContainerContents`.
- Цвета в `items/*.json` ARGB: `default` у `minecraft:dye` без альфы делает некрашеный слой прозрачным.
- Еда только через `Registration.food` (Consumable вне `platform/` запрещён). BE сохраняются через ванильные `ValueInput`/`ValueOutput`.
- GameTest: мок-игроку нужен `connection.markClientLoaded()`, иначе он неуязвим. Сервер тестов тикает без пауз, кулдауны по реальному времени ждать в тесте. Outer Lands в тестах подключается датапаком `src/gametest/packs/outer_lands`.
- `onEquipped`/`onUnequipped` бижутерии вызываются только на сервере.

## Намеренные отступления от TC4

- Жидкая смерть сохраняет конечный объём (4 четверти на ведро), но ищет кратчайший путь к спуску в пределах 4 горизонтальных шагов. Даже одна четверть может двигаться по этому пути; без доступного спуска остаётся выравнивание TC4.

- Руды адаптированы к глубине мира: кристаллы от нижней границы до поверхности минус 5, число попыток масштабируется относительно 8 попыток TC4; киноварь в нижней пятой части мира с плотностью 18 попыток на 51 слой. Заменяются обычные и глубинные рудные породы, включая туф. Янтарь сохраняет 20 попыток у поверхности. Только новые чанки, без ретрогенерации.

- Shift+ПКМ жезлом опустошает обычную и пустотную банки эссенции, сохраняя этикетку и фильтр. Действия пустой рукой остаются как в TC4.

- Колокольчик перепривязывает выбранного голема к инвентарю через Shift+ПКМ по его грани. Обычный ПКМ сохраняет назначение рабочих меток. Это добавлено по запросу пользователя; в TC4 колокольчик менял только метки.

- Руки моделей брони TC4 вытянуты по оси руки (`TcArmorModel.SLEEVE_SCALE`): рукава и перчатки TC4 кончаются на y=9.5, рука игрока на y=10, и при взмахе из них торчит кисть.

- Знания игрока в attachment `research/Knowledge` (ключи скана строками), владельцы и связи по UUID и `GlobalPos`, рецепты TC данными в `crafting/TcRecipes` по ключам TC4.
- Блендинг GL_ONE, texgen и `glLineWidth` заменены приближениями (`GuiLines`, вращающиеся UV, шейдер портала Края). Сверка остаётся визуальной.
- Неизученные исследования пишутся шрифтом `thaumcraft:galactic` (`minecraft:alt` плюс кириллица на глифах SGA), в `minecraft:alt` кириллицы нет.
- `allow_cheat_sheet` по умолчанию `false`, веса регионов TerraBlender 3 и 1.

## API для аддонов

- `AspectSync.addRegistrar(Consumer<AspectRegistry>)`: вызывается в `rebuild` после `ConfigAspects`, до `registerDerived`. Прямые правки `AspectRegistry.active()` теряются при `/reload`.
- `AddonAspects.addRegistrar(Consumer<Addon>)`: аддон вызывает из своего конструктора, применяется в `FMLCommonSetupEvent` на обеих сторонах, после инициализации срабатывает сразу. `new Aspect(tag, color, components, image, blend)` регистрирует аспект немедленно: тег без `:`, ровно два компонента, дубликат бросает исключение. Lang `tc.aspect.<tag>` и текстуру аддон добавляет сам.
- `Aspect` реализован классом с реестром: встроенные ordinal 0–47 неизменны, аддонные идут после них по тегу. В сохранениях и пакетах аспект пишется тегом, ordinal годится только внутри сессии.
- `AspectRegistry.mergeEntity(type, aspects, nbt...)` (и `AspectRegistrar.entityCopy`): складывает аспекты с записью сущности, у которой совпадает набор NBT-условий, иначе добавляет новую.
- `WandRegistry.registerCap`/`registerRod`: явные `item` и `texture` Identifier любого namespace, дубликат тега бросает исключение. Ключи исследования `CAP_<tag>`/`ROD_<tag>`, lang `item.Wand.<tag>.cap`/`.rod` аддон добавляет сам.
- `ArcaneCrafting.registerDynamic(DynamicArcaneRecipe)`: добавляется после рецептов жезла и скипетра.
- `Warp.registerItem(Identifier, int)`: изменяемая карта `WARP_ITEMS`.
- `TcArmorMaterial.assetId` принимает полный Identifier ассета брони.
- `TcStack.registerDataHandler(key, BiConsumer<ItemStack, String>)`: `create()` вызывает обработчик для ключа `data`, результат инфузии с таким ключом создаётся через `create()`, а не копией центрального предмета.
- `TcStack.registerDataMatcher(key, BiPredicate<ItemStack, String>)`: `matches()` требует совпадения для каждого ключа `data` с зарегистрированным матчером (аналог сравнения меты в TC4, например тип зелья).
- `FocusPouchItem.pouchSize(stack)` и `canStore(stack)` переопределяются подклассом: статические `getInventory/setInventory` берут размер и фильтр из предмета, переключатель жезла видит все слоты (мешочек ихора TT на 117 слотов).

## Код-стайл

- Java: 4 пробела, без комментариев.
- Python: 4 пробела, двойные кавычки, без комментариев, `pathlib`.
- Коммиты на английском `feat:`/`fix:`, без строк соавторства.
