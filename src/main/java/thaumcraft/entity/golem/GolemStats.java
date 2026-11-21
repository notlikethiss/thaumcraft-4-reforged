package thaumcraft.entity.golem;

public record GolemStats(int health, int armor, float speed, boolean fireResist, int upgrades, int carry,
    int regenDelay, int strength, int visCost, boolean heavy) {

    public static GolemStats of(GolemType type) {
        return new GolemStats(type.health, type.armor, type.speed, type.fireResist, type.upgrades,
            type.carry, type.regenDelay, type.strength, type.visCost, type.heavy);
    }
}
