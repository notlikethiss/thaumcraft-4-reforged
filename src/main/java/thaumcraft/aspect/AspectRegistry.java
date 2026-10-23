package thaumcraft.aspect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;

public final class AspectRegistry {
    private static final int HISTORY_LIMIT = 100;
    private static final int CAP = 64;
    private static volatile AspectRegistry active = new AspectRegistry();

    private final Map<Item, AspectList> tags = new HashMap<>();
    private final Map<EntityType<?>, List<EntityAspects>> entities = new HashMap<>();
    private final Map<Item, List<CraftingSource>> craftingByOutput = new HashMap<>();
    private final Map<Item, List<CraftingSource>> processingByOutput = new HashMap<>();
    private @Nullable MinecraftServer server;

    private record CraftingSource(List<Ingredient> ingredients, int outputCount) {
    }

    public AspectRegistry() {
    }

    public AspectRegistry(MinecraftServer server) {
        this.server = server;
        collectCrafting(server);
    }

    public static AspectRegistry active() {
        return active;
    }

    public static void setActive(AspectRegistry registry) {
        active = registry;
    }

    public static AspectRegistry fromSync(Map<Item, AspectList> tags, Map<EntityType<?>, List<EntityAspects>> entities) {
        AspectRegistry registry = new AspectRegistry();
        tags.forEach((item, aspects) -> registry.tags.put(item, aspects.copy()));
        entities.forEach((type, list) -> registry.entities.put(type, new ArrayList<>(list)));
        return registry;
    }

    public void register(Item item, @Nullable AspectList aspects) {
        tags.put(item, aspects == null ? new AspectList() : aspects.copy());
    }

    public boolean exists(Item item) {
        return tags.containsKey(item);
    }

    public boolean hasAspects(Item item) {
        AspectList existing = tags.get(item);
        return existing != null && !existing.isEmpty();
    }

    public @Nullable AspectList getObjectTags(Item item) {
        AspectList existing = tags.get(item);
        if (existing != null) {
            return cap(existing);
        }
        if (server == null) {
            return null;
        }
        return cap(generate(item, new ArrayList<>()));
    }

    public @Nullable AspectList generate(Item item, List<Item> history) {
        AspectList existing = tags.get(item);
        if (existing != null) {
            return existing.copy();
        }
        if (server == null || history.contains(item)) {
            return null;
        }
        history.add(item);
        if (history.size() >= HISTORY_LIMIT) {
            return null;
        }
        AspectList generated = cap(generateFromRecipes(item, history));
        register(item, generated);
        return generated;
    }

    public void registerEntity(EntityType<?> type, AspectList aspects, Object... nbt) {
        entities.computeIfAbsent(type, key -> new ArrayList<>()).add(EntityAspects.of(aspects, nbt));
    }

    public void mergeEntity(EntityType<?> type, AspectList additions, Object... nbt) {
        List<EntityAspects.Condition> conditions = EntityAspects.of(new AspectList(), nbt).conditions();
        List<EntityAspects> candidates = entities.computeIfAbsent(type, key -> new ArrayList<>());
        for (int index = candidates.size() - 1; index >= 0; index--) {
            EntityAspects candidate = candidates.get(index);
            if (candidate.conditions().equals(conditions)) {
                AspectList merged = candidate.aspects().copy();
                for (Aspect aspect : additions.getAspects()) {
                    merged.add(aspect, additions.getAmount(aspect));
                }
                candidates.set(index, new EntityAspects(merged, candidate.conditions()));
                return;
            }
        }
        candidates.add(EntityAspects.of(additions.copy(), nbt));
    }

    public @Nullable AspectList getEntityTags(Entity entity) {
        List<EntityAspects> candidates = entities.get(entity.getType());
        if (candidates == null) {
            return null;
        }
        CompoundTag data = null;
        AspectList result = null;
        for (EntityAspects candidate : candidates) {
            if (candidate.conditions().isEmpty()) {
                result = candidate.aspects();
                continue;
            }
            if (data == null) {
                data = save(entity);
            }
            if (candidate.matches(data)) {
                result = candidate.aspects();
            }
        }
        return result == null ? null : result.copy();
    }

    public Map<Item, AspectList> buildAll() {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                getObjectTags(item);
            }
        }
        server = null;
        craftingByOutput.clear();
        processingByOutput.clear();
        return exportTags();
    }

    public Map<Item, AspectList> exportTags() {
        Map<Item, AspectList> result = new LinkedHashMap<>();
        tags.forEach((item, aspects) -> {
            if (!aspects.isEmpty()) {
                result.put(item, aspects.copy());
            }
        });
        return result;
    }

    public Map<EntityType<?>, List<EntityAspects>> exportEntities() {
        Map<EntityType<?>, List<EntityAspects>> result = new LinkedHashMap<>();
        entities.forEach((type, list) -> result.put(type, List.copyOf(list)));
        return result;
    }

    private static CompoundTag save(Entity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.level().registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    private static @Nullable AspectList cap(@Nullable AspectList source) {
        if (source == null) {
            return null;
        }
        AspectList out = new AspectList();
        for (Aspect aspect : source.getAspects()) {
            out.merge(aspect, Math.min(CAP, source.getAmount(aspect)));
        }
        return out;
    }

    private @Nullable AspectList generateFromRecipes(Item item, List<Item> history) {
        Context context = new Context(server, history);
        for (RecipeAspectSource source : RecipeAspectSource.SOURCES) {
            AspectList result = source.generate(item, context);
            if (result != null) {
                return result;
            }
        }
        AspectList crafted = generateFromSources(craftingByOutput, item, history);
        if (crafted != null) {
            return crafted;
        }
        return generateFromSources(processingByOutput, item, history);
    }

    private @Nullable AspectList generateFromSources(Map<Item, List<CraftingSource>> sources, Item item, List<Item> history) {
        List<CraftingSource> recipes = sources.getOrDefault(item, List.of());
        AspectList best = null;
        int value = Integer.MAX_VALUE;
        for (CraftingSource recipe : recipes) {
            AspectList candidate = fromItems(resolve(recipe.ingredients(), history), recipe.outputCount(), history);
            if (candidate.visSize() < value && candidate.visSize() > 0) {
                best = candidate;
                value = candidate.visSize();
            }
        }
        if (best != null) {
            return best;
        }
        for (CraftingSource recipe : recipes) {
            AspectList candidate = dominantAspect(resolve(recipe.ingredients(), history), history);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private @Nullable AspectList dominantAspect(List<Item> ingredients, List<Item> history) {
        AspectList mid = new AspectList();
        for (Item ingredient : ingredients) {
            AspectList aspects = generate(ingredient, history);
            if (aspects != null) {
                for (Aspect aspect : aspects.getAspects()) {
                    mid.add(aspect, aspects.getAmount(aspect));
                }
            }
        }
        List<Aspect> sorted = mid.getAspectsSortedAmount();
        return sorted.isEmpty() ? null : new AspectList().add(sorted.getFirst(), 1);
    }

    private List<Item> resolve(List<Ingredient> ingredients, List<Item> history) {
        List<Item> items = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }
            List<Holder<Item>> options = ingredient.items().toList();
            if (options.size() == 1) {
                items.add(options.getFirst().value());
                continue;
            }
            for (Holder<Item> option : options) {
                AspectList aspects = generate(option.value(), history);
                if (aspects != null && aspects.size() > 0) {
                    items.add(option.value());
                    break;
                }
            }
        }
        return items;
    }

    private AspectList fromItems(List<Item> ingredients, int outputCount, List<Item> history) {
        AspectList mid = new AspectList();
        for (Item ingredient : ingredients) {
            AspectList aspects = generate(ingredient, history);
            ItemStackTemplate remainder = ingredient.getCraftingRemainder(new ItemStack(ingredient));
            if (remainder != null) {
                if (remainder.item().value() == ingredient) {
                    continue;
                }
                generate(remainder.item().value(), history);
            }
            if (aspects != null) {
                for (Aspect aspect : aspects.getAspects()) {
                    mid.add(aspect, aspects.getAmount(aspect));
                }
            }
        }
        AspectList out = new AspectList();
        for (Aspect aspect : mid.getAspects()) {
            int amount = (int) (mid.getAmount(aspect) * 0.75F / outputCount);
            if (amount > 0) {
                out.add(aspect, amount);
            }
        }
        return out;
    }

    private void collectCrafting(MinecraftServer minecraftServer) {
        for (RecipeHolder<?> holder : minecraftServer.getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe instanceof ShapedRecipe shaped) {
                ItemStack result = shaped.assemble(CraftingInput.EMPTY);
                if (!result.isEmpty()) {
                    List<Ingredient> ingredients = new ArrayList<>();
                    shaped.getIngredients().forEach(optional -> optional.ifPresent(ingredients::add));
                    craftingByOutput.computeIfAbsent(result.getItem(), key -> new ArrayList<>())
                        .add(new CraftingSource(ingredients, Math.max(1, result.getCount())));
                }
            } else if (recipe instanceof SingleItemRecipe single) {
                ItemStack result = single.assemble(new SingleRecipeInput(ItemStack.EMPTY));
                if (!result.isEmpty()) {
                    processingByOutput.computeIfAbsent(result.getItem(), key -> new ArrayList<>())
                        .add(new CraftingSource(List.of(single.input()), Math.max(1, result.getCount())));
                }
            } else if (recipe instanceof ShapelessRecipe shapeless) {
                ItemStackTemplate result = shapeless.result();
                if (result != null) {
                    List<Ingredient> ingredients = shapeless.placementInfo().ingredients();
                    craftingByOutput.computeIfAbsent(result.item().value(), key -> new ArrayList<>())
                        .add(new CraftingSource(ingredients.subList(0, Math.min(9, ingredients.size())), Math.max(1, result.count())));
                }
            }
        }
    }

    private final class Context implements RecipeAspectSource.Context {
        private final MinecraftServer contextServer;
        private final List<Item> history;

        private Context(MinecraftServer contextServer, List<Item> history) {
            this.contextServer = contextServer;
            this.history = history;
        }

        @Override
        public MinecraftServer server() {
            return contextServer;
        }

        @Override
        public List<Item> history() {
            return history;
        }

        @Override
        public @Nullable AspectList tags(Item item) {
            return generate(item, history);
        }

        @Override
        public AspectList fromIngredients(List<Ingredient> ingredients, int outputCount) {
            return AspectRegistry.this.fromItems(resolve(ingredients, history), outputCount, history);
        }

        @Override
        public AspectList fromItems(List<Item> items, int outputCount) {
            return AspectRegistry.this.fromItems(items, outputCount, history);
        }
    }
}
