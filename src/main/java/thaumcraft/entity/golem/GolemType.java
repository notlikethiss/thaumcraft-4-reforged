package thaumcraft.entity.golem;

public enum GolemType {
    STRAW(10, 0, 0.38F, false, false, 1, 1, 75, 0, 0),
    WOOD(20, 6, 0.35F, false, false, 1, 4, 75, 1, 0),
    TALLOW(20, 9, 0.33F, false, false, 2, 8, 75, 2, 0),
    CLAY(25, 9, 0.33F, true, false, 1, 8, 100, 2, 0),
    FLESH(15, 6, 0.35F, false, false, 2, 4, 40, 1, 0),
    STONE(30, 12, 0.32F, true, true, 1, 16, 100, 3, 0),
    IRON(35, 15, 0.31F, true, true, 1, 32, 125, 4, 0),
    THAUMIUM(40, 15, 0.32F, true, true, 2, 32, 100, 4, 0);

    public final int health;
    public final int armor;
    public final float speed;
    public final boolean fireResist;
    public final boolean heavy;
    public final int upgrades;
    public final int carry;
    public final int regenDelay;
    public final int strength;
    public final int visCost;

    GolemType(int health, int armor, float speed, boolean fireResist, boolean heavy, int upgrades, int carry,
        int regenDelay, int strength, int visCost) {
        this.health = health;
        this.armor = armor;
        this.speed = speed;
        this.fireResist = fireResist;
        this.heavy = heavy;
        this.upgrades = upgrades;
        this.carry = carry;
        this.regenDelay = regenDelay;
        this.strength = strength;
        this.visCost = visCost;
    }

    public static GolemType byId(int id) {
        return values()[Math.clamp(id, 0, values().length - 1)];
    }

    public String itemId() {
        return name().toLowerCase(java.util.Locale.ROOT) + "_golem";
    }
}
