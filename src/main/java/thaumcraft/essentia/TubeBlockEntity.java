package thaumcraft.essentia;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.fx.Fx;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModSounds;
import thaumcraft.wand.WandInteractable;

public class TubeBlockEntity extends TcBlockEntity implements EssentiaTransport, WandInteractable {
    protected final boolean[] openSides = {true, true, true, true, true, true};
    private @Nullable Aspect essentiaType;
    private int essentiaAmount;
    private @Nullable Aspect suctionType;
    private int suction;
    private int venting;
    private int ventColor;
    protected int count;

    public TubeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TUBE.get(), pos, state);
    }

    protected TubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TubeBlockEntity tube) {
        tube.tick();
    }

    public Direction getFacing() {
        return getBlockState().getValue(TubeBlock.FACING);
    }

    public boolean isSideOpen(Direction side) {
        return openSides[side.ordinal()];
    }

    public void setSideOpen(Direction side, boolean open) {
        openSides[side.ordinal()] = open;
        sync();
    }

    public int getVenting() {
        return venting;
    }

    protected void tick() {
        if (level == null) {
            return;
        }
        if (venting > 0) {
            venting--;
        }
        if (count == 0) {
            count = level.getRandom().nextInt(10);
        }
        if (!level.isClientSide()) {
            if (venting <= 0) {
                if (++count % 2 == 0) {
                    calculateSuction(null, false, false);
                    checkVenting();
                    if (essentiaType != null && essentiaAmount == 0) {
                        essentiaType = null;
                    }
                }
                if (count % 5 == 0 && suction > 0) {
                    equalizeWithNeighbours(false);
                }
            }
        } else if (venting > 0) {
            Random random = new Random(hashCode() * 4L);
            float pitch = random.nextFloat() * 360.0F;
            float yaw = random.nextFloat() * 360.0F;
            double motionX = -Math.sin(yaw / 180.0F * Math.PI) * Math.cos(pitch / 180.0F * Math.PI);
            double motionY = -Math.sin(pitch / 180.0F * Math.PI);
            Fx.get().ventParticles(
                level,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                motionX / 5.0,
                motionY / 5.0,
                motionX / 5.0,
                ventColor,
                1.0F
            );
        }
    }

    protected void calculateSuction(@Nullable Aspect filter, boolean restrict, boolean directional) {
        suction = 0;
        suctionType = null;
        for (Direction dir : Direction.values()) {
            if ((!directional || getFacing() == dir.getOpposite()) && isConnectable(dir)) {
                EssentiaTransport other = Essentia.getConnectable(level, worldPosition, dir);
                if (other == null) {
                    continue;
                }
                Direction back = dir.getOpposite();
                Aspect otherType = other.getSuctionType(back);
                if ((filter == null || otherType == null || otherType == filter)
                    && (filter != null || getEssentiaAmount(dir) <= 0 || otherType == null || getEssentiaType(dir) == otherType)
                    && (filter == null || getEssentiaAmount(dir) <= 0 || getEssentiaType(dir) == null || otherType == null || getEssentiaType(dir) == otherType)) {
                    int suck = other.getSuctionAmount(back);
                    if (suck > 0 && suck > suction + 1) {
                        Aspect type = otherType;
                        if (type == null) {
                            type = filter;
                        }
                        setSuction(type, restrict ? suck / 2 : suck - 1);
                    }
                }
            }
        }
    }

    protected void checkVenting() {
        for (Direction dir : Direction.values()) {
            if (!isConnectable(dir)) {
                continue;
            }
            EssentiaTransport other = Essentia.getConnectable(level, worldPosition, dir);
            if (other == null) {
                continue;
            }
            int suck = other.getSuctionAmount(dir.getOpposite());
            if (suction > 0 && (suck == suction || suck == suction - 1) && suctionType != other.getSuctionType(dir.getOpposite())) {
                int color = suctionType != null ? suctionType.ordinal() : -1;
                level.blockEvent(worldPosition, getBlockState().getBlock(), 1, color);
                venting = 40;
            }
        }
    }

    protected void equalizeWithNeighbours(boolean directional) {
        if (essentiaAmount > 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if ((!directional || getFacing() != dir.getOpposite()) && isConnectable(dir)) {
                EssentiaTransport other = Essentia.getConnectable(level, worldPosition, dir);
                if (other == null) {
                    continue;
                }
                Direction back = dir.getOpposite();
                if (other.canOutputTo(back)
                    && (getSuctionType(null) == null || getSuctionType(null) == other.getEssentiaType(back) || other.getEssentiaType(back) == null)
                    && getSuctionAmount(null) > other.getSuctionAmount(back)
                    && getSuctionAmount(null) >= other.getMinimumSuction()) {
                    Aspect aspect = getSuctionType(null);
                    if (aspect == null) {
                        aspect = other.getEssentiaType(back);
                        if (aspect == null) {
                            aspect = other.getEssentiaType(null);
                        }
                    }
                    if (aspect == null) {
                        continue;
                    }
                    int moved = addEssentia(aspect, other.takeEssentia(aspect, 1, back), dir);
                    if (moved > 0) {
                        if (level.getRandom().nextInt(100) == 0) {
                            level.blockEvent(worldPosition, getBlockState().getBlock(), 0, 0);
                        }
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean isConnectable(@Nullable Direction face) {
        return face != null && openSides[face.ordinal()];
    }

    @Override
    public boolean canInputFrom(@Nullable Direction face) {
        return face != null && openSides[face.ordinal()];
    }

    @Override
    public boolean canOutputTo(@Nullable Direction face) {
        return face != null && openSides[face.ordinal()];
    }

    @Override
    public void setSuction(@Nullable Aspect aspect, int amount) {
        suctionType = aspect;
        suction = amount;
    }

    @Override
    public @Nullable Aspect getSuctionType(@Nullable Direction face) {
        return suctionType;
    }

    @Override
    public int getSuctionAmount(@Nullable Direction face) {
        return suction;
    }

    @Override
    public @Nullable Aspect getEssentiaType(@Nullable Direction face) {
        return essentiaType;
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction face) {
        return essentiaAmount;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        if (canOutputTo(face) && essentiaType == aspect && essentiaAmount > 0 && amount > 0) {
            essentiaAmount--;
            if (essentiaAmount <= 0) {
                essentiaType = null;
            }
            setChanged();
            return 1;
        }
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (canInputFrom(face) && essentiaAmount == 0 && amount > 0) {
            essentiaType = aspect;
            essentiaAmount++;
            setChanged();
            return 1;
        }
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 0) {
            if (level != null && level.isClientSide()) {
                level.playLocalSound(
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    ModSounds.CREAK.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.3F + level.getRandom().nextFloat() * 0.2F,
                    false
                );
            }
            return true;
        }
        if (id == 1) {
            if (level != null && level.isClientSide()) {
                if (venting <= 0) {
                    level.playLocalSound(
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        0.1F,
                        1.0F + level.getRandom().nextFloat() * 0.1F,
                        false
                    );
                }
                venting = 50;
                Aspect[] aspects = Aspect.values();
                ventColor = param >= 0 && param < aspects.length ? aspects[param].getColor() : 0xAAAAAA;
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    public List<TubeArms.TraceBox> traceBoxes() {
        return TubeArms.traceBoxes(level, worldPosition, 0.34375, 0.65625);
    }

    protected boolean canConnectSide(Direction side) {
        return level.getBlockEntity(worldPosition.relative(side)) instanceof EssentiaTransport;
    }

    protected boolean canFaceTowards(Direction candidate) {
        return canConnectSide(candidate.getOpposite()) && isConnectable(candidate.getOpposite());
    }

    @Override
    public InteractionResult onWandUseOn(Level useLevel, BlockPos pos, Player player, ItemStack wand, Direction side) {
        int hit = TubeArms.trace(player, pos, traceBoxes());
        if (hit < 0) {
            return InteractionResult.PASS;
        }
        if (useLevel.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        useLevel.playSound(null, pos, ModSounds.TOOL.get(), SoundSource.BLOCKS, 0.5F, 0.9F + useLevel.getRandom().nextFloat() * 0.2F);
        Direction arm = TubeArms.direction(hit);
        if (arm != null) {
            openSides[arm.ordinal()] = !openSides[arm.ordinal()];
            BlockEntity neighbour = useLevel.getBlockEntity(pos.relative(arm));
            if (neighbour instanceof TubeBlockEntity other) {
                other.openSides[arm.getOpposite().ordinal()] = openSides[arm.ordinal()];
                other.sync();
            }
            sync();
            TubeArms.refreshAround(useLevel, pos);
        } else {
            int index = getFacing().ordinal();
            while (++index < 20) {
                Direction candidate = Direction.from3DDataValue(index % 6);
                if (canFaceTowards(candidate)) {
                    useLevel.setBlock(pos, getBlockState().setValue(TubeBlock.FACING, candidate), Block.UPDATE_ALL);
                    sync();
                    TubeArms.refreshAround(useLevel, pos);
                    break;
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        essentiaType = input.read("type", Aspect.CODEC).orElse(null);
        essentiaAmount = input.getIntOr("amount", 0);
        suctionType = input.read("stype", Aspect.CODEC).orElse(null);
        suction = input.getIntOr("samount", 0);
        int mask = input.getIntOr("open", 63);
        for (int index = 0; index < 6; index++) {
            openSides[index] = (mask & (1 << index)) != 0;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (essentiaType != null) {
            output.store("type", Aspect.CODEC, essentiaType);
        }
        output.putInt("amount", essentiaAmount);
        if (suctionType != null) {
            output.store("stype", Aspect.CODEC, suctionType);
        }
        output.putInt("samount", suction);
        int mask = 0;
        for (int index = 0; index < 6; index++) {
            if (openSides[index]) {
                mask |= 1 << index;
            }
        }
        output.putInt("open", mask);
    }
}
