# Add-on API (experimental)

All hooks are plain static calls on Thaumcraft classes. Call them from your mod constructor unless noted otherwise. The client and the server must load the same add-ons.

## Aspects

- `AddonAspects.addRegistrar(api -> api.register(tag, color, components, blend, texture))` registers a new aspect.
  - Registrars run during `FMLCommonSetupEvent` on both sides. A registrar added after setup runs immediately.
  - Tags are global and must not contain `:`. Each aspect needs exactly two components. A duplicate tag throws.
  - The add-on provides the `tc.aspect.<tag>` translation and the texture.
- `Aspect` is a registry-backed class. Built-in ordinals 0-47 are stable, add-on aspects follow in tag order. Saves and packets store the tag; an ordinal is only valid within one session.
- `AspectSync.addRegistrar(Consumer<AspectRegistry>)` adds item and entity aspects. It runs on every rebuild, including `/reload`, after the built-in tables and before derived aspects are computed. Direct edits to `AspectRegistry.active()` are lost on `/reload`.
- `AspectRegistry.mergeEntity(type, aspects, nbt...)` and `AspectRegistrar.entityCopy(...)` add amounts to the entity entry with the same NBT conditions, or create a new entry.

## Wands

- `WandRegistry.registerCap(tag, discount, special, specialDiscount, item, craftCost, metal, texture)` and `WandRegistry.registerRod(base, capacity, item, craftCost, update, glowing, staff, runes, texture)`.
  - `item` and `texture` are full `Identifier`s in any namespace. A duplicate tag throws.
  - The add-on provides the research keys `CAP_<tag>` / `ROD_<tag>` and the translations `item.Wand.<tag>.cap` / `item.Wand.<tag>.rod`.
- `WandTriggers.register(Predicate<BlockState>, WandTrigger)` adds a wand right-click action on a block.
- Focus pouches: subclass `FocusPouchItem` and override `pouchSize(stack)` and `canStore(stack)`. The static `getInventory` / `setInventory` and the wand focus switcher use the item's size and filter.

## Crafting

- `ArcaneCrafting.registerDynamic(DynamicArcaneRecipe)` adds an arcane recipe computed at match time. Dynamic recipes are checked after the wand and sceptre recipes.
- `TcStack.registerDataHandler(key, BiConsumer<ItemStack, String>)`: `TcStack.create()` calls the handler for a recipe stack with this `data` key. An infusion result with such a key is created through `create()` instead of copying the central item.
- `TcStack.registerDataMatcher(key, BiPredicate<ItemStack, String>)`: `TcStack.matches()` requires the matcher to accept every `data` value with this key, like the metadata check in TC4 (for example a potion type).

## Other hooks

- `Warp.registerItem(Identifier, int)` makes an item give warp when crafted.
- `CropUtils.addClickableCrop(Predicate<BlockState>)` marks a grown crop that golems harvest by right-click.
- `GolemFluids.registerDrainable(Predicate<BlockState>, Drainable)` lets liquid golems drain a custom fluid block.
- `TcArmorMaterial` takes a full `Identifier` for the armor asset, so add-on armor can use its own namespace.
