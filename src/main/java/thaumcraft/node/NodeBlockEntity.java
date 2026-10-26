package thaumcraft.node;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectHelper;
import thaumcraft.aspect.AspectList;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.fx.Fx;
import thaumcraft.network.NodeZapPayload;
import thaumcraft.platform.BiomeEdit;
import thaumcraft.platform.ModNetwork;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.ResearchGate;
import thaumcraft.research.ScanManager;
import thaumcraft.wand.WandInteractable;
import thaumcraft.wand.WandKeys;
import thaumcraft.wand.Wands;
import thaumcraft.Config;

public class NodeBlockEntity extends TcBlockEntity implements Node, WandInteractable {
    private static final ResourceKey<Biome> TAINTED_LAND = ResourceKey.create(Registries.BIOME, Thaumcraft.id("tainted_land"));
    private static final ResourceKey<Biome> MAGICAL_FOREST = ResourceKey.create(Registries.BIOME, Thaumcraft.id("magical_forest"));
    private static final ResourceKey<Biome> EERIE = ResourceKey.create(Registries.BIOME, Thaumcraft.id("eerie"));

    private long lastActive = 0L;
    private AspectList aspects = new AspectList();
    private AspectList aspectsBase = new AspectList();
    private NodeType nodeType = NodeType.NORMAL;
    private @Nullable NodeModifier nodeModifier = null;
    private int count = 0;
    private int regeneration = -1;
    private int wait = 0;
    private @Nullable String id = null;
    private byte nodeLock = 0;
    private boolean catchUp = false;
    private @Nullable Entity drainEntity = null;
    private @Nullable HitResult drainCollision = null;
    private int drainEntityId = -1;
    private int drainTicks;
    private int drainColor = 0xFFFFFF;
    private int targetColor = 0xFFFFFF;
    private int color = 0xFFFFFF;

    public NodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NODE.get(), pos, state);
    }

    public static <T extends BlockEntity> @Nullable BlockEntityTicker<T> ticker(BlockEntityType<T> type) {
        if (type != ModBlockEntities.NODE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> ((NodeBlockEntity) entity).tick(tickLevel);
    }

    @Override
    public String getId() {
        if (id == null) {
            generateId();
        }
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id.isEmpty() ? null : id;
        setChanged();
    }

    private String generateId() {
        String dimension = level == null ? "unknown" : level.dimension().identifier().toString();
        id = dimension + ":" + worldPosition.getX() + ":" + worldPosition.getY() + ":" + worldPosition.getZ();
        return id;
    }

    public int getColor() {
        return color;
    }

    public int getTargetColor() {
        return targetColor;
    }

    public int getDrainColor() {
        return drainColor;
    }

    public @Nullable Entity getDrainEntity() {
        if (drainEntity == null && drainTicks > 0 && level != null) {
            return level.getEntity(drainEntityId);
        }
        return drainEntity;
    }

    public @Nullable HitResult getDrainCollision() {
        if (drainCollision == null && drainTicks > 0) {
            return new BlockHitResult(Vec3.atCenterOf(worldPosition), Direction.UP, worldPosition, false);
        }
        return drainCollision;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("drainEntity", drainEntity == null ? -1 : drainEntity.getId());
        return tag;
    }

    public boolean isHungry() {
        return nodeType == NodeType.HUNGRY;
    }

    public int getCount() {
        return count;
    }

    public byte getLock() {
        return nodeLock;
    }

    @Override
    public AspectList getAspects() {
        return aspects;
    }

    @Override
    public AspectList getAspectsBase() {
        return aspectsBase;
    }

    @Override
    public void setAspects(AspectList aspects) {
        this.aspects = aspects;
        this.aspectsBase = aspects.copy();
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        int left = amount + aspects.getAmount(aspect) - aspectsBase.getAmount(aspect);
        left = Math.max(left, 0);
        aspects.add(aspect, amount - left);
        return left;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        return aspects.reduce(aspect, amount);
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
    public boolean doesContainerAccept(Aspect aspect) {
        return true;
    }

    public @Nullable Aspect takeRandomPrimalFromSource() {
        List<Aspect> primals = aspects.getPrimalAspects();
        if (primals.isEmpty()) {
            return null;
        }
        Aspect aspect = primals.get(level.getRandom().nextInt(primals.size()));
        return aspects.reduce(aspect, 1) ? aspect : null;
    }

    public @Nullable Aspect chooseRandomFilteredFromSource(AspectList filter, boolean preserve) {
        int min = preserve ? 1 : 0;
        List<Aspect> valid = new ArrayList<>();
        for (Aspect aspect : aspects.getAspects()) {
            if (filter.getAmount(aspect) > 0 && aspects.getAmount(aspect) > min) {
                valid.add(aspect);
            }
        }
        if (valid.isEmpty()) {
            return null;
        }
        Aspect chosen = valid.get(level.getRandom().nextInt(valid.size()));
        return aspects.getAmount(chosen) > min ? chosen : null;
    }

    @Override
    public NodeType getNodeType() {
        return nodeType;
    }

    @Override
    public void setNodeType(NodeType type) {
        this.nodeType = type;
    }

    @Override
    public @Nullable NodeModifier getNodeModifier() {
        return nodeModifier;
    }

    @Override
    public void setNodeModifier(@Nullable NodeModifier modifier) {
        this.nodeModifier = modifier;
    }

    @Override
    public int getNodeVisBase(Aspect aspect) {
        return aspectsBase.getAmount(aspect);
    }

    @Override
    public void setNodeVisBase(Aspect aspect, int amount) {
        if (aspectsBase.getAmount(aspect) < amount) {
            aspectsBase.merge(aspect, amount);
        } else {
            aspectsBase.reduce(aspect, aspectsBase.getAmount(aspect) - amount);
        }
    }

    public void nodeChange() {
        regeneration = -1;
        sync();
    }

    public double getDistanceTo(double x, double y, double z) {
        double dx = worldPosition.getX() + 0.5 - x;
        double dy = worldPosition.getY() + 0.5 - y;
        double dz = worldPosition.getZ() + 0.5 - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static int baseRegeneration(@Nullable NodeModifier modifier) {
        if (modifier == null) {
            return 600;
        }
        return switch (modifier) {
            case BRIGHT -> 400;
            case PALE -> 900;
            case FADING -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        lastActive = input.getLongOr("lastActive", 0L);
        id = input.getStringOr("nodeId", "");
        if (id.isEmpty()) {
            id = null;
        }
        nodeType = input.read("type", NodeType.CODEC).orElse(NodeType.NORMAL);
        nodeModifier = input.read("modifier", NodeModifier.CODEC).orElse(null);
        aspects = input.read("aspects", AspectList.CODEC).orElseGet(AspectList::new);
        aspectsBase = input.read("aspectsBase", AspectList.CODEC).orElseGet(AspectList::new);
        drainColor = input.getIntOr("drainColor", 0xFFFFFF);
        targetColor = drainColor;
        drainEntityId = input.getIntOr("drainEntity", -1);
        drainTicks = drainEntityId < 0 ? 0 : 10;
        drainEntity = null;
        drainCollision = null;
        int regen = baseRegeneration(nodeModifier);
        long inc = regen * 75L;
        if (regen > 0 && lastActive > 0L && System.currentTimeMillis() > lastActive + inc) {
            catchUp = true;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("lastActive", lastActive);
        if (id != null) {
            output.putString("nodeId", id);
        }
        output.store("type", NodeType.CODEC, nodeType);
        output.storeNullable("modifier", NodeModifier.CODEC, nodeModifier);
        output.store("aspects", AspectList.CODEC, aspects);
        output.store("aspectsBase", AspectList.CODEC, aspectsBase);
        output.putInt("drainColor", drainColor);
    }

    private BlockState blockState() {
        return getBlockState();
    }

    private boolean isAiryNode() {
        return blockState().is(ModBlocks.AURA_NODE.get());
    }

    public void tick(Level tickLevel) {
        if (tickLevel.isClientSide()) {
            if (drainTicks > 0) {
                drainTicks--;
            }
            int red = ((targetColor >> 16) & 255) + ((color >> 16) & 255) * 4;
            int green = ((targetColor >> 8) & 255) + ((color >> 8) & 255) * 4;
            int blue = (targetColor & 255) + (color & 255) * 4;
            color = ((red / 5) << 16) | ((green / 5) << 8) | (blue / 5);
        } else if (drainEntity instanceof Player player && (!player.isUsingItem() || player.isRemoved())) {
            clearDrain();
            sync();
        }
        if (id == null) {
            generateId();
        }
        boolean change = false;
        change = handleHungryNodeFirst(tickLevel, change);
        count++;
        checkLock(tickLevel);
        if (tickLevel instanceof ServerLevel serverLevel) {
            change = handleDischarge(serverLevel, change);
            change = handleRecharge(serverLevel, change);
            if (isRemoved()) {
                return;
            }
            change = handleTaintNode(serverLevel, change);
            change = handleNodeStability(serverLevel, change);
            change = handleDarkNode(serverLevel, change);
            change = handlePureNode(serverLevel, change);
            change = handleHungryNodeSecond(serverLevel, change);
            if (change) {
                sync();
            }
        } else if (nodeType == NodeType.DARK && count % 50 == 0) {
            Fx.get().sinisterNode(tickLevel, worldPosition);
        }
    }

    public void checkLock(Level checkLevel) {
        if ((count <= 1 || count % 50 == 0) && worldPosition.getY() > checkLevel.getMinY() && isAiryNode()) {
            byte oldLock = nodeLock;
            nodeLock = 0;
            BlockPos below = worldPosition.below();
            if (!checkLevel.hasNeighborSignal(below) && checkLevel.getBlockState(below).getBlock() instanceof NodeStabilizerBlock stabilizer) {
                nodeLock = (byte) (stabilizer.isAdvanced() ? 2 : 1);
            }
            if (oldLock != nodeLock) {
                regeneration = -1;
            }
        }
    }

    private boolean handleHungryNodeFirst(Level tickLevel, boolean change) {
        if (nodeType == NodeType.HUNGRY && tickLevel.isClientSide()) Fx.get().hungryNode(tickLevel, worldPosition);
        if (nodeType != NodeType.HUNGRY || !Config.hardNode()) {
            return change;
        }
        AABB area = new AABB(worldPosition).inflate(15.0);
        List<Entity> entities = tickLevel.getEntitiesOfClass(Entity.class, area);
        for (Entity entity : entities) {
            if (entity instanceof Player player && player.getAbilities().invulnerable) {
                continue;
            }
            if (entity.isAlive() && !entity.isInvulnerable() && tickLevel instanceof ServerLevel serverLevel) {
                double distance = getDistanceTo(entity.getX(), entity.getY(), entity.getZ());
                if (distance < 2.0) {
                    entity.hurtServer(serverLevel, serverLevel.damageSources().fellOutOfWorld(), 1.0F);
                    if (!entity.isAlive()) {
                        change = feedOnKill(serverLevel, entity, change);
                    }
                }
            }
            double dx = (worldPosition.getX() + 0.5 - entity.getX()) / 15.0;
            double dy = (worldPosition.getY() + 0.5 - entity.getY()) / 15.0;
            double dz = (worldPosition.getZ() + 0.5 - entity.getZ()) / 15.0;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double strength = 1.0 - length;
            if (strength > 0.0) {
                strength *= strength;
                entity.setDeltaMovement(entity.getDeltaMovement().add(
                    dx / length * strength * 0.15,
                    dy / length * strength * 0.25,
                    dz / length * strength * 0.15
                ));
            }
        }
        return change;
    }

    private boolean feedOnKill(ServerLevel serverLevel, Entity entity, boolean change) {
        ScanManager.Target target = ScanManager.resolveEntity(entity);
        AspectList scanned = target == null ? null : target.aspects();
        if (scanned == null || scanned.size() <= 0) {
            return change;
        }
        AspectList primals = AspectHelper.reduceToPrimals(scanned.copy());
        if (primals.size() <= 0) {
            return change;
        }
        List<Aspect> found = primals.getAspects();
        Aspect aspect = found.get(serverLevel.getRandom().nextInt(found.size()));
        if (aspects.getAmount(aspect) < getNodeVisBase(aspect)) {
            addToContainer(aspect, 1);
            return true;
        }
        if (serverLevel.getRandom().nextInt(1 + getNodeVisBase(aspect) * 2) < primals.getAmount(aspect)) {
            aspectsBase.add(aspect, 1);
            return true;
        }
        return change;
    }

    private boolean handleDischarge(ServerLevel serverLevel, boolean change) {
        if (!isAiryNode() || nodeLock == 1) {
            return change;
        }
        if (nodeModifier == NodeModifier.FADING) {
            return change;
        }
        RandomSource random = serverLevel.getRandom();
        boolean shiny = nodeType == NodeType.HUNGRY || nodeModifier == NodeModifier.BRIGHT;
        int increment = nodeModifier == null ? 2 : (shiny ? 1 : (nodeModifier == NodeModifier.PALE ? 3 : 2));
        if (count % increment != 0) {
            return change;
        }
        int offsetX = random.nextInt(5) - random.nextInt(5);
        int offsetY = random.nextInt(5) - random.nextInt(5);
        int offsetZ = random.nextInt(5) - random.nextInt(5);
        if (nodeModifier == NodeModifier.PALE && random.nextBoolean()) {
            return change;
        }
        if (offsetX == 0 && offsetY == 0 && offsetZ == 0) {
            return change;
        }
        BlockPos targetPos = worldPosition.offset(offsetX, offsetY, offsetZ);
        BlockEntity entity = serverLevel.getBlockEntity(targetPos);
        BlockState targetState = serverLevel.getBlockState(targetPos);
        if (!(entity instanceof Node target) || !(targetState.is(ModBlocks.AURA_NODE.get()) || targetState.is(ModBlocks.ENERGIZED_AURA_NODE.get()))) {
            return change;
        }
        if (target instanceof NodeBlockEntity targetNode && targetNode.getLock() > 0) {
            return change;
        }
        int targetAverage = (target.getAspects().visSize() + target.getAspectsBase().visSize()) / 2;
        int thisAverage = (aspects.visSize() + aspectsBase.visSize()) / 2;
        if (targetAverage >= thisAverage || target.getAspects().size() <= 0) {
            return change;
        }
        List<Aspect> targetAspects = target.getAspects().getAspects();
        Aspect aspect = targetAspects.get(random.nextInt(targetAspects.size()));
        boolean transferred = false;
        if (aspects.getAmount(aspect) < getNodeVisBase(aspect) && target.takeFromContainer(aspect, 1)) {
            addToContainer(aspect, 1);
            transferred = true;
        } else if (target.takeFromContainer(aspect, 1)) {
            if (random.nextInt(1 + (int) (getNodeVisBase(aspect) / (shiny ? 1.5 : 1.0))) == 0) {
                aspectsBase.add(aspect, 1);
                if (nodeModifier == NodeModifier.PALE && random.nextInt(100) == 0) {
                    nodeModifier = null;
                    regeneration = -1;
                }
                if (random.nextInt(3) == 0) {
                    target.setNodeVisBase(aspect, target.getNodeVisBase(aspect) - 1);
                }
            }
            transferred = true;
        }
        if (!transferred) {
            return change;
        }
        if (target instanceof NodeBlockEntity targetNode) {
            targetNode.wait = targetNode.regeneration / 2;
        }
        entity.setChanged();
        serverLevel.sendBlockUpdated(targetPos, targetState, targetState, Block.UPDATE_CLIENTS);
        NodeZapPayload payload = new NodeZapPayload(
            targetPos.getX() + 0.5F, targetPos.getY() + 0.5F, targetPos.getZ() + 0.5F,
            worldPosition.getX() + 0.5F, worldPosition.getY() + 0.5F, worldPosition.getZ() + 0.5F
        );
        ModNetwork.sendToNear(serverLevel, null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 32.0, payload);
        serverLevel.playSound(null, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, ModSounds.ZAP.get(), SoundSource.BLOCKS, 0.1F, 1.0F + random.nextFloat() * 0.2F);
        return true;
    }

    private boolean handleRecharge(ServerLevel serverLevel, boolean change) {
        RandomSource random = serverLevel.getRandom();
        if (regeneration < 0) {
            regeneration = baseRegeneration(nodeModifier);
            if (nodeLock == 1) {
                regeneration *= 2;
            }
            if (nodeLock == 2) {
                regeneration *= 20;
            }
        }
        if (catchUp) {
            catchUp = false;
            long increment = regeneration * 75L;
            int amount = increment > 0 ? (int) ((System.currentTimeMillis() - lastActive) / increment) : 0;
            if (amount > 0) {
                int limit = Math.min(amount, aspectsBase.visSize());
                for (int step = 0; step < limit; step++) {
                    List<Aspect> missing = missingAspects();
                    if (!missing.isEmpty()) {
                        addToContainer(missing.get(random.nextInt(missing.size())), 1);
                    }
                }
            }
        }
        if (count % 1200 == 0) {
            for (Aspect aspect : aspects.getAspects()) {
                if (aspects.getAmount(aspect) <= 0) {
                    setNodeVisBase(aspect, getNodeVisBase(aspect) - 1);
                    if (random.nextInt(20) == 0 || getNodeVisBase(aspect) <= 0) {
                        aspects.remove(aspect);
                        if (random.nextInt(5) == 0) {
                            if (nodeModifier == NodeModifier.BRIGHT) {
                                nodeModifier = null;
                            } else if (nodeModifier == null) {
                                nodeModifier = NodeModifier.PALE;
                            }
                            if (nodeModifier == NodeModifier.PALE && random.nextInt(5) == 0) {
                                nodeModifier = NodeModifier.FADING;
                            }
                        }
                        nodeChange();
                        break;
                    }
                    nodeChange();
                }
            }
            if (aspects.size() <= 0) {
                nodeDied(serverLevel);
                return change;
            }
        }
        if (wait > 0) {
            wait--;
        }
        if (regeneration > 0 && wait == 0 && count % regeneration == 0) {
            lastActive = System.currentTimeMillis();
            List<Aspect> missing = missingAspects();
            if (!missing.isEmpty()) {
                addToContainer(missing.get(random.nextInt(missing.size())), 1);
                change = true;
            }
        }
        return change;
    }

    private List<Aspect> missingAspects() {
        List<Aspect> missing = new ArrayList<>();
        for (Aspect aspect : aspects.getAspects()) {
            if (aspects.getAmount(aspect) < getNodeVisBase(aspect)) {
                missing.add(aspect);
            }
        }
        return missing;
    }

    private void nodeDied(ServerLevel serverLevel) {
        BlockState state = blockState();
        if (state.is(ModBlocks.AURA_NODE.get())) {
            serverLevel.removeBlock(worldPosition, false);
        } else if (state.is(ModBlocks.SILVERWOOD_KNOT.get())) {
            BlockState log = ModBlocks.SILVERWOOD_LOG.get().defaultBlockState();
            if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                log = log.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
            }
            serverLevel.setBlock(worldPosition, log, Block.UPDATE_ALL);
        }
    }

    private void setBiome(ServerLevel serverLevel, int x, int z, ResourceKey<Biome> key) {
        Holder<Biome> biome = serverLevel.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
        BiomeEdit.setColumn(serverLevel, new BlockPos(x, worldPosition.getY(), z), biome);
    }

    private boolean biomeAt(ServerLevel serverLevel, int x, int z, ResourceKey<Biome> key) {
        BlockPos pos = new BlockPos(x, worldPosition.getY(), z);
        return serverLevel.hasChunkAt(pos) && serverLevel.getBiome(pos).is(key);
    }

    private boolean handleTaintNode(ServerLevel serverLevel, boolean change) {
        RandomSource random = serverLevel.getRandom();
        if (nodeType == NodeType.TAINTED && count % 50 == 0) {
            int x = worldPosition.getX() + random.nextInt(8) - random.nextInt(8);
            int z = worldPosition.getZ() + random.nextInt(8) - random.nextInt(8);
            if (serverLevel.hasChunkAt(new BlockPos(x, worldPosition.getY(), z)) && !biomeAt(serverLevel, x, z, TAINTED_LAND)) {
                setBiome(serverLevel, x, z, TAINTED_LAND);
            }
            if (Config.hardNode() && random.nextBoolean()) {
                x = worldPosition.getX() + random.nextInt(5) - random.nextInt(5);
                z = worldPosition.getZ() + random.nextInt(5) - random.nextInt(5);
                int y = worldPosition.getY() + random.nextInt(5) - random.nextInt(5);
                NodeHooks.spreadTaintFibres(serverLevel, new BlockPos(x, y, z));
            }
        } else if (nodeType != NodeType.PURE && nodeType != NodeType.TAINTED && count % 100 == 0) {
            if (biomeAt(serverLevel, worldPosition.getX(), worldPosition.getZ(), TAINTED_LAND) && random.nextInt(500) == 0) {
                nodeType = NodeType.TAINTED;
                nodeChange();
            }
        }
        return change;
    }

    private boolean handleNodeStability(ServerLevel serverLevel, boolean change) {
        RandomSource random = serverLevel.getRandom();
        if (count % 100 == 0) {
            if (nodeType == NodeType.UNSTABLE && random.nextBoolean()) {
                if (nodeLock == 0) {
                    Aspect aspect = takeRandomPrimalFromSource();
                    if (aspect != null) {
                        NodeHooks.spawnAspectOrb(serverLevel, worldPosition, aspect, 1);
                        change = true;
                    }
                } else if (random.nextInt(10000 / nodeLock) == 42) {
                    nodeType = NodeType.NORMAL;
                    change = true;
                }
            }
            if (nodeModifier == NodeModifier.FADING && nodeLock > 0 && random.nextInt(12500 / nodeLock) == 69) {
                nodeModifier = NodeModifier.PALE;
                change = true;
            }
        }
        return change;
    }

    private boolean spreadsBiomes(ServerLevel serverLevel) {
        return serverLevel.dimension() != Level.NETHER && serverLevel.dimension() != Level.END;
    }

    private boolean handlePureNode(ServerLevel serverLevel, boolean change) {
        if (spreadsBiomes(serverLevel) && nodeType == NodeType.PURE && count % 50 == 0) {
            RandomSource random = serverLevel.getRandom();
            int x = worldPosition.getX() + random.nextInt(8) - random.nextInt(8);
            int z = worldPosition.getZ() + random.nextInt(8) - random.nextInt(8);
            BlockPos pos = new BlockPos(x, worldPosition.getY(), z);
            if (serverLevel.hasChunkAt(pos) && !biomeAt(serverLevel, x, z, MAGICAL_FOREST)) {
                if (biomeAt(serverLevel, x, z, TAINTED_LAND)) {
                    setBiome(serverLevel, x, z, MAGICAL_FOREST);
                } else if (blockState().is(ModBlocks.SILVERWOOD_KNOT.get())) {
                    setBiome(serverLevel, x, z, MAGICAL_FOREST);
                }
            }
        }
        return change;
    }

    private boolean handleDarkNode(ServerLevel serverLevel, boolean change) {
        if (spreadsBiomes(serverLevel) && nodeType == NodeType.DARK && count % 50 == 0) {
            RandomSource random = serverLevel.getRandom();
            int x = worldPosition.getX() + random.nextInt(12) - random.nextInt(12);
            int z = worldPosition.getZ() + random.nextInt(12) - random.nextInt(12);
            if (serverLevel.hasChunkAt(new BlockPos(x, worldPosition.getY(), z)) && !biomeAt(serverLevel, x, z, EERIE)) {
                setBiome(serverLevel, x, z, EERIE);
            }
            if (Config.hardNode()
                && random.nextBoolean()
                && serverLevel.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 24.0, false) != null) {
                double spawnX = worldPosition.getX() + (random.nextDouble() - random.nextDouble()) * 5.0;
                double spawnY = worldPosition.getY() + random.nextInt(3) - 1;
                double spawnZ = worldPosition.getZ() + (random.nextDouble() - random.nextDouble()) * 5.0;
                NodeHooks.spawnGiantBrainyZombie(serverLevel, worldPosition, BlockPos.containing(spawnX, spawnY, spawnZ));
            }
        }
        return change;
    }

    private boolean handleHungryNodeSecond(ServerLevel serverLevel, boolean change) {
        if (nodeType != NodeType.HUNGRY || count % 50 != 0) {
            return change;
        }
        RandomSource random = serverLevel.getRandom();
        int targetX = worldPosition.getX() + random.nextInt(16) - random.nextInt(16);
        int targetY = worldPosition.getY() + random.nextInt(16) - random.nextInt(16);
        int targetZ = worldPosition.getZ() + random.nextInt(16) - random.nextInt(16);
        BlockPos probe = new BlockPos(targetX, worldPosition.getY(), targetZ);
        if (!serverLevel.hasChunksAt(worldPosition.offset(-16, -16, -16), worldPosition.offset(16, 16, 16)) || !serverLevel.hasChunkAt(probe)) {
            return change;
        }
        int height = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, targetX, targetZ);
        if (targetY > height) {
            targetY = height;
        }
        Vec3 from = Vec3.atCenterOf(worldPosition);
        Vec3 to = new Vec3(targetX + 0.5, targetY + 0.5, targetZ + 0.5);
        BlockHitResult hit = serverLevel.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.BLOCK && worldPosition.distSqr(hit.getBlockPos()) < 256.0) {
            BlockPos blockPos = hit.getBlockPos();
            BlockState state = serverLevel.getBlockState(blockPos);
            if (!state.isAir()) {
                float hardness = state.getDestroySpeed(serverLevel, blockPos);
                if (hardness >= 0.0F && hardness < 5.0F) {
                    serverLevel.destroyBlock(blockPos, true);
                }
            }
        }
        return change;
    }

    @Override
    public InteractionResult onWandUse(Level useLevel, BlockPos pos, Player player, ItemStack wand) {
        WandKeys.TARGET.set(wand, worldPosition);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUsingWandTick(ItemStack wand, Player player, int remaining) {
        Level useLevel = player.level();
        boolean changed = false;
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, true);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            if (!blockHit.getBlockPos().equals(worldPosition)) {
                player.stopUsingItem();
                onWandStoppedUsing(wand, useLevel, player, remaining);
                return;
            }
        } else {
            player.stopUsingItem();
            onWandStoppedUsing(wand, useLevel, player, remaining);
            return;
        }
        if (useLevel.isClientSide()) {
            return;
        }
        if (remaining % 5 == 0) {
            int tap = 1;
            if (ResearchGate.isComplete(player, "NODETAPPER1")) {
                tap++;
            }
            if (ResearchGate.isComplete(player, "NODETAPPER2")) {
                tap++;
            }
            boolean preserve = !player.isShiftKeyDown()
                && ResearchGate.isComplete(player, "NODEPRESERVE")
                && !Wands.getRod(wand).tag().equals("wood")
                && !Wands.getCap(wand).tag().equals("iron");
            boolean success = false;
            Aspect aspect = chooseRandomFilteredFromSource(Wands.getAspectsWithRoom(wand), preserve);
            if (aspect != null) {
                int amount = aspects.getAmount(aspect);
                if (tap > amount) {
                    tap = amount;
                }
                if (preserve && tap == amount) {
                    tap--;
                }
                if (tap > 0) {
                    int remainder = Wands.addVis(wand, aspect, tap, !useLevel.isClientSide());
                    if (remainder < tap) {
                        drainColor = aspect.getColor();
                        if (!useLevel.isClientSide()) {
                            takeFromContainer(aspect, tap - remainder);
                            changed = true;
                        }
                        success = true;
                    }
                }
            }
            if (success) {
                drainEntity = player;
                drainCollision = hit;
                targetColor = drainColor;
            } else {
                changed |= drainEntity != null;
                clearDrain();
            }
            if (changed) {
                sync();
            }
        }
    }

    @Override
    public void onWandStoppedUsing(ItemStack wand, Level useLevel, Player player, int remaining) {
        if (drainEntity == player) {
            clearDrain();
            if (!useLevel.isClientSide()) {
                sync();
            }
        }
    }

    private void clearDrain() {
        drainEntity = null;
        drainCollision = null;
        drainEntityId = -1;
        drainTicks = 0;
    }
}
