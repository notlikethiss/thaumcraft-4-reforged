# Backport to Minecraft 1.21.1

## Targets

- Minecraft 1.21.1, NeoForge `21.1.256`, Java 21 (Gradle runs on Java 25: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25`, toolchain 21), ModDevGradle `2.0.148`
- Parchment `1.21.1` / `2024.11.17`
- JEI `19.57.0.451`, TerraBlender `1.21.1-4.1.0.1`, Curios `9.5.1+1.21.1`
- Branch: `mc/1.21.1`, worktree `~/projects/thaumcraft-4-reforged/.worktrees/mc-1.21.1`

## Last synced main commit

`f0c40ed` (phases 1–11; the 2026-10-14 parity fixes are committed on top and do not advance this SHA)

## Sync procedure

1. `git log --reverse f0c40ed..main` in the worktree of this branch.
2. A commit touching only resources, lang or `tools/`: `git cherry-pick`, then regenerate with `python3 tools/gen_resources.py --mc 1.21.1`.
3. A commit with Java: `git show <sha> | python3 tools/backport/rewrite.py --target 1.21.1 | git apply -3`, resolve conflicts by hand, run `compileJava`.
4. Commit with the same message, then update the sha above.
5. Check ids: `python3 tools/validate_ids.py --mc 1.21.1`.

## Reference sources

In the main checkout (gitignored): `reference/1.21.1/mc` (net/minecraft, com/mojang) and `reference/1.21.1/neo` (net/neoforged). The Thaumcraft 3 port of the same classes lives in `~/projects/thaumcraft-reforged-1.21.1` (branch `mc/1.21.1` of TC3 Reforged): a class that exists there is taken from there.

## Thaumometer fixes

Fixes from `6651f14`: localized names for itemless fluid targets, continuous held scanning and a centered third-person hand pose. The lifecycle uses NeoForge's shared `onStopUsing` hook; the regression test uses 1.21.1 interaction results and GameTest annotations. This targeted backport does not advance the global synchronization SHA.

## Branch-only differences (TC4)

- Baubles: `platform/Baubles` is backed by Curios (`amulet`, `ring` x2, `belt` slots). The own slot implementation of `main` (`platform/baubles/*`, `client/platform/baubles/*`) does not exist on this branch
- GameTests: `@GameTestHolder` + `@PrefixGameTestTemplate` instead of the `test_function` registry and `TestEnvironmentDefinition`
- Item models: `items/*.json` client item definitions do not exist; the generator writes `models/item` with `overrides`, special renderers go through `BlockEntityWithoutLevelRenderer` + `IClientItemExtensions`, tints through `RegisterColorHandlersEvent.Item`
- Data attachments: no `.sync()`, `DataAttachments` sends its own payload
- Warp blur post effect: vanilla 1.21.1 has no `blur` program, `gen_warp_shaders.py` writes `box_blur` passes for this profile

## Intentional changes from TC4

- Inventory aspect previews show all available aspect icons and amounts in creative mode without scanning. Survival still requires scanning and aspect discovery. This only changes visibility; player knowledge is not modified. Shared with `main`.

## rewrite.py rules (1.21.1)

- Imports: `Identifier` to `ResourceLocation`, `EntitySpawnReason` to `MobSpawnType`, `EntityTypes` to `EntityType`, `ItemUseAnimation` to `UseAnim`, `VegetationBlock` to `BushBlock`, `ARGB` to `FastColor` (`FastColor.ARGB32.*`), `org.jspecify.annotations.Nullable` to `javax.annotation.Nullable`, `ValueInput`/`ValueOutput` to `thaumcraft.compat.*`
- Package moves: golems (`animal.golem.*` to `animal`), `animal.sheep`, `monster.zombie`, `monster.spider`, `npc.villager`, `projectile.arrow`, `projectile.throwableitemprojectile`, `ParticleStatus`, `BlockAndTintGetter`, `PlayerModel`
- Constants: `EntitySpawnReason.SPAWN_ITEM_USE` to `SPAWN_EGG`, `MobEffects` (SPEED, SLOWNESS, HASTE, MINING_FATIGUE, STRENGTH, INSTANT_HEALTH, INSTANT_DAMAGE, JUMP_BOOST, NAUSEA, RESISTANCE), `PushReaction.POPPED/IMMOVEABLE` to `DESTROY/BLOCK`
- Calls: `x.hurtServer(level, source, amount)` to `x.hurt(source, amount)` (and declarations), `poseStack.rotateDegrees(Axis.X, a)` to `poseStack.mulPose(Axis.X.rotationDegrees(a))`, `.identifier()` to `.location()`, `getMinY()` to `getMinBuildHeight()`, `getSelectedSlot()` to `.selected`, `getNonEquipmentItems()` to `.items`

## API differences (26.3 vs 1.21.1)

- `hurtServer(ServerLevel, ...)` runs only on the server in 26.3. Its 1.21.1 `hurt(...)` adapter is also called by client attacks: reject non-server levels before accessing server state or changing boss phases. A direct `ServerLevel` cast here crashes the client.

| Area | 26.3 | 1.21.1 |
|---|---|---|
| Ids | `Identifier` | `ResourceLocation` |
| Save data | `ValueInput`/`ValueOutput`, `loadAdditional(ValueInput)` | `CompoundTag` + `HolderLookup.Provider` (compat adapters) |
| Saved data | `SavedDataType` | `SavedData.Factory` |
| Item component stack template | `ItemStackTemplate` | `ItemStack.OPTIONAL_CODEC` |
| Item data components getter | `DataComponentGetter` | `DataComponentInput` in `applyImplicitComponents` |
| Registration | `registerBlock(name, fn)` | `register(name, () -> new ...)` with `Properties` |
| Materials | `ToolMaterial`, `ArmorMaterial` + `ArmorType` + `EquipmentAsset` | `Tier`, `ArmorMaterial` holder + `ArmorItem.Type` |
| Item use | `ItemUseAnimation`, `Consumable` | `UseAnim`, `FoodProperties` and `use` overrides |
| Interaction | `InteractionResult.SUCCESS_SERVER`, `useItemOn`/`useWithoutItem` return `InteractionResult` | `ItemInteractionResult`, `InteractionResultHolder` for `use` |
| Block updates | `neighborChanged(..., Orientation)`, `updateShape(..., ScheduledTickAccess, ...)`, `affectNeighborsAfterRemoval` | `neighborChanged(..., Block, BlockPos, boolean)`, `updateShape(state, dir, neighborState, LevelAccessor, pos, neighborPos)`, `onRemove` |
| Entities | `hurtServer(ServerLevel, ...)`, `InsideBlockEffectApplier` | `hurt(...)`, `entityInside(state, level, pos, entity)` |
| Level | `getMinY()/getMaxY()` | `getMinBuildHeight()/getMaxBuildHeight()` (max is exclusive) |
| Inventory | `getSelectedSlot()`, `getNonEquipmentItems()` | `selected`, `items` |
| Potions | `MobEffects.SPEED` etc. | `MOVEMENT_SPEED` etc. |
| Transfer | `ResourceHandler`, `Transaction`, `ItemResource`, `FluidResource` | `IItemHandler`, `IFluidHandler` capabilities |
| Network | client handlers registered separately, `ClientPacketDistributor` | handlers in `playToClient`, `PacketDistributor` |
| Attachments | `.sync()` | custom payload |
| Villagers | `TradeSet`, `villager_trade` data | `VillagerTradesEvent` |
| Recipes | `recipeAccess()`, `RecipeDisplay`, `RecipesReceivedEvent` | `getRecipeManager()`, client `RecipeManager` |
| Fuel | `context_int_provider` item component | `furnace_fuels` data map |
| Furnace AT | `cookingTimer` | `cookingProgress` |
| Render | extract/submit, `SubmitNodeCollector`, render states, `RenderPipeline`, `GuiGraphicsExtractor` | `render(...)` with `MultiBufferSource`, `RenderType`, `GuiGraphics` |
| Entity render | `EntityRenderState`, `AvatarRenderer` | `LivingEntityRenderer`, `PlayerRenderer` |
| Item models | `items/*.json` client item definitions, `SpecialModelRenderer`, tint sources | `models/item` overrides, `BlockEntityWithoutLevelRenderer`, `RegisterColorHandlersEvent` |
| Input events | `KeyEvent`, `MouseButtonEvent` | plain `int` parameters |
| Particles | `SingleQuadParticle.Layer` | `TextureSheetParticle`, `ParticleRenderType` |
| Events | `ExtractLevelRenderStateEvent`, `SubmitCustomGeometryEvent`, `AddClientReloadListenersEvent` | `RenderLevelStageEvent`, `RegisterClientReloadListenersEvent` |
| Annotations | `org.jspecify.annotations.Nullable` | `javax.annotation.Nullable` |

## Compat shims (`thaumcraft/compat`)

- `ValueInput`: `of(CompoundTag, Provider)`, `read(name, codec)`, `read(MapCodec)`, `child`, `childOrEmpty`, `childrenList`, `childrenListOrEmpty`, `list`, `listOrEmpty`, `get*Or`, `getInt`, `getLong`, `getString`, `getIntArray`, `keySet`, `lookup`, `tag`
- `ValueOutput`: `of(CompoundTag, Provider)`, `store(name, codec, value)`, `storeNullable`, `store(MapCodec, value)`, `store(CompoundTag)`, `put*`, `child`, `childrenList`, `list`, `discard`, `isEmpty`, `tag`
- Codec IO goes through `provider.createSerializationContext(NbtOps.INSTANCE)`
- `TcBlockEntity` bridges `loadAdditional(CompoundTag, Provider)` and `saveAdditional(CompoundTag, Provider)` to `loadAdditional(ValueInput)` and `saveAdditional(ValueOutput)`. Block entities extending vanilla classes directly need the same bridge: `ArcaneBoreBlockEntity`, `HungryChestBlockEntity` (both `BaseContainerBlockEntity`), `InfernalFurnaceBlockEntity` (`BlockEntity`)

## Deviations on this branch

- Curios: slot ids `necklace`, `ring` (size raised to 2 by `data/curios/curios/slots/ring.json` with `ADD`), `belt`; `Baubles` slot order 0 necklace, 1-2 ring, 3 belt; `platform/BaublesCurios` registers every `Bauble` item as a curio in common setup
- Data attachments: `DataAttachments.SyncPayload` instead of `.sync()`, player attachments are resent on login, respawn and dimension change
- Village trades: `world/village/VillageTrades` (`VillagerTradesEvent`), the whole pool is added to every level 1-5 and vanilla picks 2, same tables as the `villager_trade` data on `main`; `tools/backport/trades.json` is the manifest written by the generator
- GameTests: names are `thaumcraft.<class>.<method>`; relative Y is one higher than on `main` (1.21.1 puts the structure block layer at relative Y 0); `runGameTestServer` uses `level-name` from `run/server.properties`; use separate names for GameTests and normal server worlds
- Tools from `Registration` use the `TOOL` component on a plain `Item` (no right-click block actions); fast food eats in 0.8 s instead of 0.5 s; goggles extend `ArmorItem` with the armor layer `textures/models/armor/goggles_layer_1.png`
- Blocks: `PlantSoil.PLAINS` uses `BlockTags.DIRT`; no `getEntityInsideCollisionShape`; workbench result slot without `safeClone`; bellows speed up furnaces through `CookTime`
- Rendering: `WorldFx` draws in `RenderLevelStageEvent` (`AFTER_PARTICLES`) instead of vanilla particles; translucent layer order follows call order instead of `order(n)`; REPEAT sampler is the default; GUI `nextStratum` became `flush`, tooltips draw immediately; the first person sleeve is always drawn
- Item models: special items render through `client/render/TcItemRenderer` (`BlockEntityWithoutLevelRenderer`) by `tools/backport/item_models_1.21.1.json`, tints through `ItemColor`
- Worldgen: patches (`count` + `offset` with `trapezoid` on 26.3) are written as `minecraft:random_patch` with `xz_spread` 7 and `y_spread` 3
- Aspects: smelting recipes (`AbstractCookingRecipe`) are read separately because they are not `SingleItemRecipe` on 1.21.1; `minecraft:boat` and `minecraft:chest_boat` entities have entries in `tools/modern_aspects/entities.json`

## Phase 8 validation (2026-10-11)

- compileJava, check_seams.py, validate_ids.py --mc 1.21.1 pass; all 32 GameTests pass (29 ported, 3 backport regressions).
- Dedicated server loads a normal TerraBlender world; focal manipulator and warded block placement, /reload, focus inventory and save-all pass without ERROR entries.
- Armor materials are registered before registry events; robe/void robe materials have dyeable base and undyed overlay layers. Dye recipes are generated and the six robes support water cauldron washing.
- compat/ItemStackTemplate preserves immutable item-stack components with value equality; raw ItemStack component values fail development validation.
- GearMovementPayload relays forward input for server traveller boots/haste because vanilla 1.21.1 lacks getLastClientInput.
- platform/CamouflagedBlockEntity owns ModelData/ModelProperty; only its two server-safe NeoForge model-data imports are exempted from the client-package seam rule.
- Visual validation and two-client animation/synchronization checks remain pending.

## Phase 9 validation (2026-10-11)

- `build` passes; all 60 GameTests pass (57 ported tests and 3 backport regressions). `check_seams.py` checks 654 files; `validate_ids.py --mc 1.21.1` reports zero problems.
- Resource generation produces 1129 JSON files for 1.21.1. The isolated modern profile still generates 1393 JSON files.
- Dedicated server creates a normal TerraBlender world, places devices, reloads data and saves without ERROR entries. After a full server restart, focal manipulator focus, warded block disguise, hungry chest and bore inventories, spa water (1000 mB), brain jar XP (17), and banner color/aspect are retained. The bore needs its base; redstone pauses spa fluid consumption during the persistence check.
- `SidedInvWrapper` marks a container changed before setting its slot in 1.21.1; the grate delivers inserted items from `setItem` as well. The open/closed grate GameTest verifies insertion, dropped item count, collisions and redstone closure.
- Block entity renderers use the 1.21.1 direct rendering API. Warded disguises use `ModelData`; connected warded glass uses a custom baked-model loader and 47 variants. Special device items use `BlockEntityWithoutLevelRenderer`; mirrors/lodestone use legacy item predicates.
- `runClient` completes resource loading without Thaumcraft errors or missing models/textures. Vanilla sound/shader warnings and focus texture mipmap reductions remain in the development log. In-world visual checks and two-client animation/synchronization checks remain pending.


## Phase 10–11 backport status

- Common entities, champions, aspect orbs, fluids/taint/warp, direct entity/block rendering, post shaders, GameTest source and generated resource/tool changes are present. The 1.21.1 taint fibres model uses the `thaumcraft:taint_fibres` geometry loader and ordinary blockstate model references.
- Final automated verification completed: build passed, all 122 GameTests passed, and `python3 tools/validate_ids.py --mc 1.21.1` reported zero problems. This does not constitute visual validation or user gameplay testing; those checks remain open.

## Parity fixes (2026-10-14)

- Applied parity corrections for phases 1–11 only. The eight production-file changes cover primal-orb taint spread, uncommon/rare loot tier stack preservation and replacement, focal-manipulator canceled-upgrade drain behavior, excavation focus cost caching, Brain Jar research bonus, and the champion whitelist clamp. Backport-specific API adapters are recorded alongside the code changes.
- Added regression GameTests for the fixes, including deterministic primal-orb taint spread, focus loot tier pick/save/load and replacement, excavation shared lazy pricing, canceled focal manipulation, Brain Jar, champion clamping, and warp encounter/effect behavior. The test suite passed as part of the 122/122 run above.
- `vignette.png` and its `.mcmeta` come from `convert_assets.py` in `src/main/resources`, as on `main`; `gen_warp_shaders.py` no longer copies them into `src/generated`.
- The synced base remains `f0c40ed`. No phase 12 or 13 content was backported. Automated checks do not replace visual or user gameplay validation.

## Phases 12–13 and reported port bugs (2026-10-16)

- Ported the complete golem/traveling-trunk and Outer Lands/eldritch feature sets, including registries, components, menus, entity persistence, world generation, encounters and resource generators. The phase baseline is `80b327a`; phase 12–13 behavior corrections through `95dcec5` are included. This does not advance the global synchronization SHA or import phase 14 configuration/JEI changes.
- Applied the shared port-bug fixes: original trunk dimensions/model origin, guardian population/light checks, grounded natural eldritch spawning, authoritative following-drop movement and velocity updates, and opaque depth-writing eldritch surfaces with batched layers and hidden internal faces.
- `FollowingItemMotion` owns item movement/lifetime API differences. Access transformers expose only item age and pickup delay in addition to the existing adapters; boss AI uses the same removal of `serverAiStep` finality as main.
- `OuterLandsData` retains the shared codec through the 1.21.1 SavedData factory. Golem fluid components use immutable `compat/FluidStackTemplate`; transfer and fake-player actions use the existing capability APIs.
- Client adapters in `client/platform` provide immediate geometry submission, model state, item rendering and block-entity rendering on the 1.21.1 API. The golem model scale is restored after every pose reset. Item predicates use the legacy item-model profile.
- `runGameTestServer` prepares the Outer Lands pack in `build/gametestworld/datapacks`, with its own world selected explicitly. New encounter tests use separate batches to avoid interference with existing tests. A larger regression structure keeps following drops and their moving target inside active chunks.
- Resource generation now writes the correct dimension-type and biome spawn formats for both profiles. Portal orientation remains the TC4 behavior; the underspecified vertical-portal report does not justify rotating either portal.
- Visual acceptance, two-client synchronization and comparative FPS measurements remain manual checks. Automated server tests do not establish those results.

### Automated validation

- 26.3: `build` and all 200 required GameTests pass. The crusher drop/pickup scenario also passes five consecutive repetitions.
- 1.21.1: `build` and all 173 required GameTests pass, including target removal and dimension-change release of following drops.
- Both profiles: seam checks, vanilla identifier validation (zero problems), inventory checks, deterministic resource regeneration and headless eldritch geometry checks pass.
- Separate dedicated-server worlds on both versions generated the maze, spawned a boss after lock activation, completed `/reload` and saved. Restart preserved maze readiness and the altar eye count.
- During validation, a Gradle compile cache entry restored an incomplete set of classes on main. The final main build and tests used `--no-build-cache`; both final jars were inspected for the required entity/network/world classes.

## Ore depth adaptation

- Shared with main: infused stone spans the world bottom to the surface minus five, with attempts scaled from TC4's eight to preserve approximate density; cinnabar spans the bottom fifth with 18 attempts per 51 layers. Amber retains 20 near-surface attempts. Stone/deepslate ore replacement tags and tuff are accepted. Surface biome controls crystal aspect bias. Existing chunks are not regenerated.
- Uses the branch's existing `WorldGenConfig` switches, `getMinBuildHeight()` and annotation-based GameTests. The global synchronization SHA is unchanged.

## Aura node jar rendering

- Shared with main: the node jar inherits the warded jar model, node glow uses full block and sky light, and the empty node jar item is hidden from the creative tab and JEI. Captured node jars retain their aspects and scan identity.
- Iris is a compile-only dependency. While a shader pack is active, `client/platform/NodeRenderTypes` uses the vanilla particle shader getter intercepted by Iris; otherwise it retains the custom node shader with its low alpha cutoff. Main uses pipeline assignments instead. No Iris classes are loaded when the mod is absent.

## Addon API for Thaumic Tinkerer

The 1.21.1 branch exposes the addon contracts from main: aspect registrars before derived aspects, explicit wand component items/textures with duplicate checks, dynamic arcane recipes, item warp registration, stack data writers/matchers and custom infusion results. Research availability and extensible focus pouch inventories are also supported. Existing wand constructors and string armor material names remain valid; foreign armor texture namespaces are preserved.

The changes are limited to these integration contracts and their five regression GameTests. All 228 required base GameTests passed after the additions. An earlier full run had one failing `mindspiderspawner` scenario; the following full run passed without changing encounter code.

## Portable resource validation

Vanilla report generation selects a suitable JDK from the version-specific JAVA_HOME variables supplied by CI, JAVA_HOME, or PATH. It checks the runtime version and reports how to configure Java if none is suitable. No Homebrew installation path is required. Regression tests cover Linux CI selection and generation when the registry report cache is absent.

## Release 1.1 branding

The root `icon.png` and `poster.png` are packaged under `assets/thaumcraft/textures/gui/branding`. NeoForge 21.1 uses the mod-specific `logoFile` for the in-game banner and ignores the newer `bannerFile` and `iconFile` fields. Prism Launcher prefers the top-level `logoFile`, which points to the square icon; the mod-specific value points to the poster. This keeps each image's aspect ratio appropriate for its display.

## Targeted fluid physics fix

Backported `ae19e55` and `ead4042`: finite liquid death drain routing, fluid physics regression tests, and the version-specific `platform/FluidPhysics` factory. NeoForge 21.1 already supplies water movement for custom fluids, so this factory does not use the newer `isWaterLike` API. The 26.3-only fluid spreading tag is generated only for that profile. This targeted backport does not advance the global synchronization SHA.

Before the backport, clean NeoForge 21.1.256 passed the new movement, ascent, exit, current and water-flow comparison scenarios. The reported complete freeze was not reproduced; exact user runtime versions and mod list are still needed. The new finite drain routing scenarios failed before the fix.

Validation: build, all 246 required GameTests, and seam checks pass, including the stationary-item regression. Generated resources are unchanged.

## Targeted add-on aspect API

Backported PR #37 with the follow-up fix (`6989c6a..7df0506` on `main`): `Aspect` as a registry-backed class, `AddonAspects`, `AspectRegistry.mergeEntity`, tag-based `AspectOrb` sync and the add-on aspect GameTest. `Aspect.java` keeps the 1.21.1 `@Nullable Aspect[]` annotation placement, `Thaumcraft.java` gets the common setup listener by hand, and the test uses `@GameTestHolder`. The branch CLAUDE.md and README point to `main` for the add-on API. This targeted backport does not advance the global synchronization SHA.

Validation: build, all 247 required GameTests and seam checks pass.
