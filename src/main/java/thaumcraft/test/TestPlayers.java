package thaumcraft.test;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class TestPlayers {
    private TestPlayers() {
    }

    public static ServerPlayer mockServerPlayer(GameTestHelper helper, GameType gameType) {
        ServerPlayer player = new ServerPlayer(
            helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "test-mock-player"), ClientInformation.createDefault()
        ) {
            @Override
            public GameType gameMode() {
                return gameType;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }
        };
        gameType.updatePlayerAbilities(player.getAbilities());
        return player;
    }
}
