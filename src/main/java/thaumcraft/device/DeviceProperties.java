package thaumcraft.device;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import thaumcraft.registry.ModSounds;

public final class DeviceProperties {
    private DeviceProperties() {
    }

    private static SoundType airySound() {
        return new SoundType(
            0.0F,
            1.0F,
            SoundEvents.WOOL_BREAK,
            SoundEvents.WOOL_STEP,
            SoundEvents.WOOL_PLACE,
            SoundEvents.WOOL_HIT,
            SoundEvents.WOOL_FALL
        );
    }

    private static BlockBehaviour.Properties airy(BlockBehaviour.Properties properties) {
        return properties
            .mapColor(MapColor.NONE)
            .noCollision()
            .noOcclusion()
            .sound(airySound())
            .isValidSpawn((state, level, pos, type) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);
    }

    public static BlockBehaviour.Properties nitorLight(BlockBehaviour.Properties properties) {
        return airy(properties).instabreak().lightLevel(state -> 15).pushReaction(PushReaction.BLOCK);
    }

    public static BlockBehaviour.Properties lampLight(BlockBehaviour.Properties properties) {
        return airy(properties).air().replaceable().instabreak().lightLevel(state -> 15).pushReaction(PushReaction.DESTROY).noLootTable();
    }

    public static BlockBehaviour.Properties wardingAura(BlockBehaviour.Properties properties) {
        return airy(properties).replaceable().instabreak().pushReaction(PushReaction.BLOCK).noLootTable();
    }

    public static BlockBehaviour.Properties metalDevice(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0F, 17.0F).sound(SoundType.METAL).noOcclusion();
    }

    public static BlockBehaviour.Properties woodDevice(BlockBehaviour.Properties properties, float resistance) {
        return properties.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F, resistance).sound(SoundType.WOOD);
    }

    public static BlockBehaviour.Properties crystal(BlockBehaviour.Properties properties) {
        SoundType sound = new SoundType(
            1.0F,
            1.0F,
            ModSounds.CRYSTAL.get(),
            ModSounds.CRYSTAL.get(),
            ModSounds.CRYSTAL.get(),
            ModSounds.CRYSTAL.get(),
            ModSounds.CRYSTAL.get()
        );
        return properties
            .mapColor(MapColor.QUARTZ)
            .instrument(NoteBlockInstrument.HAT)
            .strength(0.7F, 1.0F)
            .sound(sound)
            .lightLevel(state -> 7)
            .noOcclusion()
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);
    }
}
