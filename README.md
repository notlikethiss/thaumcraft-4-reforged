# Thaumcraft 4: Reforged

[![Thaumcraft 4: Reforged](poster.png)](poster.png)

Unofficial port of **Thaumcraft 4.2.3.5** by Azanor from Minecraft 1.7.10 to **Minecraft 26.3** on **NeoForge 26.3**.

The goal is to recreate the original gameplay as closely as possible: aspects, aura nodes, vis and flux, wands and foci, research and the Thaumonomicon, alchemy and essentia networks, arcane and infusion crafting, warp, taint, golems and the Outer Lands. Mechanics and numbers are ported from the decompiled original. The technical side is rebuilt on modern APIs (data components, attachments, payloads, render states), with JEI recipe integration and TerraBlender biome generation.

> **Personal use only.** Thaumcraft and all of its assets belong to Azanor. This repository contains the original mod's logic and assets and is not intended for redistribution.

The `mc/26.1.2` and `mc/1.21.1` branches contain backports; Minecraft 1.19.2 is planned. Manual visual and multiplayer checks are still in progress.

## Requirements to build

- Java 25
- Python 3 (for the conversion, resource generation and validation scripts)

Versions used by the main branch:

- Mod `1.1.1`
- Minecraft `26.3`
- NeoForge `26.3.0.57-beta`
- ModDevGradle `2.0.148`
- TerraBlender `26.3-26.3.0.0.9` (required)
- JEI `31.9.0.58` (optional at runtime; included in development runs)

## Releases

Every push to `main` or `mc/*` that passes the workflow's path filters builds the mod on GitHub Actions and publishes it on the [Releases](https://github.com/notlikethiss/thaumcraft-4-reforged/releases) page. The mod version comes from `mod_version` in `gradle.properties`, independently of the original Thaumcraft version.

For Minecraft 26.3, version `1.1.1` is published as a release named `26.3` with the tag `26.3` and the file `thaumcraft-4-reforged-1.1.1-26.3.jar`. Tags contain only the Minecraft version. Each build replaces the release for that Minecraft version. Releases from `main` are marked as latest.

The root `icon.png` and `poster.png` are packaged into the JAR as the mod list icon and banner. The icon is also declared through `logoFile` for Prism Launcher compatibility.

## Building and running

```sh
export JAVA_HOME=/path/to/openjdk-25

./gradlew compileJava         # compile
./gradlew build               # build the mod jar into build/libs
./gradlew runClient           # start a development client
./gradlew runServer           # start a development server (game folder: run/)
./gradlew runGameTestServer   # run the automated game tests
python3 tools/check_seams.py  # check version-specific API boundaries (also runs in CI)
```

The server's stdin is forwarded, so it can be driven by a file of commands:

```sh
tail -f cmds | ./gradlew runServer
echo "forceload add 0 0" >> cmds
```

## Admin commands

- `/thaumcraft research <players> <key|all>` grants research
- `/thaumcraft research <players> reset` resets research
- `/thaumcraft aspect <players> <aspect|all> <amount>` grants research points
- `/thaumcraft warp <players> set <amount> [perm|temp|sticky]` sets warp
- `/thaumcraft wand fill [players]` fills a held wand with vis
- `/thaumcraft node info <x> <y> <z>` inspects an aura node
- `/thaumcraft outer prepare <x> <y> <z>` prepares an Outer Lands maze
- `/thaumcraft outer info <x> <y> <z>` checks maze status

## Project layout

```text
src/main/java/thaumcraft/
  aspect/        aspects, aspect lists and aspect registry
  node/          aura nodes, stabilization, jars and transduction
  vis/           vis relay networks and energized nodes
  wand/          wands, staves, sceptres, foci and upgrades
  research/      research list, player knowledge, scanning and research notes
  alchemy/       crucible, alchemical furnaces and processing
  essentia/      essentia containers, tubes and suction
  arcane/        arcane crafting
  infusion/      infusion crafting and enchantments
  crafting/      Thaumcraft recipe registry
  taint/         taint spread and biome changes
  eldritch/      eldritch content and portals
  entity/        creatures, champions, cultists, bosses and golems
  world/         world generation, structures, villages and Outer Lands
  device/        magical devices, machines and mirrors
  block/         blocks
  blockentity/   block entities
  item/          items and equipment
  menu/          container menus
  network/       payloads
  platform/      version-specific API implementations
  registry/      content registration
  command/       admin and development commands
  client/        screens, renderers, geometry, particles, HUD, research book and JEI
  test/          automated game tests
src/main/templates/      mod metadata templates
src/main/resources/     converted textures, sounds, lang files and structure data
src/generated/resources/ generated blockstates, models, loot tables, recipes and tags
src/gametest/           game test data packs
tools/                  Python conversion, generation and validation scripts
reference/              original mod and platform sources (git ignored)
```

## Tools

Resources are generated by Python scripts instead of datagen:

| Script                        | Purpose                                                                                            |
| ----------------------------- | -------------------------------------------------------------------------------------------------- |
| `tools/convert_assets.py`     | Converts original textures, sounds and `.lang` files; merges extra translations from `tools/lang/` |
| `tools/gen_resources.py`      | Generates blockstates, models, loot tables, recipes and tags, including phase-specific resources   |
| `tools/convert_tags.py`       | Generates `aspect/ConfigAspects.java` from the original aspect registrations                       |
| `tools/gen_modern_aspects.py` | Generates aspects for modern Minecraft content from `tools/modern_aspects/`                        |
| `tools/convert_research.py`   | Generates `research/ConfigResearch.java` from the original research config                         |
| `tools/convert_recipes.py`    | Converts the original recipe config into the port's recipe registry and recipe data                |
| `tools/convert_structures.py` | Converts the mound structure from the original world generator                                     |
| `tools/remap_srg.py`          | Remaps SRG names in the decompiled source to MCP names                                             |
| `tools/check_seams.py`        | Checks version-specific API boundaries                                                             |
| `tools/validate_ids.py`       | Validates resource IDs and references                                                              |

The conversion scripts read from `reference/` (git ignored), which holds the original mod decompiled with Vineflower and remapped to MCP 1.7.10 names, plus Minecraft and NeoForge sources for reference. Version-specific resource formats are defined in `tools/mcformat.py`.

## Localization

All 22 original language files are converted, including the research texts. Manual additions and corrections are maintained for English and Russian in `tools/lang/` and merged by `tools/convert_assets.py`.

## Add-on API (experimental)

- Custom aspects: call `AddonAspects.addRegistrar(api -> api.register(tag, color, components, blend, texture))` from the add-on constructor. Callbacks run during `FMLCommonSetupEvent` on both sides, so the client and the server must load the same add-ons. Tags are global and must not contain `:`, each aspect needs exactly two components, and the add-on provides the `tc.aspect.<tag>` translation and the texture.
- `Aspect` is a registry-backed class. Built-in ordinals 0-47 are stable, add-on aspects follow in tag order. Saves and packets store tags.
- Item and entity aspects: register them through `AspectSync.addRegistrar(registry -> ...)`, which runs on every rebuild including `/reload`. `AspectRegistry.mergeEntity` adds amounts to an entity entry with the same NBT conditions.

## Credits

- **Azanor** for the original Thaumcraft
- **CovertJaguar, Dan Scott, Lorc, bonemouse, Scott Killen, Rorax and chicken_bones**, credited by the original mod
- The NeoForge and TerraBlender teams and the JEI authors
