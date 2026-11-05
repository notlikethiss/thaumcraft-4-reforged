package thaumcraft.infusion;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectContainer;
import thaumcraft.aspect.AspectList;
import thaumcraft.block.InfusionStabiliser;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.crafting.InfusionCrafting;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.effect.EffectHooks;
import thaumcraft.essentia.EssentiaHandler;
import thaumcraft.fx.Fx;
import thaumcraft.network.InfusionSourceFxPayload;
import thaumcraft.network.NodeZapPayload;
import thaumcraft.platform.ModNetwork;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;
import thaumcraft.wand.WandInteractable;

public class InfusionMatrixBlockEntity extends TcBlockEntity implements WandInteractable, AspectContainer {
    private static final int SYNC_RADIUS = 32;

    private final List<BlockPos> pedestals = new ArrayList<>();
    private boolean active;
    private boolean crafting;
    private boolean checkSurroundings = true;
    private int symmetry;
    private int instability;
    private AspectList recipeEssentia = new AspectList();
    private List<Integer> recipeRemaining = new ArrayList<>();
    private @Nullable List<TcIngredient> recipeComponents;
    private String recipeKey = "";
    private int recipeType;
    private ItemStack recipeInput = ItemStack.EMPTY;
    private int recipeInstability;
    private int recipeXp;
    private @Nullable UUID recipePlayer;
    private int count;
    private int countDelay = 10;
    private int itemCount;
    private int craftCount;
    private float startUp;
    private final Map<String, SourceFx> sourceFx = new HashMap<>();

    public static final class SourceFx {
        private final BlockPos location;
        private int ticks;
        private final int color;

        private SourceFx(BlockPos location, int ticks, int color) {
            this.location = location;
            this.ticks = ticks;
            this.color = color;
        }

        public BlockPos getLocation() {
            return location;
        }

        public int getTicks() {
            return ticks;
        }

        public int getColor() {
            return color;
        }
    }

    public InfusionMatrixBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.INFUSION_MATRIX.get(), pos, state);
    }

    protected InfusionMatrixBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, InfusionMatrixBlockEntity entity) {
        entity.tick();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        active = value;
        sync();
    }

    public boolean isCrafting() {
        return crafting;
    }

    public int getInstability() {
        return instability;
    }

    public int getSymmetry() {
        return symmetry;
    }

    public int getCraftCount() {
        return craftCount;
    }

    public float getStartUp() {
        return startUp;
    }

    public int getCount() {
        return count;
    }

    public List<BlockPos> getPedestals() {
        return pedestals;
    }

    public Map<String, SourceFx> getSourceFx() {
        return sourceFx;
    }

    public void markSurroundingsDirty() {
        checkSurroundings = true;
    }

    public void recalculateSurroundings() {
        getSurroundings();
        checkSurroundings = false;
    }

    private void tick() {
        count++;
        if (checkSurroundings) {
            checkSurroundings = false;
            getSurroundings();
        }
        if (level.isClientSide()) {
            doEffects();
            return;
        }
        if (count % (crafting ? 20 : 100) == 0 && !validLocation()) {
            active = false;
            sync();
            return;
        }
        if (active && crafting && count % countDelay == 0) {
            craftCycle((ServerLevel) level);
            setChanged();
        }
    }

    public boolean validLocation() {
        BlockPos base = worldPosition.below(2);
        if (!(level.getBlockEntity(base) instanceof PedestalBlockEntity)) {
            return false;
        }
        return level.getBlockEntity(base.offset(1, 0, 1)) instanceof InfusionPillarBlockEntity
            && level.getBlockEntity(base.offset(1, 0, -1)) instanceof InfusionPillarBlockEntity
            && level.getBlockEntity(base.offset(-1, 0, -1)) instanceof InfusionPillarBlockEntity
            && level.getBlockEntity(base.offset(-1, 0, 1)) instanceof InfusionPillarBlockEntity;
    }

    private @Nullable PedestalBlockEntity getCenterPedestal() {
        return level.getBlockEntity(worldPosition.below(2)) instanceof PedestalBlockEntity pedestal ? pedestal : null;
    }

    @Override
    public InteractionResult onWandUseOn(Level useLevel, BlockPos pos, Player player, ItemStack wand, Direction side) {
        if (active && !crafting) {
            if (!useLevel.isClientSide()) {
                craftingStart(player);
            }
            return InteractionResult.SUCCESS;
        }
        if (!active && validLocation()) {
            if (!useLevel.isClientSide()) {
                active = true;
                sync();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void craftingStart(Player player) {
        if (!validLocation()) {
            active = false;
            sync();
            return;
        }
        getSurroundings();
        recipeInput = ItemStack.EMPTY;
        PedestalBlockEntity center = getCenterPedestal();
        if (center != null && !center.getStack().isEmpty()) {
            recipeInput = center.getStack().copy();
        }
        if (recipeInput.isEmpty()) {
            return;
        }
        List<ItemStack> components = new ArrayList<>();
        for (BlockPos pedestalPos : pedestals) {
            if (level.getBlockEntity(pedestalPos) instanceof PedestalBlockEntity pedestal && !pedestal.getStack().isEmpty()) {
                components.add(pedestal.getStack().copy());
            }
        }
        if (components.isEmpty()) {
            return;
        }
        InfusionCrafting.Plan plan = InfusionCrafting.findRecipe(components, recipeInput, player);
        if (plan == null) {
            plan = InfusionCrafting.findEnchantment(level, components, recipeInput, player);
        }
        if (plan == null) {
            return;
        }
        recipeType = plan.type();
        recipeKey = plan.key();
        recipeComponents = plan.components();
        recipeRemaining = new ArrayList<>();
        for (int index = 0; index < plan.components().size(); index++) {
            recipeRemaining.add(index);
        }
        recipeInstability = plan.instability();
        recipeEssentia = plan.aspects();
        recipeXp = plan.xp();
        recipePlayer = player.getUUID();
        instability = symmetry + recipeInstability;
        crafting = true;
        level.playSound(null, worldPosition, ModSounds.CRAFTSTART.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        sync();
    }

    private @Nullable List<TcIngredient> recipeIngredients() {
        if (recipeComponents == null && !recipeInput.isEmpty()) {
            InfusionCrafting.Plan plan = InfusionCrafting.resolve(level, recipeType, recipeKey, recipeInput);
            if (plan != null) {
                recipeComponents = plan.components();
            }
        }
        return recipeComponents;
    }

    private void craftCycle(ServerLevel serverLevel) {
        RandomSource random = serverLevel.getRandom();
        boolean valid = false;
        PedestalBlockEntity center = getCenterPedestal();
        if (center != null && !center.getStack().isEmpty() && !recipeInput.isEmpty()
            && ItemStack.isSameItemSameComponents(center.getStack(), recipeInput) && recipeIngredients() != null) {
            valid = true;
        }
        if (!valid || instability > 0 && random.nextInt(500) <= instability) {
            switch (random.nextInt(21)) {
                case 0, 2, 10, 13 -> inEvEjectItem(serverLevel, 0);
                case 1, 11 -> inEvEjectItem(serverLevel, 2);
                case 3, 8, 14 -> inEvZap(serverLevel, false);
                case 4, 15 -> inEvEjectItem(serverLevel, 5);
                case 5, 16 -> inEvHarm(serverLevel, false);
                case 6, 17 -> inEvEjectItem(serverLevel, 1);
                case 7 -> inEvEjectItem(serverLevel, 4);
                case 9 -> serverLevel.explode(
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    1.5F + random.nextFloat(),
                    Level.ExplosionInteraction.BLOCK
                );
                case 12 -> inEvZap(serverLevel, true);
                case 18 -> inEvHarm(serverLevel, true);
                case 19 -> inEvEjectItem(serverLevel, 3);
                default -> inEvWarp(serverLevel);
            }
            if (valid) {
                return;
            }
        }
        if (!valid) {
            instability = 0;
            crafting = false;
            recipeEssentia = new AspectList();
            serverLevel.playSound(null, worldPosition, ModSounds.CRAFTFAIL.get(), SoundSource.BLOCKS, 1.0F, 0.6F);
            sync();
            return;
        }
        if (recipeType == InfusionCrafting.TYPE_ENCHANTMENT && recipeXp > 0) {
            drainExperience(serverLevel);
            return;
        }
        if (recipeType == InfusionCrafting.TYPE_ENCHANTMENT && recipeXp == 0) {
            countDelay = 10;
        }
        if (recipeEssentia.visSize() > 0) {
            for (Aspect aspect : recipeEssentia.getAspects()) {
                if (recipeEssentia.getAmount(aspect) > 0) {
                    if (EssentiaHandler.drainEssentia(this, aspect, null, 12)) {
                        recipeEssentia.reduce(aspect, 1);
                        sync();
                        return;
                    }
                    if (random.nextInt(Math.max(1, 100 - recipeInstability * 3)) == 0) {
                        instability++;
                    }
                    if (instability > 25) {
                        instability = 25;
                    }
                    sync();
                }
            }
            checkSurroundings = true;
        } else if (recipeRemaining.isEmpty()) {
            instability = 0;
            crafting = false;
            craftingFinish(serverLevel);
            sync();
        } else {
            consumeIngredient(serverLevel, random);
        }
    }

    private void drainExperience(ServerLevel serverLevel) {
        RandomSource random = serverLevel.getRandom();
        List<Player> targets = serverLevel.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(10.0));
        if (targets.isEmpty()) {
            return;
        }
        for (Player target : targets) {
            if (target.experienceLevel > 0) {
                target.giveExperienceLevels(-1);
                recipeXp--;
                target.hurtServer(serverLevel, serverLevel.damageSources().magic(), random.nextInt(2));
                ModNetwork.sendToNear(
                    serverLevel,
                    null,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ(),
                    SYNC_RADIUS,
                    new InfusionSourceFxPayload(worldPosition, (byte) 0, (byte) 0, (byte) 0, target.getId())
                );
                serverLevel.playSound(null, target, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 2.0F + random.nextFloat() * 0.4F);
                countDelay = 20;
                return;
            }
        }
        List<Aspect> aspects = recipeEssentia.getAspects();
        if (!aspects.isEmpty() && random.nextInt(3) == 0) {
            addInstabilityEssentia(random, aspects);
        }
    }

    private void addInstabilityEssentia(RandomSource random, List<Aspect> aspects) {
        Aspect aspect = aspects.get(random.nextInt(aspects.size()));
        recipeEssentia.add(aspect, 1);
        if (random.nextInt(Math.max(1, 50 - recipeInstability * 2)) == 0) {
            instability++;
        }
        if (instability > 25) {
            instability = 25;
        }
        sync();
    }

    private void consumeIngredient(ServerLevel serverLevel, RandomSource random) {
        List<TcIngredient> components = recipeIngredients();
        for (int index = 0; index < recipeRemaining.size(); index++) {
            TcIngredient ingredient = components.get(recipeRemaining.get(index));
            for (BlockPos pedestalPos : pedestals) {
                if (serverLevel.getBlockEntity(pedestalPos) instanceof PedestalBlockEntity pedestal
                    && !pedestal.getStack().isEmpty()
                    && ingredient.test(pedestal.getStack())) {
                    if (itemCount == 0) {
                        itemCount = 5;
                        ModNetwork.sendToNear(
                            serverLevel,
                            null,
                            worldPosition.getX(),
                            worldPosition.getY(),
                            worldPosition.getZ(),
                            SYNC_RADIUS,
                            new InfusionSourceFxPayload(
                                worldPosition,
                                (byte) (worldPosition.getX() - pedestalPos.getX()),
                                (byte) (worldPosition.getY() - pedestalPos.getY()),
                                (byte) (worldPosition.getZ() - pedestalPos.getZ()),
                                0
                            )
                        );
                    } else if (itemCount-- <= 1) {
                        ItemStack current = pedestal.getStack();
                        ItemStackTemplate remainder = current.getItem().getCraftingRemainder(current);
                        pedestal.setStackFromInfusion(remainder == null ? ItemStack.EMPTY : remainder.create());
                        recipeRemaining.remove(index);
                    }
                    return;
                }
            }
            List<Aspect> aspects = recipeEssentia.getAspects();
            if (!aspects.isEmpty() && random.nextInt(1 + index) == 0) {
                addInstabilityEssentia(random, aspects);
            }
        }
    }

    private List<LivingEntity> nearbyLiving(ServerLevel serverLevel) {
        return serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(10.0));
    }

    private void inEvZap(ServerLevel serverLevel, boolean all) {
        for (LivingEntity target : nearbyLiving(serverLevel)) {
            ModNetwork.sendToNear(
                serverLevel,
                null,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                SYNC_RADIUS,
                new NodeZapPayload(
                    worldPosition.getX() + 0.5F,
                    worldPosition.getY() + 0.5F,
                    worldPosition.getZ() + 0.5F,
                    (float) target.getX(),
                    (float) target.getY() + target.getBbHeight() / 2.0F,
                    (float) target.getZ()
                )
            );
            DamageSource magic = serverLevel.damageSources().magic();
            target.hurtServer(serverLevel, magic, 4 + serverLevel.getRandom().nextInt(4));
            if (!all) {
                break;
            }
        }
    }

    private void inEvHarm(ServerLevel serverLevel, boolean all) {
        for (LivingEntity target : nearbyLiving(serverLevel)) {
            if (serverLevel.getRandom().nextBoolean()) {
                EffectHooks.applyTaintPoison(target, 120, 0);
            } else {
                EffectHooks.applyVisExhaust(target, 2400, 0);
            }
            if (!all) {
                break;
            }
        }
    }

    private void inEvWarp(ServerLevel serverLevel) {
        List<Player> targets = serverLevel.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(10.0));
        if (targets.isEmpty()) {
            return;
        }
        RandomSource random = serverLevel.getRandom();
        if (targets.get(random.nextInt(targets.size())) instanceof ServerPlayer target) {
            if (random.nextFloat() < 0.25F) {
                Warp.add(target, 1, WarpType.STICKY);
            } else {
                Warp.add(target, 1 + random.nextInt(5), WarpType.TEMPORARY);
            }
        }
    }

    private void inEvEjectItem(ServerLevel serverLevel, int type) {
        RandomSource random = serverLevel.getRandom();
        for (int attempt = 0; attempt < 50 && !pedestals.isEmpty(); attempt++) {
            BlockPos pedestalPos = pedestals.get(random.nextInt(pedestals.size()));
            if (!(serverLevel.getBlockEntity(pedestalPos) instanceof PedestalBlockEntity pedestal) || pedestal.getStack().isEmpty()) {
                continue;
            }
            if (type >= 3 && type != 5) {
                pedestal.removeStack();
            } else {
                Containers.dropItemStack(
                    serverLevel,
                    pedestalPos.getX() + 0.5,
                    pedestalPos.getY() + 0.5,
                    pedestalPos.getZ() + 0.5,
                    pedestal.removeStack()
                );
            }
            BlockPos above = pedestalPos.above();
            if (type == 1 || type == 3) {
                serverLevel.setBlock(above, ModBlocks.FLUX_GOO.get().defaultBlockState(), Block.UPDATE_ALL);
                serverLevel.playSound(null, pedestalPos, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, 0.3F, 1.0F);
            } else if (type == 2 || type == 4) {
                serverLevel.setBlock(above, ModBlocks.FLUX_GAS.get().defaultBlockState(), Block.UPDATE_ALL);
                serverLevel.playSound(null, pedestalPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.0F);
            } else if (type == 5) {
                serverLevel.explode(
                    null,
                    pedestalPos.getX() + 0.5,
                    pedestalPos.getY() + 0.5,
                    pedestalPos.getZ() + 0.5,
                    1.0F,
                    Level.ExplosionInteraction.BLOCK
                );
            }
            serverLevel.blockEvent(pedestalPos, ModBlocks.ARCANE_PEDESTAL.get(), 11, 0);
            ModNetwork.sendToNear(
                serverLevel,
                null,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                SYNC_RADIUS,
                new NodeZapPayload(
                    worldPosition.getX() + 0.5F,
                    worldPosition.getY() + 0.5F,
                    worldPosition.getZ() + 0.5F,
                    pedestalPos.getX() + 0.5F,
                    pedestalPos.getY() + 1.5F,
                    pedestalPos.getZ() + 0.5F
                )
            );
            return;
        }
    }

    private void craftingFinish(ServerLevel serverLevel) {
        PedestalBlockEntity center = getCenterPedestal();
        if (center == null) {
            return;
        }
        ItemStack result = InfusionCrafting.result(serverLevel, recipeType, recipeKey, center.getStack());
        if (!result.isEmpty()) {
            center.setStackFromInfusion(result);
        }
        if (recipePlayer != null && serverLevel.getPlayerByUUID(recipePlayer) instanceof ServerPlayer player) {
            InfusionHooks.onInfusionCrafted(player, center.getStack());
        }
        recipeEssentia = new AspectList();
        recipeKey = "";
        recipeComponents = null;
        sync();
        serverLevel.blockEvent(center.getBlockPos(), ModBlocks.ARCANE_PEDESTAL.get(), 12, 0);
    }

    public void onBlockRemoved() {
        if (crafting && level != null && !level.isClientSide()) {
            crafting = false;
            level.explode(
                null,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                2.0F,
                Level.ExplosionInteraction.BLOCK
            );
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        onBlockRemoved();
    }

    private boolean isStabiliser(BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        return block instanceof AbstractSkullBlock || block instanceof InfusionStabiliser stabiliser && stabiliser.canStabiliseInfusion(level, pos);
    }

    private void getSurroundings() {
        List<BlockPos> stabilisers = new ArrayList<>();
        pedestals.clear();
        for (int xx = -12; xx <= 12; xx++) {
            for (int zz = -12; zz <= 12; zz++) {
                boolean skip = false;
                for (int yy = -5; yy <= 10; yy++) {
                    if (xx == 0 && zz == 0) {
                        continue;
                    }
                    BlockPos check = worldPosition.offset(xx, -yy, zz);
                    if (!level.hasChunkAt(check)) {
                        continue;
                    }
                    if (!skip && yy > 0 && Math.abs(xx) <= 8 && Math.abs(zz) <= 8 && level.getBlockEntity(check) instanceof PedestalBlockEntity) {
                        pedestals.add(check);
                        skip = true;
                    } else if (isStabiliser(check)) {
                        stabilisers.add(check);
                    }
                }
            }
        }
        symmetry = 0;
        for (BlockPos pedestalPos : pedestals) {
            boolean items = false;
            int x = worldPosition.getX() - pedestalPos.getX();
            int z = worldPosition.getZ() - pedestalPos.getZ();
            if (level.getBlockEntity(pedestalPos) instanceof PedestalBlockEntity pedestal) {
                symmetry += 2;
                if (!pedestal.getStack().isEmpty()) {
                    symmetry++;
                    items = true;
                }
            }
            BlockPos mirror = new BlockPos(worldPosition.getX() + x, pedestalPos.getY(), worldPosition.getZ() + z);
            if (level.hasChunkAt(mirror) && level.getBlockEntity(mirror) instanceof PedestalBlockEntity mirrored) {
                symmetry -= 2;
                if (!mirrored.getStack().isEmpty() && items) {
                    symmetry--;
                }
            }
        }
        float stabiliserSymmetry = 0.0F;
        for (BlockPos stabiliserPos : stabilisers) {
            int x = worldPosition.getX() - stabiliserPos.getX();
            int z = worldPosition.getZ() - stabiliserPos.getZ();
            if (isStabiliser(stabiliserPos)) {
                stabiliserSymmetry += 0.1F;
            }
            BlockPos mirror = new BlockPos(worldPosition.getX() + x, stabiliserPos.getY(), worldPosition.getZ() + z);
            if (level.hasChunkAt(mirror)) {
                Block mirrored = level.getBlockState(mirror).getBlock();
                if (mirrored instanceof AbstractSkullBlock
                    || mirrored instanceof InfusionStabiliser stabiliser && stabiliser.canStabiliseInfusion(level, stabiliserPos)) {
                    stabiliserSymmetry -= 0.2F;
                }
            }
        }
        symmetry = (int) (symmetry + stabiliserSymmetry);
    }

    public void addSourceFx(int dx, int dy, int dz, int color) {
        BlockPos target = worldPosition.offset(-dx, -dy, -dz);
        String key = target.getX() + ":" + target.getY() + ":" + target.getZ() + ":" + color;
        int ticks = level != null && level.getBlockEntity(target) instanceof PedestalBlockEntity ? 60 : 15;
        SourceFx existing = sourceFx.get(key);
        if (existing != null) {
            existing.ticks = ticks;
        } else {
            sourceFx.put(key, new SourceFx(target, ticks, color));
        }
    }

    private void doEffects() {
        RandomSource random = level.getRandom();
        if (crafting) {
            if (craftCount == 0) {
                level.playLocalSound(worldPosition, ModSounds.INFUSERSTART.get(), SoundSource.BLOCKS, 0.5F, 1.0F, false);
            } else if (craftCount % 65 == 0) {
                level.playLocalSound(worldPosition, ModSounds.INFUSER.get(), SoundSource.BLOCKS, 0.5F, 1.0F, false);
            }
            craftCount++;
            Fx.get().blockRunes(
                level,
                worldPosition.getX(),
                worldPosition.getY() - 2,
                worldPosition.getZ(),
                0.5F + random.nextFloat() * 0.2F,
                0.1F,
                0.7F + random.nextFloat() * 0.3F,
                25,
                -0.03F
            );
        } else if (craftCount > 0) {
            craftCount -= 2;
            if (craftCount < 0) {
                craftCount = 0;
            }
            if (craftCount > 50) {
                craftCount = 50;
            }
        }
        if (active && startUp != 1.0F) {
            if (startUp < 1.0F) {
                startUp = startUp + Math.max(startUp / 10.0F, 0.001F);
            }
            if (startUp > 0.999) {
                startUp = 1.0F;
            }
        }
        if (!active && startUp > 0.0F) {
            startUp = startUp - startUp / 10.0F;
            if (startUp < 0.001) {
                startUp = 0.0F;
            }
        }
        for (String fxKey : new ArrayList<>(sourceFx.keySet())) {
            SourceFx fx = sourceFx.get(fxKey);
            if (fx.ticks <= 0) {
                sourceFx.remove(fxKey);
                continue;
            }
            if (fx.location.equals(worldPosition)) {
                Entity entity = level.getEntity(fx.color);
                if (entity != null) {
                    for (int index = 0; index < 2; index++) {
                        Fx.get().infusionSparkle(
                            level,
                            entity.getX() + (random.nextFloat() - random.nextFloat()) * entity.getBbWidth(),
                            entity.getBoundingBox().minY + random.nextFloat() * entity.getBbHeight(),
                            entity.getZ() + (random.nextFloat() - random.nextFloat()) * entity.getBbWidth(),
                            worldPosition,
                            true
                        );
                    }
                }
            } else if (level.getBlockEntity(fx.location) instanceof PedestalBlockEntity pedestal) {
                ItemStack stack = pedestal.getStack();
                if (!stack.isEmpty()) {
                    spawnPedestalParticles(random, fx.location, stack);
                }
            } else {
                fx.ticks = 0;
            }
            fx.ticks--;
        }
        if (crafting && instability > 0 && random.nextInt(200) <= instability) {
            Fx.get().nodeBolt(
                level,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 0.5F,
                worldPosition.getZ() + 0.5F,
                worldPosition.getX() + 0.5F + (random.nextFloat() - random.nextFloat()) * 2.0F,
                worldPosition.getY() + 0.5F + (random.nextFloat() - random.nextFloat()) * 2.0F,
                worldPosition.getZ() + 0.5F + (random.nextFloat() - random.nextFloat()) * 2.0F
            );
        }
    }

    private void spawnPedestalParticles(RandomSource random, BlockPos location, ItemStack stack) {
        if (random.nextInt(3) == 0) {
            Fx.get().infusionSparkle(
                level,
                location.getX() + random.nextFloat(),
                location.getY() + random.nextFloat() + 1.0F,
                location.getZ() + random.nextFloat(),
                worldPosition,
                false
            );
        } else if (stack.getItem() instanceof BlockItem blockItem) {
            for (int index = 0; index < 2; index++) {
                Fx.get().infusionBlockParticles(
                    level,
                    location.getX() + random.nextFloat(),
                    location.getY() + random.nextFloat() + 1.0F,
                    location.getZ() + random.nextFloat(),
                    worldPosition,
                    blockItem.getBlock().defaultBlockState()
                );
            }
        } else {
            for (int index = 0; index < 2; index++) {
                Fx.get().infusionItemParticles(
                    level,
                    location.getX() + 0.4F + random.nextFloat() * 0.2F,
                    location.getY() + 1.23F + random.nextFloat() * 0.2F,
                    location.getZ() + 0.4F + random.nextFloat() * 0.2F,
                    worldPosition,
                    stack
                );
            }
        }
    }

    @Override
    public AspectList getAspects() {
        return recipeEssentia;
    }

    @Override
    public void setAspects(AspectList aspects) {
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return true;
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        return 0;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return false;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return 0;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        active = input.getBooleanOr("active", false);
        crafting = input.getBooleanOr("crafting", false);
        instability = input.getIntOr("instability", 0);
        recipeEssentia = input.read("recipeEssentia", AspectList.CODEC).orElseGet(AspectList::new);
        recipeKey = input.getStringOr("recipeKey", "");
        recipeType = input.getIntOr("recipeType", 0);
        recipeRemaining = new ArrayList<>(input.read("recipeRemaining", Codec.INT.listOf()).orElseGet(List::of));
        recipeComponents = null;
        recipeInput = input.read("recipeInput", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        recipeInstability = input.getIntOr("recipeInstability", 0);
        recipeXp = input.getIntOr("recipeXp", 0);
        Optional<String> player = input.getString("recipePlayer");
        recipePlayer = player.filter(value -> !value.isEmpty()).map(UUID::fromString).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("active", active);
        output.putBoolean("crafting", crafting);
        output.putInt("instability", instability);
        output.store("recipeEssentia", AspectList.CODEC, recipeEssentia);
        output.putString("recipeKey", recipeKey);
        output.putInt("recipeType", recipeType);
        output.store("recipeRemaining", Codec.INT.listOf(), recipeRemaining);
        output.store("recipeInput", ItemStack.OPTIONAL_CODEC, recipeInput);
        output.putInt("recipeInstability", recipeInstability);
        output.putInt("recipeXp", recipeXp);
        output.putString("recipePlayer", recipePlayer == null ? "" : recipePlayer.toString());
    }
}
