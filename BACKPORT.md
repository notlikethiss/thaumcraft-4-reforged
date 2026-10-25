# Backport to Minecraft 26.1.2

## Targets

- Minecraft 26.1.2, NeoForge `26.1.2.122` (FML 11), Java 25 (`export JAVA_HOME=/opt/homebrew/opt/openjdk@25`), ModDevGradle `2.0.148`, official names (no Parchment)
- JEI `29.47.0.111` (`jei-26.1.2-*`), TerraBlender `26.1.2-26.1.2.0.3`, Iris `1.11.4+26.1-neoforge` (compile only)
- Baubles: the own slot implementation of `main` (`platform/baubles`), no Curios
- Branch: `mc/26.1.2`, worktree `~/projects/thaumcraft-4-reforged/.worktrees/mc-26.1.2`

## Last synced main commit

`e9fcba1`

## Sync procedure

1. `git log --reverse e9fcba1..main` in the worktree of this branch.
2. A commit touching only resources, lang or `tools/`: `git cherry-pick`, then regenerate with `python3 tools/gen_resources.py --mc 26.1.2`.
3. A commit with Java: `git show <sha> | python3 tools/backport/rewrite.py --target 26.1.2 | git apply -3`, resolve conflicts by hand, run `compileJava`.
4. Commit with the same message, then update the sha above.
5. Check ids: `python3 tools/validate_ids.py --mc 26.1.2 --reports-dir tools/cache/26.1.2/reports/reports --server-jar tools/cache/26.1.2/server.jar --client-jar tools/cache/26.1.2/client.jar`. The cache is created by `validate_ids.fetch("26.1.2")` and `run_reports`.

## Reference sources

In the main checkout (gitignored): `reference/26.1.2/mc` (net/minecraft, com/mojang, vanilla data and assets) and `reference/26.1.2/neo` (net/neoforged). Rebuild them from `build/moddev/artifacts/minecraft-patched-26.1.2.122-sources.jar` and `neoforge-26.1.2.122-sources.jar`.

## Resource profile

`tools/mcformat.py` `Profile2612` (shared with `main`; the 26.3 output is unchanged):

- Loot tables and loot modifiers use `conditions`/`functions` lists with `condition`/`function` keys, also inside functions; match_tool and block_state_property conditions as on 1.21.1
- Configured features live in `worldgen/configured_feature` with a `config` object; `minecraft:weighted` becomes `minecraft:weighted_state_provider` with `{"Name": ...}` states; `would_survive` takes a state object
- Patch placement is `random_offset` with `xz_spread`/`y_spread`
- Biomes keep environment attributes, but spawns are `spawners`/`spawn_costs` with `minCount`/`maxCount`; spawn biome modifiers use the same spawner format
- Bed rule uses `explodes` instead of `destroy_on_use`
- Villager trades use the `given_item_modifiers` list with `function` keys
- Fuel is the `neoforge:furnace_fuels` data map (`FUELS` in `gen_resources.py`); there is no cooking fuel item component
- Structure palettes use `Name`/`Properties`; DataVersion 4790
- Shaders use `#moj_import`, plain `in`/`out` and no `GL_ARB_separate_shader_objects`; `node.fsh` has no OIT paths

## Branch-only differences

- GameTest pack: the game test server of 26.1.2 creates the world from `minecraft:flat`, so `src/gametest/packs/outer_lands` overrides `world_preset/flat.json` (vanilla 26.1.2 flat plus `thaumcraft:outer_lands`); pack format 101. Tests stand at Y -59, encounter tests bound candidates by `getMinY()`
- GameTest difficulty: 26.1.2 has no `TestEnvironmentDefinition.SetDifficulty`, so `monsters_not_in_peaceful` runs in its own environment and sets and restores the server difficulty itself. `EntityType.create` ignores difficulty on 26.1.2 (natural spawns are gated by `checkMonsterSpawnRules`), so the test checks only the flag and the peaceful despawn
- HUD visibility: `client/platform/HudVisibility` reads `options.hideGui` (26.3 uses `gui.hud.isHidden()`)
- Depthless overlays: 26.1.2 has no see-through feature phase, so `client/platform/SeeThroughGeometry` queues geometry and text and draws them in `RenderLevelStageEvent.AfterLevel` with the main model-view matrix (TC4 drew them in `RenderWorldLastEvent`); the no-depth pipelines keep their `ALWAYS_PASS` depth state
- Survival mock players come from `test/TestPlayers.mockServerPlayer` (26.1.2 has no `makeMockServerPlayer(GameType)`)
- Config: `ModConfig.Type.SERVER` instead of `SYNCED`
- Rendering: no OIT, translucent and additive pipelines are plain sorted pipelines; depth tests use `LESS_THAN_OR_EQUAL` (26.3 uses reverse Z); `withSampler("Sampler1")` instead of bind group layouts
- Warp post effects: `client/platform/WarpPostEffects` processes every active TC4 effect through its own `PostChain` in `RenderLevelStageEvent.AfterLevel`, so they stack like in TC4 and do not replace vanilla spectator effects
- Thaumometer first person: 26.1.2 has no hand render state; `ThaumometerHandRenderer` renders from the live player and stores a pose snapshot per frame for `ThaumometerLens`, spin effect time is mirrored on the client tick
- Menus: no `Prediction`, `drop(stack, false)` and `placeItemBackInInventory(stack)`; the arcane workbench result has no `safeClone` hook (creative middle click clone)
- Worldgen features are `Feature<NoneFeatureConfiguration>` registered in `Registries.FEATURE`; vanilla features are looked up in `Registries.CONFIGURED_FEATURE`
- `platform/baubles/BaubleArmorSlot` replaces the package-private vanilla `ArmorSlot`

## rewrite.py rules (26.1.2)

- Imports: `com.mojang.renderpearl.api.pipeline.*` to `com.mojang.blaze3d.pipeline.*` (`CompareOp` to `blaze3d.platform`, `FilterMode` to `blaze3d.textures`), `EntityTypes` to `EntityType`, `Enderman` to `EnderMan`, `RedstoneWireBlock` to `RedStoneWireBlock`, `core.registries.codec.RegistryCodecs` to `core.RegistryCodecs`
- Constants: `PushReaction.POPPED/IMMOVEABLE` to `DESTROY/BLOCK`, `ModConfig.Type.SYNCED` to `SERVER`, `Items.DYE.red()` to `Items.RED_DYE`
- Calls: `poseStack.rotateDegrees(Axis.X, a)` to `poseStack.mulPose(Axis.X.rotationDegrees(a))`, `poseStack.rotate(q)` to `mulPose(q)`, `minecraft.gui.setScreen/screen()/toastManager()` to `minecraft.setScreen/screen/getToastManager()`

## API differences (26.3 vs 26.1.2)

| Area | 26.3 | 26.1.2 |
|---|---|---|
| Render pipelines | `com.mojang.renderpearl`, OIT pipeline sets, bind group layouts, reverse Z | `com.mojang.blaze3d.pipeline`, no OIT, samplers by name, `LESS_THAN_OR_EQUAL` |
| Post effects | `LocalPlayer` active post effect list | single `GameRenderer` post effect |
| Hand rendering | `RenderHandEvent.getPlayerState()`, camera render state fields | live player, `optionsRenderState` |
| Submit API | `submitModel(..., crumbling)` defaults, `submitMovingBlock(pose, state, outline)` | trailing crumbling argument, no outline color |
| Menus | `Prediction`, `Slot.safeClone` | plain overloads |
| Blocks | `BonemealSource`, `AmbientLeavesBlockSoundPlayer`, optional `codec()` | no source argument, `LeavesBlock(float, Properties)`, `codec()` required |
| Worldgen | `Feature` interface + `Registries.FEATURE_TYPE` | `Feature<FC>` class + `Registries.FEATURE` |
| Biomes | spawns as environment attribute | `MobSpawnSettings`, `addSpawn(category, weight, SpawnerData)` |
| Fuel | `DataComponents.COOKING_FUEL` | `stack.getBurnTime(RecipeType.SMELTING, fuelValues)` + data map |
| Entities | `damageCooldownTime`, `setInvulnerableTime` | `invulnerableTime` field |
| Mob effects | `isInstantaneous` | `isInstantenous` |
| GameTest | `makeMockServerPlayer(GameType)`, `TestData` dimension, `flat_all_dimensions` preset | own mock player, no dimension, `flat` preset |
| Config | `SYNCED` | `SERVER` |

## Validation (2026-10-23)

- `build`, all 256 GameTests, `check_seams.py`, `inventory.py --check`, `validate_ids.py --mc 26.1.2` (0 problems) and deterministic `gen_resources.py --mc 26.1.2` pass.
- Dedicated server: new TerraBlender world (magical forest located), Outer Lands loaded, `/reload` and `save-all` without errors; after a restart the hungry chest inventory, crucible and Outer Lands block remain.
- `runClient` finishes resource loading without errors; the remaining model warnings (field blocks, eldritch portal blocks) and the firebat/wisp spawn placement warning also occur on `main`.
- Visual checks (OIT replacement, post effects, thaumometer lens, node shader) and two-client play remain manual.
